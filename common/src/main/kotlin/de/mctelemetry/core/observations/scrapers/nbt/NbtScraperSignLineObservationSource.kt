package de.mctelemetry.core.observations.scrapers.nbt

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IParameterizedObservationSource
import de.mctelemetry.core.api.observations.IParameterizedObservationSource.Parameter.Companion.get
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import de.mctelemetry.core.utils.observePreferred
import de.mctelemetry.core.utils.observePreferredAccumulatedNullable
import de.mctelemetry.core.utils.withoutValue
import de.mctelemetry.core.utils.withValue
import de.mctelemetry.core.utils.withValues
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignText

object NbtScraperSignLineObservationSource :
    PositionObservationSourceBase<NbtScraperSignLineObservationSource.Instance>(),
    IParameterizedObservationSource<BlockEntity, NbtScraperSignLineObservationSource.Instance> {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "nbt_scraper.sign_line")
    )

    val lineParameter: IParameterizedObservationSource.Parameter<Int> = IParameterizedObservationSource.Parameter(
        name = "line",
        argumentType = IntegerArgumentType.integer(1, 2 * SignText.LINES),
        optional = true,
    ) { line, _ -> require(line in 1..(2 * SignText.LINES)) { "Line number must be between 1 and ${2 * SignText.LINES}" } } // 0 is for optional

    val observedLine = NativeAttributeKeyTypes.LongType.createObservationAttributeReference("line")

    override val parameters: Map<String, IParameterizedObservationSource.Parameter<*>> =
        listOf(lineParameter).associateBy { it.name }

    context(parameters: IParameterizedObservationSource.ParameterMap)
    override fun instanceFromParameters(): Instance {
        return Instance(lineParameter.get()!!.toByte())
    }

    override val streamCodec: StreamCodec<ByteBuf, Instance> =
        ByteBufCodecs.BYTE.map(
            { Instance(it) },
            {
                it.line.also { line ->
                    require(line in 0..(2 * SignText.LINES)) { "Line number must be between 0 and ${2 * SignText.LINES}" }
                }
            },
        )

    override val codec: Codec<Instance> = Codec.BYTE.comapFlatMap({
        if (it !in 0..(2 * SignText.LINES)) return@comapFlatMap DataResult.error { "Line number must be between 0 and ${2 * SignText.LINES}" }
        DataResult.success(Instance(it))
    }, {
        it.line
    })

    class Instance(val line: Byte) : PositionInstanceBase<Instance>(NbtScraperSignLineObservationSource) {

        init {
            require(line in 0..(2 * SignText.LINES)) { "Line number must be between 0 and ${2 * SignText.LINES}" }
        }

        context(sourceContext: BlockEntity, attributeStore: IAttributeValueStore.MapAttributeStore)
        override fun observePosition(
            recorder: IObservationRecorder.Unresolved,
            level: ServerLevel,
            position: BlockPos,
            facing: Direction?,
            unusedAttributes: Set<AttributeDataSource<*>>
        ) {
            val block: SignBlockEntity = level.getBlockEntity(position) as? SignBlockEntity ?: return
            if (line == 0.toByte()) {
                if (observedLine in unusedAttributes) {
                    observedLine.withoutValue {
                        recorder.observePreferredAccumulatedNullable(
                            0..<(2 * SignText.LINES),
                            doubleBlock = {
                                getTextValueDouble(
                                    block,
                                    it.toByte(),
                                    orElse = { return@observePreferredAccumulatedNullable null },
                                )
                            },
                            longBlock = {
                                getTextValueLong(
                                    block,
                                    it.toByte(),
                                    orElse = { return@observePreferredAccumulatedNullable null },
                                )
                            },
                        )
                    }
                } else {
                    observedLine.withValues(1..(2 * SignText.LINES), transform = Int::toLong) {
                        recorder.observePreferred(
                            doubleBlock = {
                                getTextValueDouble(
                                    block, (it - 1).toByte(),
                                    orElse = { return@withValues },
                                )
                            },
                            longBlock = {
                                getTextValueLong(
                                    block, (it - 1).toByte(),
                                    orElse = { return@withValues }
                                )
                            },
                        )
                    }
                }
            } else {
                observedLine.withValue(line.toLong()) {
                    recorder.observePreferred(
                        doubleBlock = {
                            getTextValueDouble(block, (line - 1).toByte()) { return }
                        },
                        longBlock = {
                            getTextValueLong(block, (line - 1).toByte()) { return }
                        })
                }
            }
        }

        private inline fun getTextValueLong(sign: SignBlockEntity, index: Byte, orElse: () -> Long): Long {
            val component = sign.getText(index < SignText.LINES).getMessage(index % SignText.LINES, false)
                ?: return orElse()
            val text = component.string.trim()
            val long = text.toLongOrNull()
            if (long != null) return long
            val double = text.toDoubleOrNull()
            if (double != null) return double.toLong()
            return orElse()
        }

        private inline fun getTextValueDouble(sign: SignBlockEntity, index: Byte, orElse: () -> Double): Double {
            val component = sign.getText((index / SignText.LINES) % 2 == 0).getMessage(index % SignText.LINES, false)
                ?: return orElse()
            val text = component.string.trim()
            val double = text.toDoubleOrNull()
            if (double != null) return double
            return orElse()
        }
    }
}
