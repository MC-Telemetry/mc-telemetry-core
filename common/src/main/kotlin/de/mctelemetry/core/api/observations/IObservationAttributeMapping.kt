package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import io.opentelemetry.api.common.Attributes
import net.minecraft.network.chat.MutableComponent

interface IObservationAttributeMapping {
    val mapping: Map<MappedAttributeKeyInfo<*, *, *>, AttributeDataSource<*>>
    val attributeDataSources: Collection<AttributeDataSource<*>>
    val instrumentAttributes: Set<MappedAttributeKeyInfo<*, *, *>>

    fun copy(): IObservationAttributeMapping
    fun validateTypes(force: Boolean = false): MutableComponent?
    fun validateStatic(force: Boolean = false): MutableComponent?
    fun validateTargets(
        targetAttributes: Collection<MappedAttributeKeyInfo<*, *, *>>,
        force: Boolean = false,
    ): MutableComponent?

    fun validateDynamic(
        targetAttributes: Collection<MappedAttributeKeyInfo<*, *, *>>,
        force: Boolean = false,
    ): MutableComponent?

    fun validate(
        targetAttributes: Collection<MappedAttributeKeyInfo<*, *, *>>,
        force: Boolean = false,
    ): MutableComponent?

    fun findUnusedAttributeDataSources(
        sourceAttributes: Collection<AttributeDataSource<*>>,
        output: MutableSet<AttributeDataSource<*>>,
    )

    context(attributeStore: IAttributeValueStore)
    fun resolveAttributes(): Attributes

    context(attributeStore: IAttributeValueStore)
    fun resolveAttributesToKeyValues(): List<MappedAttributeKeyValue<*, *>>

    operator fun get(instrumentAttribute: MappedAttributeKeyInfo<*, *, *>): AttributeDataSource<*>?
    fun plus(
        instrumentAttribute: MappedAttributeKeyInfo<*, *, *>,
        attributeDataSource: AttributeDataSource<*>
    ): IObservationAttributeMapping

    operator fun plus(entry: Pair<MappedAttributeKeyInfo<*, *, *>, AttributeDataSource<*>>): IObservationAttributeMapping

    operator fun minus(instrumentAttribute: MappedAttributeKeyInfo<*, *, *>): IObservationAttributeMapping
    fun filterForInstrument(instrumentAttributes: Collection<MappedAttributeKeyInfo<*, *, *>>): IObservationAttributeMapping
    fun filterForInstrument(definition: IInstrumentDefinition): IObservationAttributeMapping

    companion object {
        fun empty(): IObservationAttributeMapping = ObservationAttributeMapping.empty()

        context(attributeStore: IAttributeValueStore)
        fun resolveAttributesUnmapped(): Attributes = ObservationAttributeMapping.resolveAttributesUnmapped()

        context(attributeStore: IAttributeValueStore)
        fun resolveAttributesUnmappedToKeyValues(): List<MappedAttributeKeyValue<*, *>> = ObservationAttributeMapping.resolveAttributesUnmappedToKeyValues()
    }
}
