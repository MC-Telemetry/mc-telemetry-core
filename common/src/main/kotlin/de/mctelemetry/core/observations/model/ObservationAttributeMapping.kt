package de.mctelemetry.core.observations.model

import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue
import de.mctelemetry.core.api.attributes.canConvertTo
import de.mctelemetry.core.api.attributes.convertFrom
import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.codec.StreamCodec
import org.intellij.lang.annotations.MagicConstant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ObservationAttributeMapping(
    /**
     * This map is structured in "callback-direction" from the metric-attributes to the observation-attributes of the source.
     * Given a metric with attributes `A` and `B` and an observation source with attributes `X`, `Y` and `Z`, a valid
     * mapping might look like this:
     * ```json
     * {
     *   "A": "X",
     *   "B": "Z"
     * }
     *  ```
     *  where the observation attribute `Y` was not mapped to any metric attribute. While observation attributes are
     *  optional, all metric attributes MUST be assigned a type-assignable (see below for conversion) observation
     *  attribute.
     *
     *  The types of observation and metric attributes don't need to match exactly, so long as the observation attribute
     *  type is assignable to the type of the metric attribute. Type `X` is considered assignable to type `B` IFF at
     *  least one of the following is true:
     *  - `X === A`, which will result in no type-conversion
     *  - `X.assignableTo(A)`, which will result in the conversion `it: X -> X.convertTo(A, it) as A`
     *  - `A.assignableFrom(X)`, which will result in the conversion `it: X -> A.convertFrom(X, it) as A`
     **/
    mapping: Map<MappedAttributeKeyInfo<*, *>, AttributeDataSource<*>>,
) {

    // store mapping sorted by base key name to reduce later sorting overhead during OTel-Attributes construction
    val mapping: Map<MappedAttributeKeyInfo<*, *>, AttributeDataSource<*>> = mapping.toMap()

    val attributeDataSources: Collection<AttributeDataSource<*>>
        get() = mapping.values
    val instrumentAttributes: Set<MappedAttributeKeyInfo<*, *>>
        get() = mapping.keys

    private val validationFlags: AtomicInteger = AtomicInteger(0)

    fun copy(): ObservationAttributeMapping {
        return ObservationAttributeMapping(mapping)
    }

    fun resetValidationFlags() {
        validationFlags.set(0)
    }

    private inline fun cacheableValidation(
        @MagicConstant(
            intValues = [
                VALIDATION_FLAG_TYPES.toLong(),
                VALIDATION_FLAG_TARGET_ATTRIBUTES.toLong(),
            ]
        )
        flag: Int,
        force: Boolean,
        block: () -> MutableComponent?,
    ): MutableComponent? {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        val previousFlags = validationFlags.get()
        if ((!force) && ((previousFlags and flag) == flag)) return null
        val result = runWithExceptionCleanup(cleanup = {
            validationFlags.compareAndSet(previousFlags, previousFlags and flag.inv())
        }, block)
        if (result == null) {
            validationFlags.compareAndSet(previousFlags, previousFlags or flag)
        } else {
            validationFlags.compareAndSet(previousFlags, previousFlags and flag.inv())
        }
        return result
    }

    fun validateTypes(force: Boolean = false): MutableComponent? = cacheableValidation(VALIDATION_FLAG_TYPES, force) {
        for ((target, source) in mapping) {
            if (!(source.type.templateType canConvertTo target.templateType))
                return TranslationKeys.Errors.attributeTypesIncompatible(source, target)
        }
        return null
    }


    fun validateStatic(force: Boolean = false): MutableComponent? {
        return validateTypes(force)
    }

    fun validateTargets(
        targetAttributes: Collection<MappedAttributeKeyInfo<*, *>>,
        force: Boolean = false,
    ): MutableComponent? = cacheableValidation(VALIDATION_FLAG_TARGET_ATTRIBUTES, force) {
        for (target in targetAttributes) {
            if (target !in mapping) {
                return TranslationKeys.Errors.attributeMappingMissing(target)
            }
        }
        return null
    }

    fun validateDynamic(
        targetAttributes: Collection<MappedAttributeKeyInfo<*, *>>,
        force: Boolean = false,
    ): MutableComponent? {
        return validateTargets(targetAttributes, force)
    }

    fun validate(
        targetAttributes: Collection<MappedAttributeKeyInfo<*, *>>,
        force: Boolean = false,
    ): MutableComponent? {
        if ((!force) && ((validationFlags.get() and VALIDATION_COMPLETE) == VALIDATION_COMPLETE)) return null
        return validateStatic() ?: validateDynamic(targetAttributes)
    }

    fun findUnusedAttributeDataSources(
        sourceAttributes: Collection<AttributeDataSource<*>>,
        output: MutableSet<AttributeDataSource<*>>,
    ) {
        output.addAll(sourceAttributes)
        output.removeAll(this.attributeDataSources)
    }

    context(attributeStore: IAttributeValueStore)
    fun resolveAttributes(): Attributes {
        if (mapping.isEmpty()) {
            return Attributes.empty()
        }
        return mapping.entries.fold(Attributes.builder()) { builder, (metricAttribute, attributeDataSource) ->
            addConverted(metricAttribute, attributeDataSource, builder)
        }.build()
    }

    context(attributeStore: IAttributeValueStore)
    fun resolveAttributesToKeyValues(): List<MappedAttributeKeyValue<*, *>> {
        if (mapping.isEmpty()) {
            return emptyList()
        }
        return mapping.entries.map { (metricAttribute, attributeDataSource) ->
            makeConverted(metricAttribute, attributeDataSource)
        }
    }

    fun saveToTag(): Tag {
        return ListTag().also { listTag ->
            for ((key, value) in mapping) {
                listTag.add(CompoundTag().also { entryTag ->
                    entryTag.put("key", key.save())
                    entryTag.put("value", AttributeDataSource.toNbt(value))
                })
            }
        }
    }

    operator fun get(instrumentAttribute: MappedAttributeKeyInfo<*, *>): AttributeDataSource<*>? =
        mapping[instrumentAttribute]

    fun plus(instrumentAttribute: MappedAttributeKeyInfo<*, *>, attributeDataSource: AttributeDataSource<*>) =
        this + (instrumentAttribute to attributeDataSource)

    operator fun plus(entry: Pair<MappedAttributeKeyInfo<*, *>, AttributeDataSource<*>>): ObservationAttributeMapping =
        ObservationAttributeMapping(mapping + entry)

    operator fun minus(instrumentAttribute: MappedAttributeKeyInfo<*, *>): ObservationAttributeMapping {
        val other = mapping - instrumentAttribute
        return if (other.size == mapping.size)
            this
        else
            ObservationAttributeMapping(other)
    }

    fun filterForInstrument(instrumentAttributes: Collection<MappedAttributeKeyInfo<*, *>>): ObservationAttributeMapping {
        val unneededKeys = mapping.keys - instrumentAttributes.toSet()
        return if (unneededKeys.isEmpty())
            this
        else
            ObservationAttributeMapping(mapping - unneededKeys)
    }

    fun filterForInstrument(definition: IInstrumentDefinition): ObservationAttributeMapping {
        return filterForInstrument(definition.attributes.values)
    }

    companion object {

        init {
            assert(OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT <= UByte.MAX_VALUE.toInt())
        }

        val VALIDATION_ERRORS_STATIC = setOf(
            TranslationKeys.Errors.ATTRIBUTES_TYPE_INCOMPATIBLE,
            TranslationKeys.Errors.ATTRIBUTES_TYPE_INCOMPATIBLE_DETAILED,
            TranslationKeys.Errors.ATTRIBUTES_TYPE_INCOMPATIBLE_TARGET_DETAILED,
        )

        val VALIDATION_ERRORS_DYNAMIC = setOf(
            TranslationKeys.Errors.ATTRIBUTES_MAPPING_MISSING,
        )

        val VALIDATION_ERRORS = VALIDATION_ERRORS_STATIC + VALIDATION_ERRORS_DYNAMIC

        private val empty = ObservationAttributeMapping(emptyMap())

        fun empty(): ObservationAttributeMapping = empty

        private const val VALIDATION_FLAG_TYPES = 1 shl 0
        private const val VALIDATION_FLAG_TARGET_ATTRIBUTES = 1 shl 1
        private const val VALIDATION_COMPLETE =
            VALIDATION_FLAG_TYPES or
                    VALIDATION_FLAG_TARGET_ATTRIBUTES

        //private val comparator: Comparator<MappedAttributeKeyInfo<*, *>> = Comparator.comparing { it.baseKey.key }


        context(attributeStore: IAttributeValueStore)
        fun resolveAttributesUnmapped(): Attributes {
            return attributeStore.references.fold(Attributes.builder()) { builder, reference ->
                addConverted(reference.info, reference, builder)
            }.build()
        }

        context(attributeStore: IAttributeValueStore)
        fun resolveAttributesUnmappedToKeyValues(): List<MappedAttributeKeyValue<*, *>> {
            return attributeStore.references.map { reference ->
                makeConverted(reference.info, reference)
            }
        }

        context(attributeStore: IAttributeValueStore)
        private fun <T : Any, B : Any> addConverted(
            metricAttribute: MappedAttributeKeyInfo<T, B>,
            attributeDataSource: AttributeDataSource<*>,
            builder: AttributesBuilder,
        ): AttributesBuilder {
            val metricAttributeType: IAttributeKeyTypeTemplate<T, B> = metricAttribute.templateType
            val metricAttributeKey: AttributeKey<B> = metricAttribute.baseKey
            val value: T = lookupConverted(metricAttributeType, attributeDataSource)
            return builder.put(metricAttributeKey, metricAttributeType.format(value))
        }


        context(attributeStore: IAttributeValueStore)
        private fun <T : Any, I : MappedAttributeKeyInfo<T, *>> makeConverted(
            metricAttribute: I,
            attributeDataSource: AttributeDataSource<*>,
        ): MappedAttributeKeyValue<T, I> {
            val value: T = lookupConverted(metricAttribute.templateType, attributeDataSource)
            return MappedAttributeKeyValue(metricAttribute, value)
        }

        context(attributeStore: IAttributeValueStore)
        private fun <T : Any, R : Any, B : Any> lookupConverted(
            metricAttributeType: IAttributeKeyTypeTemplate<T, B>,
            attributeDataSource: AttributeDataSource<R>,
        ): T {
            val value = attributeDataSource.value
                ?: throw NoSuchElementException("Could not obtain value for $attributeDataSource")
            return metricAttributeType.convertFrom(attributeDataSource.type.templateType, value)
                ?: throw IllegalArgumentException("Could not convert value from ${attributeDataSource.type} to $metricAttributeType: $value")
        }

        fun loadFromTag(
            tag: Tag,
            holderLookupProvider: HolderLookup.Provider,
            sourceContext: IObservationSource<*, *>,
        ): ObservationAttributeMapping {
            tag as ListTag
            if (tag.isEmpty()) return empty()
            require(tag.elementType == Tag.TAG_COMPOUND)
            return ObservationAttributeMapping(
                buildMap {
                    for (entryTag in tag) {
                        entryTag as CompoundTag
                        val keyTag = entryTag.getCompound("key")
                        val valueTag = entryTag.getCompound("value")
                        val key = MappedAttributeKeyInfo.load(
                            keyTag, holderLookupProvider.asGetterLookup().lookupOrThrow(
                                OTelCoreModAPI.AttributeTypeMappings
                            )
                        )
                        val value = AttributeDataSource.fromNbt(
                            valueTag, holderLookupProvider, sourceContext
                        )
                        put(key, value)
                    }
                }
            )
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationAttributeMapping> = StreamCodec.of(
            { bb, v ->
                val mapping = v.mapping
                bb.writeByte(mapping.size.also {
                    require(0 <= it && it < OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT)
                })
                mapping.forEach { (key, value) ->
                    MappedAttributeKeyInfo.STREAM_CODEC.encode(bb, key)
                    AttributeDataSource.STREAM_CODEC.encode(bb, value)
                }
            },
            { bb ->
                val mappingSize = bb.readUnsignedByte().also {
                    require(0 <= it && it < OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_MAX_COUNT)
                }.toInt()
                val mapping: Map<MappedAttributeKeyInfo<*, *>, AttributeDataSource<*>> =
                    buildMap(mappingSize) {
                        repeat(mappingSize) { _ ->
                            val key = MappedAttributeKeyInfo.STREAM_CODEC.decode(bb)
                            val value = AttributeDataSource.STREAM_CODEC.decode(bb)
                            val storedValue = putIfAbsent(key, value)
                            require(storedValue == null) { "Duplicate key $key" }
                        }
                    }
                ObservationAttributeMapping(mapping)
            }
        )
    }
}
