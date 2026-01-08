package de.mctelemetry.core.api.observations.base

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeInstance
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.utils.runWithExceptionCleanup

abstract class ObservationSourceBase<SC> : IObservationSource<SC, IMappedAttributeValueLookup.MapLookup> {

    final override val attributes: IAttributeDateSourceReferenceSet by lazy {
        val attributes =
            pendingAttributeReferences ?: throw IllegalStateException("Internal pending attribute storage broken")
        runWithExceptionCleanup({ pendingAttributeReferences = attributes }) {
            pendingAttributeReferences = null
            IAttributeDateSourceReferenceSet.Companion(attributes.values)
        }
    }

    private var pendingAttributeReferences: MutableMap<String, AttributeDataSource.Reference.ObservationSourceAttributeReference<*>>? =
        mutableMapOf()

    protected fun <T : Any> IAttributeKeyTypeInstance<T, *>.createObservationAttributeReference(name: String): AttributeDataSource.Reference.ObservationSourceAttributeReference<T> {
        val references = pendingAttributeReferences
            ?: throw IllegalStateException("Cannot create attribute references after attributes have already been accessed")
        val newValue = AttributeDataSource.Reference.ObservationSourceAttributeReference(
            this@ObservationSourceBase,
            name,
            this
        )
        val oldValue = references.putIfAbsent(name, newValue)
        if (oldValue != null) {
            throw IllegalArgumentException("Attribute reference with name $name already exists on ${this@ObservationSourceBase}: $oldValue.")
        }
        return newValue
    }

    context(sourceContext: SC)
    override fun createAttributeStore(parent: IMappedAttributeValueLookup): IMappedAttributeValueLookup.MapLookup {
        return IMappedAttributeValueLookup.MapLookup(attributes.references, parent)
    }
}
