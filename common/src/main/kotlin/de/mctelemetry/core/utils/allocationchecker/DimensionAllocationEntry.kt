package de.mctelemetry.core.utils.allocationchecker

import kotlin.contracts.contract

sealed class DimensionAllocationEntry<K, V>(val key: K) {

    sealed class Typed<K, V>(
        key: K,
        val type: Class<V>
    ) : DimensionAllocationEntry<K, V>(key) {

        protected fun isIntersectsAll(other: DimensionAllocationEntry<K, V>): Boolean {
            contract {
                returns(true) implies (other is All<K, V>)
                returns(false) implies (other is Typed<K, V>)
            }
            assert(key == other.key)
            if (other.javaClass == All::class.java) return true
            other as Typed<*, V>
            assert(type == other.type)
            return false
        }

        companion object {
            private fun <V> intersectSomeSome(first: Some<*, V>, second: Some<*, V>): Boolean {
                val elements1 = first.elements
                val elements2 = second.elements
                return if (elements1.size < elements2.size) {
                    elements1.any { it in elements2 }
                } else {
                    elements2.any { it in elements1 }
                }
            }

            private fun <V : Comparable<V>> intersectRangeSome(first: Range<*, V>, second: Some<*, V>): Boolean {
                return second.elements.any { it in first.range }
            }

            private fun <V : Comparable<V>> intersectRangeRange(first: Range<*, V>, second: Range<*, V>): Boolean {
                val start1 = first.range.start
                val end1 = first.range.endInclusive
                val start2 = second.range.endInclusive
                val end2 = second.range.endInclusive
                if (start1 > end1) return false // range1 empty
                if (start2 > end2) return false // range2 empty
                if (start2 > end1) return false // range2 larger than range1
                if (start1 > end2) return false // range1 larger than range2
                return true
            }
        }

        class Range<K, V : Comparable<V>>(
            key: K,
            type: Class<V>,
            val range: ClosedRange<V>,
        ) : Typed<K, V>(key, type) {

            override fun intersects(other: DimensionAllocationEntry<K, V>): Boolean {
                if (isIntersectsAll(other)) return true
                @Suppress("UNCHECKED_CAST") // casting to common type, actual types are safely restricted by `K,V` generics
                return when (other) {
                    is Range<*, *> -> intersectRangeRange(
                        this as Range<Any, Comparable<Any>>,
                        other as Range<Any, Comparable<Any>>
                    )

                    is Some<*, V> -> {
                        intersectRangeSome(this, other)
                    }
                }
            }
        }

        class Some<K, V>(
            key: K,
            type: Class<V>,
            val elements: Set<V>,
        ) : Typed<K, V>(key, type) {
            override fun intersects(other: DimensionAllocationEntry<K, V>): Boolean {
                if (isIntersectsAll(other)) return true
                @Suppress("UNCHECKED_CAST") // casting to common type, actual types are safely restricted by `K,V` generics
                return when (other) {
                    is Range<*, *> -> intersectRangeSome(
                        other as Range<Any, Comparable<Any>>,
                        this as Some<Any, Comparable<Any>>
                    )

                    is Some<*, V> -> {
                        intersectSomeSome(this, other)
                    }
                }
            }

            override fun toString(): String {
                return "Some($key, $type, ${elements.joinToString(prefix = "[", postfix = "]")})"
            }
        }
    }


    class All<K, V>(key: K) : DimensionAllocationEntry<K, V>(key) {
        override fun intersects(other: DimensionAllocationEntry<K, V>): Boolean {
            assert(key == other.key)
            return true
        }

        override fun toString(): String {
            return "All($key)"
        }
    }

    abstract infix fun intersects(other: DimensionAllocationEntry<K, V>): Boolean
}
