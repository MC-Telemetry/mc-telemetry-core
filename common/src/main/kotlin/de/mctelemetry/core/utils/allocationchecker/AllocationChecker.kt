package de.mctelemetry.core.utils.allocationchecker

class AllocationChecker<K, C>(
    dimensions: Collection<Pair<K, Class<*>>>,
    val dimensionKeyComparator: Comparator<in K>,
) : MutableCollection<Allocation<K, *, C>> {

    companion object {
        operator fun <K : Comparable<K>, C> invoke(dimensions: Collection<Pair<K, Class<*>>>): AllocationChecker<K, C> {
            return AllocationChecker(dimensions, Comparator.naturalOrder())
        }
    }

    val dimensions: List<Pair<K, Class<*>>> =
        dimensions.sortedWith { d1, d2 -> dimensionKeyComparator.compare(d1.first, d2.first) }

    private val _allocations: MutableSet<Allocation<K, *, C>> = mutableSetOf()
    private val dimensionComparator: Comparator<DimensionAllocationEntry<K, *>> =
        compareBy(dimensionKeyComparator, DimensionAllocationEntry<K, *>::key)

    fun intersects(other: Allocation<K, *, *>): Boolean {
        other.requireCompatible(dimensions)
        return _allocations.any(other::intersectsUnsafe)
    }

    fun intersects(other: Collection<DimensionAllocationEntry<K, *>>): Boolean {
        return intersects(Allocation(other, dimensionComparator))
    }

    fun add(dimensionEntries: Collection<DimensionAllocationEntry<K, *>>, context: C): Boolean {
        return add(Allocation(dimensionEntries, dimensionKeyComparator, context))
    }

    override fun add(element: Allocation<K, *, C>): Boolean {
        element.requireCompatible(dimensions)
        return _allocations.add(element)
    }

    fun addIfNoIntersect(dimensionEntries: Collection<DimensionAllocationEntry<K, *>>, context: C): Boolean {
        return addIfNoIntersect(Allocation(dimensionEntries, dimensionKeyComparator, context))
    }

    fun addIfNoIntersect(element: Allocation<K, *, C>): Boolean {
        if (intersects(element)) return false
        // `requireCompatible` already invoked inside `intersects`
        return _allocations.add(element)
    }

    override fun addAll(elements: Collection<Allocation<K, *, C>>): Boolean {
        elements.forEach { it.requireCompatible(dimensions) }
        return _allocations.addAll(elements)
    }

    override fun clear() {
        _allocations.clear()
    }

    override fun iterator(): MutableIterator<Allocation<K, *, C>> {
        return _allocations.iterator()
    }

    override fun remove(element: Allocation<K, *, C>): Boolean {
        return _allocations.remove(element)
    }

    @Suppress("ConvertArgumentToSet") // delegate to _allocations
    override fun removeAll(elements: Collection<Allocation<K, *, C>>): Boolean {
        return _allocations.removeAll(elements)
    }

    @Suppress("ConvertArgumentToSet") // delegate to _allocations
    override fun retainAll(elements: Collection<Allocation<K, *, C>>): Boolean {
        return _allocations.retainAll(elements)
    }

    override val size: Int
        get() = _allocations.size

    override fun isEmpty(): Boolean {
        return _allocations.isEmpty()
    }

    override fun contains(element: Allocation<K, *, C>): Boolean {
        return _allocations.contains(element)
    }

    override fun containsAll(elements: Collection<Allocation<K, *, C>>): Boolean {
        return _allocations.containsAll(elements)
    }

    fun findIntersectionsFor(additional: Allocation<K, *, *>): Sequence<Allocation<K, *, C>> {
        additional.requireCompatible(dimensions)
        return _allocations.toList().asSequence().filter {
            it !== additional && it.intersectsUnsafe(additional)
        }
    }

    fun findIntersections(additional: Collection<Allocation<K, *, C>> = emptyList()): Sequence<Pair<Allocation<K, *, C>, Allocation<K, *, C>>> {
        val allocations: Array<Allocation<K, *, C>> =
            if (additional.isEmpty()) {
                _allocations.toTypedArray()
            } else {
                additional.forEach {
                    it.requireCompatible(dimensions)
                }
                (_allocations + additional).toTypedArray()
            }
        return sequence {
            for (i1 in allocations.indices) {
                val allocation1 = allocations[i1]
                for (i2 in i1 + 1..<allocations.size) {
                    val allocation2 = allocations[i2]
                    if (allocation1 intersectsUnsafe allocation2)
                        yield(Pair(allocation1, allocation2))
                }
            }
        }
    }
}
