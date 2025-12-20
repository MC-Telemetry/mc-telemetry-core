package de.mctelemetry.core.api.attributes

@JvmInline
value class MappedAttributeKeyMap<out T : Any> private constructor(val map: Map<AttributeDataSource.ObservationSourceAttributeReference<*>, T>) :
        IMappedAttributeValueLookup,
        Collection<MappedAttributeKeyValue<*, *>> {

    constructor() : this(emptyMap())

    constructor(
        reference: AttributeDataSource.ObservationSourceAttributeReference<*>,
        value: T,
    ) : this(mapOf(reference to value))

    constructor(entries: Collection<MappedAttributeKeyValue<T, *>>) : this(entries.associate {
        AttributeDataSource.ObservationSourceAttributeReference(
            it.key
        ) to it.value
    })

    override fun <T2 : Any> get(reference: AttributeDataSource.ObservationSourceAttributeReference<T2>): T2? {
        @Suppress("UNCHECKED_CAST") // if reference matches, the associated value has to be of type T2
        return map[reference] as T2?
    }

    private fun <T2 : Any> getOrThrow(reference: AttributeDataSource.ObservationSourceAttributeReference<T2>): T2 {
        @Suppress("UNCHECKED_CAST")  // if reference matches, the associated value has to be of type T2
        return map.getValue(reference) as T2
    }

    override fun <T2 : Any> prepareLookup(reference: AttributeDataSource.ObservationSourceAttributeReference<T2>): ((AttributeDataSource.ObservationSourceAttributeReference<T2>) -> T2)? {
        if (reference !in map) return null
        return this::getOrThrow
    }

    override val references: Set<AttributeDataSource.ObservationSourceAttributeReference<*>>
        get() = map.keys

    fun <T : Any> contains(reference: AttributeDataSource<T>, value: T): Boolean {
        return containsImpl(reference, value as Any)
    }

    private fun containsImpl(reference: AttributeDataSource<*>, value: Any): Boolean {
        return map[reference] == value
    }

    operator fun contains(reference: AttributeDataSource<*>): Boolean {
        return reference in map
    }

    fun containsAllKeys(references: Collection<AttributeDataSource<*>>): Boolean {
        return map.keys.containsAll(references)
    }

    override fun contains(element: MappedAttributeKeyValue<*, *>): Boolean {
        return containsImpl(
            AttributeDataSource.ObservationSourceAttributeReference(element.key),
            element.value
        )
    }

    override fun containsAll(elements: Collection<MappedAttributeKeyValue<*, *>>): Boolean {
        return elements.all(this::contains)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun iterator(): Iterator<MappedAttributeKeyValue<T, *>> = IteratorImpl(map.iterator())

    private class IteratorImpl<out T : Any>(private val baseIterator: Iterator<Map.Entry<AttributeDataSource.ObservationSourceAttributeReference<*>, T>>) :
            Iterator<MappedAttributeKeyValue<T, *>> {

        override fun hasNext(): Boolean {
            return baseIterator.hasNext()
        }

        override fun next(): MappedAttributeKeyValue<T, *> {
            return baseIterator.next().let {
                // type matches because values of containing map are restricted by constructor
                @Suppress("UNCHECKED_CAST")
                MappedAttributeKeyValue(it.key.info, it.value) as MappedAttributeKeyValue<T, *>
            }
        }
    }

    override val size: Int
        get() = map.size
}
