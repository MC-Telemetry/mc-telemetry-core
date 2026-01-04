package de.mctelemetry.core.api.attributes

@JvmInline
value class MappedAttributeKeyMap<out T : Any> private constructor(val map: Map<MappedAttributeKeyInfo<*, *>, T>) :
        Collection<MappedAttributeKeyValue<*, *>> {

    constructor() : this(emptyMap())

    constructor(info: MappedAttributeKeyInfo<T, *>, value: T) : this(mapOf(info to value))

    constructor(entries: Collection<MappedAttributeKeyValue<T, *>>) : this(entries.associate { it.pair })

    operator fun <T2 : Any> get(info: MappedAttributeKeyInfo<T2, *>): T2? {
        @Suppress("UNCHECKED_CAST") // if reference matches, the associated value has to be of type T2
        return map[info] as T2?
    }

    operator fun <T2 : Any> get(reference: AttributeDataSource.Reference.TypedSlot<T2>): T2? =
        get(reference.info)

    private fun <T2 : Any> getOrThrow(reference: AttributeDataSource.Reference.TypedSlot<T2>): T2 {
        @Suppress("UNCHECKED_CAST")  // if reference matches, the associated value has to be of type T2
        return map.getValue(reference.info) as T2
    }
    private fun <T2 : Any> getOrThrow(info: MappedAttributeKeyInfo<T2,*>): T2 {
        @Suppress("UNCHECKED_CAST")  // if reference matches, the associated value has to be of type T2
        return map.getValue(info) as T2
    }

    fun <T2 : Any> prepareLookup(reference: AttributeDataSource.Reference.TypedSlot<T2>): ((AttributeDataSource.Reference.TypedSlot<T2>) -> T2)? {
        if (reference.info !in map) return null
        return this::getOrThrow
    }

    fun <T2 : Any> prepareLookup(info: MappedAttributeKeyInfo<T2,*>): ((AttributeDataSource.Reference.TypedSlot<T2>) -> T2)? {
        if (info !in map) return null
        return this::getOrThrow
    }

    val attributeKeys: Set<MappedAttributeKeyInfo<*, *>>
        get() = map.keys

    override fun contains(element: MappedAttributeKeyValue<*, *>): Boolean {
        return map[element.key] == element.value
    }

    override fun containsAll(elements: Collection<MappedAttributeKeyValue<*, *>>): Boolean {
        return elements.all(this::contains)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun iterator(): Iterator<MappedAttributeKeyValue<T, *>> = IteratorImpl(map.iterator())

    private class IteratorImpl<out T : Any>(private val baseIterator: Iterator<Map.Entry<MappedAttributeKeyInfo<*, *>, T>>) :
            Iterator<MappedAttributeKeyValue<T, *>> {

        override fun hasNext(): Boolean {
            return baseIterator.hasNext()
        }

        override fun next(): MappedAttributeKeyValue<T, *> {
            return baseIterator.next().let {
                // type matches because values of containing map are restricted by constructor
                @Suppress("UNCHECKED_CAST")
                MappedAttributeKeyValue(it.key, it.value) as MappedAttributeKeyValue<T, *>
            }
        }
    }

    override val size: Int
        get() = map.size
}
