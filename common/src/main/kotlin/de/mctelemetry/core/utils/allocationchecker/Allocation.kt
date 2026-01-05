package de.mctelemetry.core.utils.allocationchecker

import kotlin.comparisons.compareBy

class Allocation<K, V, C> private constructor(
    val context: C,
    private val dimensions: Array<DimensionAllocationEntry<K, out V>>,
) {

    infix fun intersects(other: Allocation<K, *, *>): Boolean {
        require(dimensions.size == other.dimensions.size) { "Incompatible dimensions (wrong count): ${dimensions.size} != ${other.dimensions.size}" }
        for (i in 0 until dimensions.size) {
            val ownEntry = dimensions[i]
            val otherEntry = other.dimensions[i]
            require(ownEntry.key == otherEntry.key) { "Incompatible dimensions (key mismatch at $i): $ownEntry incompatible with $otherEntry" }
            if (ownEntry is DimensionAllocationEntry.Typed<*, *> && otherEntry is DimensionAllocationEntry.Typed<*, *>) {
                require(ownEntry.type == otherEntry.type) { "Incompatible types (type mismatch at $i, ${ownEntry.key}): $ownEntry incompatible with $otherEntry" }
            }
            @Suppress("UNCHECKED_CAST") // Cast to same type, compatibility guaranteed by earlier requires
            ownEntry as DimensionAllocationEntry<K, Any?>
            @Suppress("UNCHECKED_CAST") // Cast to same type, compatibility guaranteed by earlier requires
            otherEntry as DimensionAllocationEntry<K, Any?>
            if (!ownEntry.intersects(otherEntry)) return false
        }
        return true
    }

    internal infix fun intersectsUnsafe(other: Allocation<K, *, *>): Boolean {
        for (i in 0 until dimensions.size) {
            val ownEntry = dimensions[i]
            val otherEntry = other.dimensions[i]
            @Suppress("UNCHECKED_CAST") // Cast to same type, compatibility assumed ("unsafe") by caller calling to requireCompatible before
            ownEntry as DimensionAllocationEntry<K, Any?>
            @Suppress("UNCHECKED_CAST") // Cast to same type, compatibility assumed ("unsafe") by caller calling to requireCompatible before
            otherEntry as DimensionAllocationEntry<K, Any?>
            if (!ownEntry.intersects(otherEntry)) return false
        }
        return true
    }

    fun requireCompatible(types: List<Pair<K, Class<*>>>) {
        require(dimensions.size == types.size) { "Incompatible types (wrong count): ${dimensions.size} != ${types.size}" }
        types.forEachIndexed { index, (key, type) ->
            val dimensionEntry = dimensions[index]
            require(dimensionEntry.key == key) { "Incompatible types (key mismatch at $index): $dimensionEntry incompatible with ($key -> $type)" }
            require(dimensionEntry !is DimensionAllocationEntry.Typed<*, *> || dimensionEntry.type == type) { "Incompatible types (type mismatch at $index, $key): $dimensionEntry incompatible with ($key -> $type)" }
        }
    }

    override fun toString(): String {
        return "Allocation($context, [${dimensions.joinToString()}])"
    }

    companion object {

        @JvmName("new")
        operator fun <K : Comparable<K>, V, C> invoke(
            dimensions: Collection<DimensionAllocationEntry<K, out V>>,
            context: C,
        ): Allocation<K, V, C> {
            val sortedDimensions = dimensions.toTypedArray().also { it.sortBy(DimensionAllocationEntry<K, *>::key) }
            return Allocation(context, sortedDimensions)
        }

        @JvmName("new")
        operator fun <K : Comparable<K>, V> invoke(dimensions: Collection<DimensionAllocationEntry<K, out V>>): Allocation<K, V, Unit> {
            return invoke(dimensions, Unit)
        }

        @JvmName("new")
        operator fun <K, V, C> invoke(
            dimensions: Collection<DimensionAllocationEntry<K, out V>>,
            comparator: Comparator<in K>,
            context: C,
        ): Allocation<K, V, C> {
            return invoke(
                dimensions,
                compareBy(
                    comparator,
                    DimensionAllocationEntry<K, *>::key
                ),
                context
            )
        }

        @JvmName("new")
        operator fun <K, V> invoke(
            dimensions: Collection<DimensionAllocationEntry<K, out V>>,
            comparator: Comparator<in K>,
        ): Allocation<K, V, Unit> {
            return invoke(
                dimensions,
                compareBy(
                    comparator,
                    DimensionAllocationEntry<K, *>::key
                ),
                Unit
            )
        }

        @JvmName("newFromCachedComparator")
        operator fun <K, V, C> invoke(
            dimensions: Collection<DimensionAllocationEntry<K, out V>>,
            comparator: Comparator<in DimensionAllocationEntry<K, *>>,
            context: C,
        ): Allocation<K, V, C> {
            val sortedDimensions = dimensions.toTypedArray().also { it.sortWith(comparator) }
            return Allocation(context, sortedDimensions)
        }

        @JvmName("newFromCachedComparator")
        operator fun <K, V> invoke(
            dimensions: Collection<DimensionAllocationEntry<K, out V>>,
            comparator: Comparator<in DimensionAllocationEntry<K, *>>,
        ): Allocation<K, V, Unit> {
            return invoke(dimensions, comparator, Unit)
        }
    }

}
