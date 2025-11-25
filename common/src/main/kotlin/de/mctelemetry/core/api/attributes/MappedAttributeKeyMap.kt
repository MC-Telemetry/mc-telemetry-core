package de.mctelemetry.core.api.attributes

@JvmInline
value class MappedAttributeKeyMap<out T : Any> private constructor(val map: Map<MappedAttributeKeyInfo<*, *>, T>) : IMappedAttributeValueLookup {

    constructor() : this(emptyMap())

    constructor(info: MappedAttributeKeyInfo<T,*>, value: T) : this(mapOf(info to value))

    constructor(entries: Collection<MappedAttributeKeyValue<T, *>>) : this(entries.associate { it.pair })

    override fun <T2 : Any> get(info: MappedAttributeKeyInfo<T2, *>): T2? {
        @Suppress("UNCHECKED_CAST") // if info matches, the associated value has to be of type T2
        return map[info] as T2?
    }

    private fun <T2 : Any> getOrThrow(info: MappedAttributeKeyInfo<T2, *>): T2 {
        @Suppress("UNCHECKED_CAST")  // if info matches, the associated value has to be of type T2
        return map.getValue(info) as T2
    }

    override fun <T2 : Any> prepareLookup(info: MappedAttributeKeyInfo<T2, *>): ((MappedAttributeKeyInfo<T2, *>) -> T2)? {
        if(info !in map) return null
        return this::getOrThrow
    }

    override val keys: Set<MappedAttributeKeyInfo<*, *>>
        get() = map.keys
}
