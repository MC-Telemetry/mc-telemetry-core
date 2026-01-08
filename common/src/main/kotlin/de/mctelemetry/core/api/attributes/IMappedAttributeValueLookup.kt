package de.mctelemetry.core.api.attributes

import kotlin.collections.associateWithTo
import kotlin.collections.mutableMapOf

interface IMappedAttributeValueLookup : IAttributeDateSourceReferenceSet {

    operator fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T?

    fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(reference: R): ((R) -> T)?

    interface Mutable : IMappedAttributeValueLookup {

        operator fun <T : Any> set(reference: AttributeDataSource.Reference<T>, value: T?)
    }

    companion object {

        private val Empty = object : IMappedAttributeValueLookup {
            override fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T? = null
            override val references: Set<AttributeDataSource.Reference<*>> = emptySet()

            override fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(reference: R): ((R) -> T)? = null
        }

        fun empty(): IMappedAttributeValueLookup = Empty
    }

    class PairLookup<T : Any>(
        val key: AttributeDataSource.Reference<T>,
        var value: T?,
        val parent: IMappedAttributeValueLookup = empty(),
    ) : Mutable {

        constructor(
            pair: Pair<AttributeDataSource.Reference<T>, T?>,
            parent: IMappedAttributeValueLookup = empty(),
        ) : this(pair.first, pair.second, parent)

        override val references: Set<AttributeDataSource.Reference<*>> by lazy {
            if (parent === Empty) setOf(key)
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
    }

    class MapLookup private constructor(
        private val data: MutableMap<AttributeDataSource.Reference<*>, Any?>,
        private val parent: IMappedAttributeValueLookup,
    ) : Mutable {

        constructor(
            data: MapLookup,
            parent: IMappedAttributeValueLookup = empty(),
        ) : this(data.data.toMutableMap(), parent)

        companion object {

            @JvmName("newFromReferences")
            operator fun invoke(
                data: Collection<AttributeDataSource.Reference<*>>,
                parent: IMappedAttributeValueLookup = empty(),
            ) = MapLookup(data.associateWithTo(mutableMapOf()) {
                null
            }, parent)

            @JvmName("newFromReferences")
            operator fun invoke(
                data: Map<AttributeDataSource.Reference<*>, Any?>,
                parent: IMappedAttributeValueLookup = empty(),
            ) = MapLookup(data.also {
                data.forEach { (key, value) ->
                    if (value == null) return@forEach
                    require(key.type.templateType.valueType.isInstance(value)) {
                        "Incompatible key and value: $key to $value"
                    }
                }
            }.toMutableMap(), parent)

            @JvmName("newFromAttributeKeyInfos")
            operator fun invoke(
                data: Collection<MappedAttributeKeyInfo<*, *>>,
                parent: IMappedAttributeValueLookup = empty(),
            ) = MapLookup(data.associateTo(mutableMapOf()) {
                AttributeDataSource.Reference.TypedSlot(it) to null
            }, parent)

            @JvmName("newFromAttributeKeyInfos")
            operator fun invoke(
                data: Map<MappedAttributeKeyInfo<*, *>, Any?>,
                parent: IMappedAttributeValueLookup = empty(),
            ) = MapLookup(data.mapKeysTo(mutableMapOf()) { (key, value) ->
                if (value != null) {
                    require(key.templateType.valueType.isInstance(value)) {
                        "Incompatible key and value: $key to $value"
                    }
                }
                AttributeDataSource.Reference.TypedSlot(key)
            }, parent)
        }

        override val references: Set<AttributeDataSource.Reference<*>> by lazy {
            if (parent === Empty) data.keys
            else data.keys.union(parent.references)
        }

        override fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T? {
            return if (reference in data) {
                @Suppress("UNCHECKED_CAST")
                data.getValue(reference) as T?
                    ?: throw java.util.NoSuchElementException("Key $reference is stored locally but has no value")
            } else {
                parent[reference]
            }
        }

        private fun <T : Any> getValue(info: AttributeDataSource.Reference<T>): T {
            @Suppress("UNCHECKED_CAST")
            return data.getValue(info) as T?
                ?: throw NoSuchElementException("Key $info is stored locally but has no value")
        }

        override fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(reference: R): ((R) -> T)? {
            return if (reference in data) ::getValue
            else parent.prepareLookup(reference)
        }

        fun <T : Any> update(key: AttributeDataSource.Reference<T>, value: T?) {
            require(key in data) {
                "Cannot add additional values after construction (tried to add $key=$value)"
            }
            data[key] = value
        }

        override fun <T : Any> set(reference: AttributeDataSource.Reference<T>, value: T?) {
            update(reference, value)
        }
    }
}
