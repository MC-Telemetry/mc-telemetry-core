package de.mctelemetry.core.network.observations.container.observationrequest

import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.MappedAttributeKeyMap
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue.Companion.decodeToValue
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import java.lang.AssertionError

data class RecordedObservations(
    val instrument: IInstrumentDefinition?,
    val observations: Map<List<MappedAttributeKeyValue<*, *>>, RecordedObservationPoint>,
) {

    object STREAM_CODEC : StreamCodec<RegistryFriendlyByteBuf, RecordedObservations> {

        private const val FLAG_HASLONG = 0b0000_0001
        private const val FLAG_HASDOUBLE = 0b0000_0010

        // structure:
        //   - has_instrument bool
        //   - if has_instrument
        //       - [instrument]
        //   - number of attributeKeyInfo not in [instrument]
        //   - List<attributeKeyInfo not in [instrument]>
        //   - number of unique MappedAttributeKeyValue used in observations
        //   - for each MappedAttributeKeyValue in observations
        //       - var-int id referencing either
        //         instrument attributeKeyInfo (if positive, index based on alphabetical sorting by baseKey-name)
        //         or extra attributeKeyInfo (if negative, e.g. -1 references extra observation at index 0)
        //       - data encoded by MappedAttributeKeyValue.info.type.valueTypeEncoder
        //   - size of [observations]
        //   - for each observation:
        //       - intList of ids referencing the MappedAttributeKeyValues encoded earlier
        //       - 1 byte for flags: 0b0000_0001 for "hasLong" and 0b0000_0010 for "hasDouble"
        //       - if "hasLong": [longValue] of observation point
        //       - if "hasDouble": [doubleValue] of observation point

        override fun decode(`object`: RegistryFriendlyByteBuf): RecordedObservations {
            val hasDefinition: Boolean = `object`.readBoolean()
            val definition: IInstrumentDefinition? = if (hasDefinition)
                IInstrumentDefinition.Record.INTERFACE_STREAM_CODEC.decode(`object`)
            else
                null
            val builtInAttributeInfos: Array<MappedAttributeKeyInfo<*, *>> =
                definition?.attributes?.values?.toTypedArray() ?: emptyArray()
            val extraAttributeInfosSize = `object`.readVarInt()
            val extraAttributeInfos: Array<MappedAttributeKeyInfo<*, *>> =
                if (extraAttributeInfosSize == 0) emptyArray()
                else Array(extraAttributeInfosSize) {
                    MappedAttributeKeyInfo.STREAM_CODEC.decode(`object`)
                }
            val attributeValuesSize = `object`.readVarInt()
            val attributeValues: Array<MappedAttributeKeyValue<*, *>> = if (attributeValuesSize == 0) emptyArray()
            else Array(attributeValuesSize) {
                val infoId = `object`.readVarInt()
                val info: MappedAttributeKeyInfo<*, *> = if (infoId >= 0) {
                    builtInAttributeInfos[infoId]
                } else {
                    extraAttributeInfos[infoId.inv()]
                }
                info.decodeToValue(`object`)
            }
            val observationCount = `object`.readVarInt()
            val observations: Map<List<MappedAttributeKeyValue<*, *>>, RecordedObservationPoint> =
                if (observationCount == 0) emptyMap()
                else buildMap(observationCount) {
                    repeat(observationCount) {
                        val attributeValuesSize = `object`.readVarInt()
                        val attributeValues: List<MappedAttributeKeyValue<*, *>> =
                            if (attributeValuesSize == 0) emptyList()
                            else buildList(attributeValuesSize) attributeValues@{
                                repeat(attributeValuesSize) {
                                    val attributeValueId = `object`.readVarInt()
                                    val attributeValue = attributeValues[attributeValueId]
                                    this@attributeValues.add(attributeValue)
                                }
                            }
                        val attributeValuesMap: MappedAttributeKeyMap<*> = MappedAttributeKeyMap(attributeValues)
                        val flags = `object`.readByte().toInt()
                        val observationPoint: RecordedObservationPoint =
                            if (flags and FLAG_HASLONG != 0) {
                                val longValue = `object`.readLong()
                                if (flags and FLAG_HASDOUBLE != 0) {
                                    val doubleValue = `object`.readDouble()
                                    RecordedObservationPoint(
                                        attributeValuesMap,
                                        doubleValue = doubleValue,
                                        longValue = longValue,
                                    )
                                } else {
                                    RecordedObservationPoint(
                                        attributeValuesMap,
                                        longValue = longValue,
                                    )
                                }
                            } else if (flags and FLAG_HASDOUBLE != 0) {
                                val doubleValue = `object`.readDouble()
                                RecordedObservationPoint(
                                    attributeValuesMap,
                                    doubleValue = doubleValue,
                                )
                            } else {
                                throw IllegalArgumentException("Cannot decode observation point with neither FLAG_HASDOUBLE or FLAG_HASLONG set: ${flags.toHexString()}")
                            }
                        put(attributeValues, observationPoint)
                    }
                }
            return RecordedObservations(definition, observations)
        }

        override fun encode(`object`: RegistryFriendlyByteBuf, object2: RecordedObservations) {
            val instrument = object2.instrument
            if (instrument != null) {
                `object`.writeBoolean(true)
                IInstrumentDefinition.Record.INTERFACE_STREAM_CODEC.encode(`object`, instrument)
            } else {
                `object`.writeBoolean(false)
            }
            val usedAttributeValues = object2.observations.keys.flatMapTo(mutableSetOf()) { it }.toTypedArray()
            val attributeValuesToIndex: Object2IntMap<MappedAttributeKeyValue<*, *>> =
                if (usedAttributeValues.isEmpty()) Object2IntMaps.emptyMap()
                else {
                    Object2IntOpenHashMap(usedAttributeValues, IntArray(usedAttributeValues.size) { it })
                }
            val usedAttributeInfos = usedAttributeValues.mapTo(mutableSetOf()) { it.info }
            val instrumentAttributeInfos = instrument?.attributes?.values.orEmpty()
            val additionalAttributeInfos = (usedAttributeInfos - instrumentAttributeInfos).toTypedArray()
            val attributeInfosToIndex: Object2IntMap<MappedAttributeKeyInfo<*, *>> =
                if (usedAttributeInfos.isEmpty()) Object2IntMaps.emptyMap()
                else {
                    Object2IntOpenHashMap<MappedAttributeKeyInfo<*, *>>(usedAttributeInfos.size).apply {
                        usedAttributeInfos.forEach { info ->
                            val instrumentIndex = instrumentAttributeInfos.indexOf(info)
                            val infoIndex = if (instrumentIndex >= 0) {
                                instrumentIndex
                            } else {
                                val additionalIndex = additionalAttributeInfos.indexOf(info)
                                if (additionalIndex < 0) {
                                    throw AssertionError("Expected $info to be either in instrumentAttributeInfos ($instrumentAttributeInfos) or additionalAttributeInfos ($additionalAttributeInfos), but was found nowhere.")
                                }
                                additionalIndex.inv()
                            }
                            this.put(info, infoIndex)
                        }
                    }
                }
            `object`.writeVarInt(additionalAttributeInfos.size)
            for (additionalInfo in additionalAttributeInfos) {
                MappedAttributeKeyInfo.STREAM_CODEC.encode(`object`, additionalInfo)
            }
            `object`.writeVarInt(usedAttributeValues.size)
            for (usedAttributeValue in usedAttributeValues) {
                `object`.writeVarInt(attributeInfosToIndex.getInt(usedAttributeValue.info))
                usedAttributeValue.encodeValue(`object`)
            }
            `object`.writeVarInt(object2.observations.size)
            object2.observations.forEach { (attributeValues, observationPoint) ->
                `object`.writeVarInt(attributeValues.size)
                for (attributeValue in attributeValues) {
                    `object`.writeVarInt(attributeValuesToIndex.getInt(attributeValue))
                }
                val hasLong = observationPoint.hasLong
                val hasDouble = observationPoint.hasDouble
                var flags = 0
                if (hasLong) {
                    flags = flags or FLAG_HASLONG
                }
                if (hasDouble) {
                    flags = flags or FLAG_HASDOUBLE
                }
                `object`.writeByte(flags)
                if (hasLong) {
                    `object`.writeLong(observationPoint.longValue)
                }
                if (hasDouble) {
                    `object`.writeDouble(observationPoint.doubleValue)
                }
            }
        }
    }
}
