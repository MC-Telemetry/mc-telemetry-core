package de.mctelemetry.core.persistence

import de.mctelemetry.core.utils.ImposterNull
import de.mctelemetry.core.utils.ImposterNullMarker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function

open class DirtyCallbackMutableMap<K : Any, V : Any>(
    protected val backingMap: MutableMap<K, V>,
    protected val setDirty: () -> Unit,
) : MutableMap<K, V> by backingMap {

    override fun putIfAbsent(key: K, value: V): V? {
        return backingMap.putIfAbsent(key, value).also {
            if (it == null) setDirty()
        }
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        return backingMap.replace(key, oldValue, newValue).also {
            if (it) setDirty()
        }
    }

    override fun replace(key: K, value: V): V? {
        return backingMap.replace(key, value).also {
            if (it != null) setDirty()
        }
    }

    override fun replaceAll(function: BiFunction<in K, in V, out V?>) {
        return backingMap.replaceAll { k, v ->
            function.apply(k, v).also {
                if (it !== v)
                    setDirty()
            }
        }
    }

    @OptIn(ImposterNullMarker::class)
    override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V {
        // THE SIGNATURE OF THIS FUNCTION DOES NOT MATCH ITS ACTUAL SIGNATURE!
        // When called from JVM code, the actual return type of the mappingFunction can be null, because the
        // Java-Signature for this function does not match its Kotlin-Signature (the Java signature allows returning
        // null from the remapping function which then also returns null from computeIfAbsent, which the Kotlin
        // signature forbids (and cannot be changed because then it would no longer override the kotlin-defined
        // computeIfAbsent function)).
        //
        // Therefor this function is declared according to the Kotlin signature, but it must be able to accept nullable
        // mappingFunction for compatibility with Java, returning null in the case of a null result from
        // mappingFunction. Given that the nullable mappingFunction can only be used with the java signature, this is
        // still "safe" from both sides.
        //
        // While the mapping function could be declared as Function<in K, out V?>, this would imply to kotlin callers
        // that this function can return non-null V even if the mapping-function returns null, which would be incorrect.
        return backingMap.computeIfAbsent(key) { k ->
            (mappingFunction.apply(k) as V?).let {
                @Suppress("SENSELESS_COMPARISON")
                if (it != null) {
                    setDirty()
                    it
                } else {
                    ImposterNull.`null`()
                }
            }
        }
    }

    override fun computeIfPresent(
        key: K,
        remappingFunction: BiFunction<in K, in V, out V?>,
    ): V? {
        return backingMap.computeIfPresent(key) { k, v ->
            remappingFunction.apply(k, v).also {
                if (v !== it) setDirty()
            }
        }
    }

    override fun compute(key: K, remappingFunction: BiFunction<in K, in V?, out V?>): V? {
        return backingMap.compute(key) { k, v ->
            remappingFunction.apply(k, v).also {
                if (v !== it) setDirty()
            }
        }
    }

    override fun merge(
        key: K,
        value: V,
        remappingFunction: BiFunction<in V, in V, out V?>,
    ): V? {
        var remappedToSame = false
        return backingMap.merge(key, value) { k, v ->
            remappingFunction.apply(k, v).also {
                if (v !== it) setDirty()
                else remappedToSame = true
            }
        }.also {
            if (!remappedToSame) setDirty()
        }
    }

    override fun clear() {
        val wasEmpty = backingMap.isEmpty()
        return backingMap.clear().also {
            if (!wasEmpty) setDirty()
        }
    }


    override val keys: MutableSet<K>
        get() = keySet()
    override val values: MutableCollection<V>
        get() = values()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = entrySet()

    override fun put(key: K, value: V): V? {
        return backingMap.put(key, value).also {
            if (it !== value) setDirty()
        }
    }

    override fun remove(key: K): V? {
        return backingMap.remove(key).also {
            if(it == null) setDirty()
        }
    }

    override fun remove(key: K, value: V): Boolean {
        return backingMap.remove(key, value).also {
            if(it) setDirty()
        }
    }

    override fun putAll(from: Map<out K, V>) {
        return backingMap.putAll(from).also {
            // will also set dirty if backingMap[k]===from[k], but I don't think we can detect that
            if (from.isNotEmpty()) setDirty()
        }
    }

    override fun forEach(action: BiConsumer<in K, in V>) {
        backingMap.forEach(action)
    }

    fun entrySet(): DirtyCallbackMutableSet<MutableMap.MutableEntry<K, V>> {
        @Suppress("UNCHECKED_CAST")
        val backingSet: MutableSet<MutableMap.MutableEntry<K, V>> = backingMap.entries
        val lastEntrySetPair = lastEntrySet.get()
        if (lastEntrySetPair != null && lastEntrySetPair.first === backingSet) {
            return lastEntrySetPair.second
        } else {
            val result = DirtyCallbackMutableSet(
                backingSet,
                setDirty
            )
            lastEntrySet.compareAndSet(lastEntrySetPair, backingSet to result)
            return result
        }
    }

    fun keySet(): DirtyCallbackMutableSet<K> {
        @Suppress("UNCHECKED_CAST")
        val backingSet: MutableSet<K> = backingMap.keys
        val lastKeySetPair = lastKeySet.get()
        if (lastKeySetPair != null && lastKeySetPair.first === backingSet) {
            return lastKeySetPair.second
        } else {
            val result = DirtyCallbackMutableSet(
                backingSet,
                setDirty
            )
            lastKeySet.compareAndSet(lastKeySetPair, backingSet to result)
            return result
        }
    }

    fun values(): DirtyCallbackMutableCollection<V> {
        @Suppress("UNCHECKED_CAST")
        val backingCollection: MutableCollection<V> = backingMap.values
        val lastValuesPair = lastValuesCollection.get()
        if (lastValuesPair != null && lastValuesPair.first === backingCollection) {
            return lastValuesPair.second
        } else {
            val result = DirtyCallbackMutableCollection(
                backingCollection,
                setDirty
            )
            lastValuesCollection.compareAndSet(lastValuesPair, backingCollection to result)
            return result
        }
    }

    private val lastEntrySet: AtomicReference<Pair<MutableSet<MutableMap.MutableEntry<K, V>>, DirtyCallbackMutableSet<MutableMap.MutableEntry<K, V>>>?> =
        AtomicReference(null)
    private val lastKeySet: AtomicReference<Pair<MutableSet<K>, DirtyCallbackMutableSet<K>>?> =
        AtomicReference(null)
    private val lastValuesCollection: AtomicReference<Pair<MutableCollection<V>, DirtyCallbackMutableCollection<V>>?> =
        AtomicReference(null)
    override val size: Int
        get() = backingMap.size

    override fun containsKey(key: K): Boolean {
        return backingMap.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        return backingMap.containsValue(value)
    }

    override fun get(key: K): V? {
        return backingMap[key]
    }

    open class Concurrent<K : Any, V : Any>(
        backingMap: ConcurrentMap<K, V> = ConcurrentHashMap(),
        setDirty: () -> Unit,
    ) : DirtyCallbackMutableMap<K, V>(
        backingMap,
        setDirty
    ), ConcurrentMap<K,V> {

        override fun replaceAll(function: BiFunction<in K, in V, out V?>) {
            return super<DirtyCallbackMutableMap>.replaceAll(function)
        }

        override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V {
            return super<DirtyCallbackMutableMap>.computeIfAbsent(key, mappingFunction)
        }

        override fun computeIfPresent(
            key: K,
            remappingFunction: BiFunction<in K, in V, out V?>,
        ): V? {
            return super<DirtyCallbackMutableMap>.computeIfPresent(key, remappingFunction)
        }

        override fun compute(key: K, remappingFunction: BiFunction<in K, in V?, out V?>): V? {
            return super<DirtyCallbackMutableMap>.compute(key, remappingFunction)
        }

        override fun merge(key: K, value: V, remappingFunction: BiFunction<in V, in V, out V?>): V? {
            return super<DirtyCallbackMutableMap>.merge(key, value, remappingFunction)
        }

        override fun forEach(action: BiConsumer<in K, in V>) {
            return super<ConcurrentMap>.forEach(action)
        }
    }
}
