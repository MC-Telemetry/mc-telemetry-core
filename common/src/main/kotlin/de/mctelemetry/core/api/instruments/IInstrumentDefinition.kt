package de.mctelemetry.core.api.instruments

import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.manager.client.IClientInstrumentManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.EnvironmentInterface
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

interface IInstrumentDefinition : IMetricDefinition {

    val attributes: Map<String, MappedAttributeKeyInfo<*, *, *>>
    val supportsFloating: Boolean

    @JvmRecord
    @EnvironmentInterface(
        value = EnvType.CLIENT,
        itf = IClientInstrumentManager.IClientInstrumentDefinition::class
    )
    data class Record(
        override val name: String,
        override val description: String = "",
        override val unit: String = "",
        override val attributes: Map<String, MappedAttributeKeyInfo<*, *, *>> = emptyMap(),
        override val supportsFloating: Boolean = false,
    ) : IInstrumentDefinition, IClientInstrumentManager.IClientInstrumentDefinition {


        init {
            attributes.forEach { (key, value) ->
                require(key == value.baseKey.key) {
                    "Illegal attributes mapping: key ${value.baseKey.key} is stored under $key"
                }
            }
        }

        constructor(
            name: String,
            description: String = "",
            unit: String = "",
            attributes: Collection<MappedAttributeKeyInfo<*, *, *>>,
            supportsFloating: Boolean = false,
        ) : this(
            name,
            description,
            unit,
            attributes.associateBy { it.baseKey.key },
            supportsFloating,
        )

        companion object {

            operator fun invoke(definition: IInstrumentDefinition): Record {
                if (definition is Record) return definition
                return Record(
                    name = definition.name,
                    description = definition.description,
                    unit = definition.unit,
                    attributes = definition.attributes,
                    supportsFloating = definition.supportsFloating,
                )
            }

            operator fun invoke(
                definition: IMetricDefinition,
                attributes: Collection<MappedAttributeKeyInfo<*, *, *>>,
                supportsFloating: Boolean,
            ): Record {
                if (definition is Record) {
                    if (
                        supportsFloating == definition.supportsFloating &&
                        definition.attributes.size == attributes.size &&
                        attributes.all {
                            definition.attributes[it.baseKey.key] == it
                        }
                    ) {
                        return definition
                    }
                }
                return Record(
                    name = definition.name,
                    description = definition.description,
                    unit = definition.unit,
                    attributes = attributes,
                    supportsFloating = supportsFloating,
                )
            }

            init {
                assert(OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT <= UByte.MAX_VALUE.toInt())
            }

            internal fun encodeInterface(bb: RegistryFriendlyByteBuf, v: IInstrumentDefinition) {
                when (v) {
                    is IWorldInstrumentDefinition -> {
                        bb.writeByte(1)
                        IWorldInstrumentDefinition.Record.encodeInterface(bb, v)
                    }
                    else -> {
                        bb.writeByte(0)
                        encodeAsSimpleRecord(bb, v)
                    }
                }
            }

            internal fun encodeAsSimpleRecord(bb: RegistryFriendlyByteBuf, v: IInstrumentDefinition) {
                val attributes = v.attributes
                val attributeCount = attributes.size.also {
                    require(it >= 0 && it < OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT)
                }
                bb.writeUtf(v.name, OTelCoreModAPI.Limits.INSTRUMENT_NAME_MAX_LENGTH)
                bb.writeUtf(v.description, OTelCoreModAPI.Limits.INSTRUMENT_DESCRIPTION_MAX_LENGTH)
                bb.writeUtf(v.unit, OTelCoreModAPI.Limits.INSTRUMENT_UNIT_MAX_LENGTH)
                bb.writeByte(attributeCount)
                for (attribute in attributes.values) {
                    MappedAttributeKeyInfo.STREAM_CODEC.encode(bb, attribute)
                }
                bb.writeBoolean(v.supportsFloating)
            }

            internal fun decodeInterface(bb: RegistryFriendlyByteBuf): IInstrumentDefinition {
                val type = bb.readByte()
                return when (type.toInt()) {
                    0 -> decodeAsSimpleRecord(bb)
                    1 -> IWorldInstrumentDefinition.Record.decodeInterface(bb)
                    else -> throw IllegalArgumentException("Unknown type $type")
                }
            }

            internal fun decodeAsSimpleRecord(bb: RegistryFriendlyByteBuf): Record {
                val name = bb.readUtf(OTelCoreModAPI.Limits.INSTRUMENT_NAME_MAX_LENGTH)
                val description = bb.readUtf(OTelCoreModAPI.Limits.INSTRUMENT_DESCRIPTION_MAX_LENGTH)
                val unit = bb.readUtf(OTelCoreModAPI.Limits.INSTRUMENT_UNIT_MAX_LENGTH)
                val attributeCount = bb.readUnsignedByte()
                require(attributeCount >= 0 && attributeCount < OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT)
                val attributes: Map<String, MappedAttributeKeyInfo<*, *, *>> =
                    if (attributeCount.toInt() == 0) emptyMap()
                    else buildMap {
                        repeat(attributeCount.toInt()) {
                            val attribute = MappedAttributeKeyInfo.STREAM_CODEC.decode(bb)
                            val existing = putIfAbsent(attribute.baseKey.key, attribute)
                            require(existing == null) { "Duplicate attributes for ${attribute.baseKey.key}: Stored $existing, tried to add $attribute" }
                        }
                    }
                val supportsFloating = bb.readBoolean()
                return Record(name, description, unit, attributes, supportsFloating)
            }

            val INTERFACE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IInstrumentDefinition> = StreamCodec.of(
                ::encodeInterface,
                ::decodeInterface,
            )
        }
    }
}
