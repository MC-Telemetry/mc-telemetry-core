package de.mctelemetry.core.observations.scrapers.nbt

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IParameterizedObservationSource
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.Tag
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity

object NbtScraperSignLineObservationSource :
    PositionObservationSourceBase<NbtScraperSignLineObservationSource.Instance>(),
    IParameterizedObservationSource<BlockEntity, NbtScraperSignLineObservationSource.Instance> {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "nbt_scraper.sign_line")
    )

    override val streamCodec: StreamCodec<ByteBuf, Instance> =
        ByteBufCodecs.BYTE.map(
            { Instance(it) },
            { it.line },
        )

    override val parameters: Map<String, ArgumentType<*>> = mapOf(
        "line" to IntegerArgumentType.integer(1,4),
    )

    override fun instanceFromParameters(parameterMap: Map<String, *>): Instance {
        return Instance((parameterMap.getValue("line") as Int).toByte())
    }

    override fun fromNbt(tag: Tag?): Instance {
        tag!! as NumericTag
        return Instance(tag.asByte)
    }

    override fun toNbt(instance: Instance): ByteTag = ByteTag.valueOf(instance.line)

    class Instance(val line: Byte) : PositionInstanceBase<Instance>(NbtScraperSignLineObservationSource) {

        init {
            require(line in 1..4) {"Line index must be between 1 and 4"}
        }

        context(sourceContext: BlockEntity, attributeStore: IAttributeValueStore.MapAttributeStore)
        override fun observePosition(
            recorder: IObservationRecorder.Unresolved,
            level: ServerLevel,
            position: BlockPos,
            facing: Direction?,
            unusedAttributes: Set<AttributeDataSource<*>>
        ) {
            println("TODO: Observe sign at $position in ${level.dimension().location()}, line $line")
        }

    }
}
