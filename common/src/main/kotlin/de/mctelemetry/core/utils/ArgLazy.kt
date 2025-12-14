package de.mctelemetry.core.utils

import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import kotlin.reflect.KProperty

class ArgLazy<T, R : Any>(val block: (T) -> R) {

    private val map: MutableMap<T, WeakReference<R>> = Collections.synchronizedMap(WeakHashMap())
    operator fun getValue(thisRef: T, property: KProperty<*>): R {
        var result: R? = map[thisRef]?.get()
        if (result != null) return result
        // store strong reference with [result], as otherwise the WeakReference returned by `compute` could already
        // have been cleared by the time it returns.
        map.compute(thisRef) { key, value ->
            val preResult = value?.get()
            if (preResult == null) {
                result = block(key)
                return@compute WeakReference(result)
            } else {
                result = preResult
                value
            }
        }
        return result!!
    }
}
