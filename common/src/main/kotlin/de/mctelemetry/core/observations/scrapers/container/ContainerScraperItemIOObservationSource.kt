package de.mctelemetry.core.observations.scrapers.container

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IParameterizedObservationSource
import de.mctelemetry.core.api.observations.ObservationSourceBase
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.observations.IORecorder
import de.mctelemetry.core.platform.ModPlatform
import de.mctelemetry.core.utils.observe
import de.mctelemetry.core.utils.runWithExceptionCleanup
import de.mctelemetry.core.utils.withValue
import de.mctelemetry.core.utils.withoutValue
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

object ContainerScraperItemIOObservationSource :
    ObservationSourceBase<ObservationSourceContainerBlockEntity, ContainerScraperItemIOObservationSource.Instance>(),
    IParameterizedObservationSource<ObservationSourceContainerBlockEntity, ContainerScraperItemIOObservationSource.Instance> {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "container_scraper.item.io")
    )

    override val parameters: Map<String, IParameterizedObservationSource.Parameter<*>>
        get() = emptyMap()

    context(parameters: IParameterizedObservationSource.ParameterMap)
    override fun instanceFromParameters(): Instance {
        return Instance(null, null)
    }

    override val sourceContextType: Class<ObservationSourceContainerBlockEntity>
        get() = ObservationSourceContainerBlockEntity::class.java

    override fun fromNbt(tag: Tag?): Instance {
        tag as CompoundTag? // throw exception when not CompoundTag or null (`x as T?` != `x as? T`)
        tag ?: return Instance(null, null)
        val levelString: String? = tag.getString("dimension").takeIf { it.isNotBlank() }
        val positionArray: IntArray? = tag.getIntArray("pos").takeIf { it.isNotEmpty() }
        val level: ResourceKey<Level>? =
            levelString?.let { ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(it)) }
        val position: BlockPos? = positionArray?.let { BlockPos(it[0], it[1], it[2]) }
        return Instance(
            null,
            if (level != null && position != null)
                GlobalPos(level, position)
            else
                null
        )
    }

    override fun toNbt(instance: Instance): CompoundTag {
        return CompoundTag().apply {
            instance.position?.let {
                putString("dimension", it.dimension.location().toString())
                val pos = it.pos
                putIntArray("pos", intArrayOf(pos.x, pos.y, pos.z))
            }
        }
    }

    override val streamCodec: StreamCodec<in ByteBuf, Instance> =
        ByteBufCodecs.optional(GlobalPos.STREAM_CODEC).map(
            { Instance(null, it.getOrNull()) },
            { Optional.ofNullable(it.position) }
        )

    val ioFlowDirection = NativeAttributeKeyTypes.StringType.createObservationAttributeReference("io")
    val ioActive = NativeAttributeKeyTypes.BooleanType.createObservationAttributeReference("active")

    class Instance(
        recorder: IORecorder.IORecorderAccess<Item>?,
        position: GlobalPos?,
    ) : InstanceBase<ObservationSourceContainerBlockEntity, Instance>(ContainerScraperItemIOObservationSource) {

        private var closed: Boolean = false

        var ioRecorder: IORecorder.IORecorderAccess<Item>? = recorder
            private set

        var position: GlobalPos? = position
            private set


        context(sourceContext: ObservationSourceContainerBlockEntity, attributeStore: IAttributeValueStore.MapAttributeStore)
        override fun observe(recorder: IObservationRecorder.Unresolved, unusedAttributes: Set<AttributeDataSource<*>>) {
            val ioRecorder = ioRecorder
            if (ioRecorder == null) {
                OTelCoreMod.logger.warn("Requested observations for uninitialized ContainerScraperItemIO instance")
                return
            }
            if (ioRecorder.closed || closed) {
                OTelCoreMod.logger.warn("Requested observations for closed ContainerScraperItemIO instance")
                return
            }
            val inserted = ioRecorder.getRelativeInserted()
            val extracted = ioRecorder.getRelativeExtracted()
            val pushed = ioRecorder.getRelativePushed()
            val pulled = ioRecorder.getRelativePulled()
            if (ioFlowDirection in unusedAttributes) {
                ioFlowDirection.withoutValue {
                    if (ioActive in unusedAttributes) {
                        ioActive.withoutValue { recorder.observe(inserted + extracted + pushed + pulled) }
                    } else {
                        ioActive.withValue(true) { recorder.observe(pushed + pulled) }
                        ioActive.withValue(false) { recorder.observe(inserted + extracted) }
                    }
                }
            } else {
                if(ioActive in unusedAttributes) {
                    ioFlowDirection.withValue("in") {
                        ioActive.withoutValue { recorder.observe(inserted + pulled) }
                    }
                    ioFlowDirection.withValue("out") {
                        ioActive.withoutValue { recorder.observe(extracted + pulled) }
                    }
                }else {
                    ioFlowDirection.withValue("in") {
                        ioActive.withValue(true) { recorder.observe(pulled) }
                        ioActive.withValue(false) { recorder.observe(inserted) }
                    }
                    ioFlowDirection.withValue("out") {
                        ioActive.withValue(true) { recorder.observe(pushed) }
                        ioActive.withValue(false) { recorder.observe(extracted) }
                    }
                }
            }
        }

        override fun close() {
            this.closed = true
            val recorder = ioRecorder
            ioRecorder = null
            recorder?.close()
        }

        override fun onLoad(sourceContext: ObservationSourceContainerBlockEntity) {
            if (closed) return
            val sourceLevel = sourceContext.level
                ?: throw IllegalArgumentException("Level has not been set for sourceContext block entity: $sourceContext")
            val position: GlobalPos
            if (this.position == null) {
                val blockPos = sourceContext.blockPos
                val direction = when {
                    sourceContext.blockState.hasProperty(BlockStateProperties.FACING) -> sourceContext.blockState.getValue(
                        BlockStateProperties.FACING
                    )

                    sourceContext.blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) -> sourceContext.blockState.getValue(
                        BlockStateProperties.HORIZONTAL_FACING
                    )

                    else -> null
                }
                val relativeBlockPos = if (direction != null)
                    blockPos.relative(direction)
                else
                    blockPos
                position = GlobalPos(sourceLevel.dimension(), relativeBlockPos)
                this.position = position
            } else {
                position = this.position!!
            }
            if (ioRecorder != null) {
                return
            }
            if (sourceLevel !is ServerLevel) return
            val level = if (sourceLevel.dimension() == position.dimension)
                sourceLevel
            else
                sourceLevel.server.getLevel(position.dimension)
                    ?: throw IllegalArgumentException("Could not find dimension \"${position.dimension}\"")
            val newRecorder = ModPlatform.getItemStorageAccessor().getIORecorder(level, position.pos)
            runWithExceptionCleanup({ newRecorder.close() }) {
                ioRecorder = newRecorder
            }
        }
    }
}
