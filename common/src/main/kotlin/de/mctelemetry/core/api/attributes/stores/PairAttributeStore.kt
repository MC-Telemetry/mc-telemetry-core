package de.mctelemetry.core.api.attributes.stores

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue

class PairAttributeStore<T : Any>(
    val key: AttributeDataSource.Reference<T>,
    var value: T?,
    val parent: IAttributeValueStore = IAttributeValueStore.Companion.empty(),
) : IAttributeValueStore.Mutable {

    constructor(
        pair: Pair<AttributeDataSource.Reference<T>, T?>,
        parent: IAttributeValueStore = IAttributeValueStore.Companion.empty(),
    ) : this(pair.first, pair.second, parent)

    override val references: Set<AttributeDataSource.Reference<*>> by lazy {
        if (parent === IAttributeValueStore.Companion.empty()) setOf(key)
        else parent.references + key
    }

    override fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T? {
        return if (reference == key) {
            @Suppress("UNCHECKED_CAST")
            value as T?
                ?: throw NoSuchElementException("Key $reference is stored locally but has not been initialized")
        } else parent[reference]
    }

    private fun <T : Any> getValue(reference: AttributeDataSource.Reference<T>): T {
        if (key !== reference) throw IllegalArgumentException("Invoked getValue with foreign reference: $reference")
        @Suppress("UNCHECKED_CAST")
        return value as T?
            ?: throw NoSuchElementException("Key $reference is stored locally but has not been initialized")
    }


    override fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(reference: R): ((R) -> T)? {
        if (reference == key) {
            return ::getValue
        }
        return parent.prepareLookup(reference)
    }

    override fun <T2 : Any> set(
        reference: AttributeDataSource.Reference<T2>,
        value: T2?,
    ) {
        if (reference !== key) throw IllegalArgumentException("Tried to set value with foreign reference: $reference")
        @Suppress("UNCHECKED_CAST")
        this.value = value as T
    }

    override fun entries(): Sequence<MappedAttributeKeyValue<*, *>> {
        val value = value ?: return parent.entries()
        return parent.entries() + MappedAttributeKeyValue(key.info, value)
    }
}
