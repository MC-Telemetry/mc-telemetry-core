package de.mctelemetry.core.api.attributes.stores

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.MappedAttributeKeyMap
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue

interface IAttributeValueStore : IAttributeDateSourceReferenceSet {

    operator fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T?

    fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(reference: R): ((R) -> T)?

    fun entries(): Sequence<MappedAttributeKeyValue<*, *>> {
        return references.asSequence().mapNotNull {
            val value = get(it) ?: return@mapNotNull null
            MappedAttributeKeyValue(it.info, value)
        }
    }

    interface Mutable : IAttributeValueStore {

        operator fun <T : Any> set(reference: AttributeDataSource.Reference<T>, value: T?)
    }

    companion object {

        private val Empty = object : IAttributeValueStore {
            override fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T? = null
            override val references: Set<AttributeDataSource.Reference<*>> = emptySet()

            override fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(reference: R): ((R) -> T)? = null
            override fun entries(): Sequence<Nothing> = emptySequence()
        }

        fun empty(): IAttributeValueStore = Empty
    }

}
