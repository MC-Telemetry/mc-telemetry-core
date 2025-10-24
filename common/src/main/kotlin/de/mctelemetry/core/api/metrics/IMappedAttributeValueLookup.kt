package de.mctelemetry.core.api.metrics

interface IMappedAttributeValueLookup {

    operator fun <T : Any> get(info: MappedAttributeKeyInfo<T, *>): T?

    val keys: Set<MappedAttributeKeyInfo<*, *>>

    fun <T : Any> prepareLookup(info: MappedAttributeKeyInfo<T, *>): ((MappedAttributeKeyInfo<T, *>) -> T)?

    companion object {

        private val Empty = object : IMappedAttributeValueLookup {
            override fun <T : Any> get(info: MappedAttributeKeyInfo<T, *>): T? = null
            override val keys: Set<MappedAttributeKeyInfo<*, *>> = emptySet()
            override fun <T : Any> prepareLookup(info: MappedAttributeKeyInfo<T, *>): ((MappedAttributeKeyInfo<T, *>) -> T)? =
                null
        }

        fun empty(): IMappedAttributeValueLookup = Empty
    }

    class PairLookup<T : Any>(
        val key: MappedAttributeKeyInfo<T, *>, var value: T?,
        val parent: IMappedAttributeValueLookup = empty(),
    ) : IMappedAttributeValueLookup {

        constructor(pair: Pair<MappedAttributeKeyInfo<T, *>, T?>, parent: IMappedAttributeValueLookup = empty()) :
                this(pair.first, pair.second, parent)

        override val keys: Set<MappedAttributeKeyInfo<*, *>> by lazy {
            if (parent === Empty) setOf(key)
            else parent.keys + key
        }

        override fun <T : Any> get(info: MappedAttributeKeyInfo<T, *>): T? {
            return if (info == key) {
                @Suppress("UNCHECKED_CAST")
                value as T?
                    ?: throw NoSuchElementException("Key $info is stored locally but has not been initialized")
            } else parent[info]
        }

        private fun <T : Any> getValue(info: MappedAttributeKeyInfo<T, *>): T {
            if (key !== info) throw IllegalArgumentException("Invoked getValue with foreign keyInfo: $info")
            @Suppress("UNCHECKED_CAST")
            return value as T?
                ?: throw NoSuchElementException("Key $info is stored locally but has not been initialized")
        }

        override fun <T : Any> prepareLookup(info: MappedAttributeKeyInfo<T, *>): ((MappedAttributeKeyInfo<T, *>) -> T)? {
            if (info == key) {
                return ::getValue
            }
            return parent.prepareLookup(info)
        }
    }

    class MapLookup private constructor(
        private val data: MutableMap<MappedAttributeKeyInfo<*, *>, Any?>,
        private val parent: IMappedAttributeValueLookup,
    ) : IMappedAttributeValueLookup {

        constructor(
            data: MapLookup,
            parent: IMappedAttributeValueLookup = empty(),
        ) : this(data.data.toMutableMap(), parent)

        companion object {

            operator fun invoke(
                data: Map<MappedAttributeKeyInfo<*, *>, Any?>,
                parent: IMappedAttributeValueLookup = empty(),
            ) = MapLookup(data.also {
                data.forEach { (key, value) ->
                    if (value == null) return@forEach
                    require(key.type.valueType.isInstance(value)) {
                        "Incompatible key and value: $key to $value"
                    }
                }
            }.toMutableMap(), parent)
        }

        override val keys: Set<MappedAttributeKeyInfo<*, *>> by lazy {
            if (parent === Empty) data.keys
            else data.keys.union(parent.keys)
        }

        override fun <T : Any> get(info: MappedAttributeKeyInfo<T, *>): T? {
            return if (info in data) {
                @Suppress("UNCHECKED_CAST")
                data.getValue(info) as T?
                    ?: throw java.util.NoSuchElementException("Key $info is stored locally but has not been initialized")
            } else {
                parent[info]
            }
        }

        private fun <T : Any> getValue(info: MappedAttributeKeyInfo<T, *>): T {
            @Suppress("UNCHECKED_CAST")
            return data.getValue(info) as T?
                ?: throw NoSuchElementException("Key $info is stored locally but has not been initialized")
        }

        override fun <T : Any> prepareLookup(info: MappedAttributeKeyInfo<T, *>): ((MappedAttributeKeyInfo<T, *>) -> T)? {
            return if (info in data) ::getValue
            else parent.prepareLookup(info)
        }

        fun <T : Any> update(key: MappedAttributeKeyInfo<T, *>, value: T) {
            require(key in data) {
                "Cannot add additional values after construction (tried to add $key=$value)"
            }
            data[key] = value
        }


        operator fun <T : Any> set(key: MappedAttributeKeyInfo<T, *>, value: T) {
            update(key, value)
        }
    }
}
