package de.mctelemetry.core.persistence

import java.util.Set
import java.util.Spliterator
import java.util.function.IntFunction
import java.util.function.Predicate

open class DirtyCallbackMutableSet<S>(
    protected val backingSet: Set<S>,
    protected val setDirty: () -> Unit,
) : Set<S> by backingSet, MutableSet<S> {

    companion object {
        @Suppress("UNCHECKED_CAST")
        operator fun <S> invoke(backingSet: MutableSet<S>, setDirty: () -> Unit) = DirtyCallbackMutableSet<S>(
            backingSet as Set<S>,
            setDirty
        )
    }

    override fun add(element: S): Boolean {
        return backingSet.add(element).also {
            if (it) setDirty()
        }
    }

    override fun addAll(elements: Collection<S>): Boolean {
        return backingSet.addAll(elements).also {
            if (it) setDirty()
        }
    }

    override fun clear() {
        val wasEmpty: Boolean = backingSet.isEmpty()
        return backingSet.clear().also {
            if (!wasEmpty) setDirty()
        }
    }

    override fun spliterator(): Spliterator<S?> {
        return backingSet.spliterator()
    }

    override fun iterator(): MutableIterator<S> {
        val backingIterator = backingSet.iterator()
        return object: MutableIterator<S> by backingIterator {
            override fun remove() {
                return backingIterator.remove().also { setDirty() }
            }
        }
    }

    override fun remove(element: S): Boolean {
        return backingSet.remove(element).also {
            if(it) setDirty()
        }
    }

    override fun removeAll(elements: Collection<S>): Boolean {
        return backingSet.removeAll(elements).also {
            if(it) setDirty()
        }
    }

    override fun retainAll(elements: Collection<S>): Boolean {
        return backingSet.retainAll(elements).also {
            if(it) setDirty()
        }
    }

    @Deprecated("'fun <T : Any!> toArray(generator: IntFunction<Array<(out) T!>!>!): Array<(out) T!>!' is deprecated. This declaration is redundant in Kotlin and might be removed soon.")
    @Suppress("DEPRECATION")
    override fun <T : Any?> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?>? {
        return super<MutableSet>.toArray(generator)
    }

    override fun removeIf(filter: Predicate<in S>): Boolean {
        return backingSet.removeIf(filter).also {
            if(it) setDirty()
        }
    }
}
