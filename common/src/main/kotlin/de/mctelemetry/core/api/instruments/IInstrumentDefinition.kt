package de.mctelemetry.core.api.instruments

import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.StreamDecoder
import net.minecraft.network.codec.StreamEncoder

interface IInstrumentDefinition : IMetricDefinition {

    val attributes: Map<String, MappedAttributeKeyInfo<*, *>>

    @JvmRecord
    data class Record(
        override val name: String,
        override val description: String,
        override val unit: String,
        override val attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
    ) : IInstrumentDefinition {


        init {
            attributes.forEach { (key, value) ->
                require(key == value.baseKey.key) {
                    "Illegal attributes mapping: key ${value.baseKey.key} is stored under $key"
                }
            }
        }

        constructor(
            name: String,
            description: String,
            unit: String,
            attributes: Collection<MappedAttributeKeyInfo<*, *>>,
        ) : this(
            name,
            description,
            unit,
            attributes.associateBy { it.baseKey.key },
        )

        companion object {

            operator fun invoke(definition: IInstrumentDefinition): Record {
                if (definition is Record) return definition
                return Record(
                    name = definition.name,
                    description = definition.description,
                    unit = definition.unit,
                    attributes = definition.attributes,
                )
            }

            operator fun invoke(
                definition: IMetricDefinition,
                attributes: Collection<MappedAttributeKeyInfo<*, *>>,
            ): Record {
                if (definition is Record) {
                    if (definition.attributes.size == attributes.size && attributes.all {
                            definition.attributes[it.baseKey.key] == it
                        }) {
                        return definition
                    }
                }
                return Record(
                    name = definition.name,
                    description = definition.description,
                    unit = definition.unit,
                    attributes = attributes,
                )
            }

            init {
                assert(OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT <= UByte.MAX_VALUE.toInt())
            }

            private val interfaceEncoder: StreamEncoder<RegistryFriendlyByteBuf, IInstrumentDefinition> =
                StreamEncoder { bb, v ->
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
                }
            private val recordDecoder: StreamDecoder<RegistryFriendlyByteBuf, Record> = StreamDecoder { bb ->
                val name = bb.readUtf(OTelCoreModAPI.Limits.INSTRUMENT_NAME_MAX_LENGTH)
                val description = bb.readUtf(OTelCoreModAPI.Limits.INSTRUMENT_DESCRIPTION_MAX_LENGTH)
                val unit = bb.readUtf(OTelCoreModAPI.Limits.INSTRUMENT_UNIT_MAX_LENGTH)
                val attributeCount = bb.readUnsignedByte()
                require(attributeCount >= 0 && attributeCount < OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT)
                val attributes: Map<String, MappedAttributeKeyInfo<*, *>> =
                    if (attributeCount.toInt() == 0) emptyMap()
                    else buildMap {
                        repeat(attributeCount.toInt()) {
                            val attribute = MappedAttributeKeyInfo.STREAM_CODEC.decode(bb)
                            val existing = putIfAbsent(attribute.baseKey.key, attribute)
                            require(existing == null) { "Duplicate attributes for ${attribute.baseKey.key}: Stored $existing, tried to add $attribute" }
                        }
                    }
                Record(name, description, unit, attributes)
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Record> = StreamCodec.of(
                // Record is subtype of IInstrumentDefinition, but StreamEncoder does not have `in` variance.
                @Suppress("UNCHECKED_CAST")
                (interfaceEncoder as StreamEncoder<RegistryFriendlyByteBuf, Record>),
                recordDecoder,
            )
            val INTERFACE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IInstrumentDefinition> = StreamCodec.of(
                interfaceEncoder,
                // IInstrumentDefinition is supertype of Record, but StreamDecoder does not have `out` variance.
                @Suppress("UNCHECKED_CAST")
                (recordDecoder as StreamDecoder<RegistryFriendlyByteBuf, IInstrumentDefinition>),
            )
        }
    }
}
