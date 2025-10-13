package de.mctelemetry.core.persistence

import java.util.Spliterator
import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.stream.Stream

open class DirtyCallbackMutableCollection<S>(
    protected val backingCollection: MutableCollection<S>,
    protected val setDirty: () -> Unit,
) : MutableCollection<S> by backingCollection {

    override fun add(element: S): Boolean {
        return backingCollection.add(element).also {
            if (it) setDirty()
        }
    }

    override fun addAll(elements: Collection<S>): Boolean {
        return backingCollection.addAll(elements).also {
            if (it) setDirty()
        }
    }

    override fun clear() {
        val wasEmpty: Boolean = backingCollection.isEmpty()
        return backingCollection.clear().also {
            if (!wasEmpty) setDirty()
        }
    }

    override fun spliterator(): Spliterator<S?> {
        return backingCollection.spliterator()
    }

    override fun stream(): Stream<S> {
        return backingCollection.stream()
    }

    override fun parallelStream(): Stream<S> {
        return backingCollection.parallelStream()
    }

    override val size: Int
        get() = backingCollection.size

    override fun contains(element: S): Boolean {
        return backingCollection.contains(element)
    }

    override fun iterator(): MutableIterator<S> {
        val backingIterator = backingCollection.iterator()
        return object: MutableIterator<S> by backingIterator {
            override fun remove() {
                return backingIterator.remove().also { setDirty() }
            }
        }
    }

    override fun containsAll(elements: Collection<S>): Boolean {
        return backingCollection.containsAll(elements)
    }

    override fun remove(element: S): Boolean {
        return backingCollection.remove(element).also {
            if(it) setDirty()
        }
    }

    override fun removeAll(elements: Collection<S>): Boolean {
        return backingCollection.removeAll(elements).also {
            if(it) setDirty()
        }
    }

    override fun retainAll(elements: Collection<S>): Boolean {
        return backingCollection.retainAll(elements).also {
            if(it) setDirty()
        }
    }

    @Deprecated("'fun <T : Any!> toArray(generator: IntFunction<Array<(out) T!>!>!): Array<(out) T!>!' is deprecated. This declaration is redundant in Kotlin and might be removed soon.")
    @Suppress("DEPRECATION")
    override fun <T : Any?> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?>? {
        return super<MutableCollection>.toArray(generator)
    }

    override fun removeIf(filter: Predicate<in S>): Boolean {
        return backingCollection.removeIf(filter).also {
            if(it) setDirty()
        }
    }
}
