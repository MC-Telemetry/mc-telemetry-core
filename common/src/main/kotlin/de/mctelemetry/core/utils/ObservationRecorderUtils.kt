package de.mctelemetry.core.utils

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Long) = observe(value, source)

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Double) = observe(value, source)

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Int) = observe(value.toLong(), source)

context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observe(value: Int, source: IObservationSourceInstance<*, *, *>) =
    observe(value.toLong(), source)

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, long: Long) =
    observePreferred(double, long, source)

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observePreferred(double: Double, int: Int) =
    observePreferred(double, int.toLong(), source)

context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.observePreferred(
    double: Double,
    int: Int,
    source: IObservationSourceInstance<*, *, *>
) = observePreferred(double, int.toLong(), source)


context(attributeStore: IAttributeValueStore)
inline fun IObservationRecorder.Unresolved.observePreferred(
    doubleBlock: () -> Double,
    longBlock: () -> Long,
    source: IObservationSourceInstance<*, *, *>
) {
    contract {
        callsInPlace(doubleBlock, InvocationKind.AT_MOST_ONCE)
        callsInPlace(longBlock, InvocationKind.AT_MOST_ONCE)
    }
    if (supportsFloating)
        observe(doubleBlock(), source)
    else
        observe(longBlock(), source)
}

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
inline fun IObservationRecorder.Unresolved.observePreferred(
    doubleBlock: () -> Double,
    longBlock: () -> Long,
) {
    contract {
        callsInPlace(doubleBlock, InvocationKind.AT_MOST_ONCE)
        callsInPlace(longBlock, InvocationKind.AT_MOST_ONCE)
    }
    observePreferred(doubleBlock, longBlock, source)
}


context(attributeStore: IAttributeValueStore)
inline fun <T> IObservationRecorder.Unresolved.observePreferredAccumulatedNullable(
    values: Iterable<T>,
    doubleBlock: (T) -> Double?,
    longBlock: (T) -> Long?,
    doubleAccumulator: (Double, Double) -> Double = Double::plus,
    longAccumulator: (Long, Long) -> Long = Long::plus,
    source: IObservationSourceInstance<*, *, *>,
) {
    contract {
        callsInPlace(doubleBlock, InvocationKind.UNKNOWN)
        callsInPlace(longBlock, InvocationKind.UNKNOWN)
        callsInPlace(doubleAccumulator, InvocationKind.UNKNOWN)
        callsInPlace(longAccumulator, InvocationKind.UNKNOWN)
    }
    var hasValue = false
    if (supportsFloating) {
        var accumulator = 0.0
        for (value in values) {
            if (hasValue) {
                accumulator = doubleAccumulator(accumulator, doubleBlock(value) ?: continue)
            } else {
                accumulator = doubleBlock(value) ?: continue
                hasValue = true
            }
        }
        if (hasValue) {
            observe(accumulator, source)
        }
    } else {
        var accumulator: Long = 0
        for (value in values) {
            if (hasValue) {
                accumulator = longAccumulator(accumulator, longBlock(value) ?: continue)
            } else {
                accumulator = longBlock(value) ?: continue
                hasValue = true
            }
        }
        if (hasValue) {
            observe(accumulator, source)
        }
    }
}

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
inline fun <T> IObservationRecorder.Unresolved.observePreferredAccumulatedNullable(
    values: Iterable<T>,
    doubleBlock: (T) -> Double?,
    longBlock: (T) -> Long?,
    doubleAccumulator: (Double, Double) -> Double = Double::plus,
    longAccumulator: (Long, Long) -> Long = Long::plus,
) {
    contract {
        callsInPlace(doubleBlock, InvocationKind.UNKNOWN)
        callsInPlace(longBlock, InvocationKind.UNKNOWN)
        callsInPlace(doubleAccumulator, InvocationKind.UNKNOWN)
        callsInPlace(longAccumulator, InvocationKind.UNKNOWN)
    }
    observePreferredAccumulatedNullable(values, doubleBlock, longBlock, doubleAccumulator, longAccumulator, source)
}

context(attributeStore: IAttributeValueStore)
inline fun <T> IObservationRecorder.Unresolved.observePreferredAccumulated(
    values: Iterable<T>,
    doubleBlock: (T) -> Double,
    longBlock: (T) -> Long,
    doubleAccumulator: (Double, Double) -> Double = Double::plus,
    longAccumulator: (Long, Long) -> Long = Long::plus,
    source: IObservationSourceInstance<*, *, *>,
) {
    contract {
        callsInPlace(doubleBlock, InvocationKind.UNKNOWN)
        callsInPlace(longBlock, InvocationKind.UNKNOWN)
        callsInPlace(doubleAccumulator, InvocationKind.UNKNOWN)
        callsInPlace(longAccumulator, InvocationKind.UNKNOWN)
    }
    var hasValue = false
    if (supportsFloating) {
        var accumulator = 0.0
        for (value in values) {
            if (hasValue) {
                accumulator = doubleAccumulator(accumulator, doubleBlock(value))
            } else {
                accumulator = doubleBlock(value)
                hasValue = true
            }
        }
        if (hasValue) {
            observe(accumulator, source)
        }
    } else {
        var accumulator: Long = 0
        for (value in values) {
            if (hasValue) {
                accumulator = longAccumulator(accumulator, longBlock(value))
            } else {
                accumulator = longBlock(value)
                hasValue = true
            }
        }
        if (hasValue) {
            observe(accumulator, source)
        }
    }
}

context(source: IObservationSourceInstance<*, *, *>, attributeStore: IAttributeValueStore)
inline fun <T> IObservationRecorder.Unresolved.observePreferredAccumulated(
    values: Iterable<T>,
    doubleBlock: (T) -> Double,
    longBlock: (T) -> Long,
    doubleAccumulator: (Double, Double) -> Double = Double::plus,
    longAccumulator: (Long, Long) -> Long = Long::plus,
) {
    contract {
        callsInPlace(doubleBlock, InvocationKind.UNKNOWN)
        callsInPlace(longBlock, InvocationKind.UNKNOWN)
        callsInPlace(doubleAccumulator, InvocationKind.UNKNOWN)
        callsInPlace(longAccumulator, InvocationKind.UNKNOWN)
    }
    observePreferredAccumulated(values, doubleBlock, longBlock, doubleAccumulator, longAccumulator, source)
}

context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.Sourceless.observe(value: Int) = observe(value.toLong())

context(attributeStore: IAttributeValueStore)
fun IObservationRecorder.Unresolved.Sourceless.observePreferred(double: Double, int: Int) =
    observePreferred(double, int.toLong())

context(attributeStore: IAttributeValueStore)
inline fun IObservationRecorder.Unresolved.Sourceless.observePreferred(
    doubleBlock: () -> Double,
    longBlock: () -> Long,
) {
    contract {
        callsInPlace(doubleBlock, InvocationKind.AT_MOST_ONCE)
        callsInPlace(longBlock, InvocationKind.AT_MOST_ONCE)
    }
    if (supportsFloating)
        observe(doubleBlock())
    else
        observe(longBlock())
}

context(attributeStore: IAttributeValueStore.Mutable)
inline fun <T : Any, R> AttributeDataSource.Reference<T>.withValue(value: T, block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    set(value)
    return block()
}

context(attributeStore: IAttributeValueStore.Mutable)
inline fun <T : Any> AttributeDataSource.Reference<T>.withValue(value: T, block: () -> Unit) {
    // subcase of above with R=Unit, but prevents overload-resolution from preferring vararg with one value when R=Unit
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    withValue<T, Unit>(value, block)
}

context(attributeStore: IAttributeValueStore.Mutable)
inline fun <T : Any> AttributeDataSource.Reference<T>.withValues(values: Iterable<T>, block: (T) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    withValues<T, T>(values, transform = { it }, block = block)
}

context(attributeStore: IAttributeValueStore.Mutable)
inline fun <T : Any> AttributeDataSource.Reference<T>.withValues(vararg values: T, block: (T) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    withValues(*values, transform = { it }, block = block)
}


context(attributeStore: IAttributeValueStore.Mutable)
inline fun <B, T : Any> AttributeDataSource.Reference<T>.withValues(
    values: Iterable<B>,
    transform: (B) -> T,
    block: (T) -> Unit
) {
    contract {
        callsInPlace(transform, InvocationKind.UNKNOWN)
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    for (value in values) {
        val transformed = transform(value)
        set(transformed)
        block(transformed)
    }
}

context(attributeStore: IAttributeValueStore.Mutable)
inline fun <B, T : Any> AttributeDataSource.Reference<T>.withValues(
    vararg values: B,
    transform: (B) -> T,
    block: (T) -> Unit
) {
    contract {
        callsInPlace(transform, InvocationKind.UNKNOWN)
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    for (value in values) {
        val transformed = transform(value)
        set(transformed)
        block(transformed)
    }
}

context(attributeStore: IAttributeValueStore.Mutable)
inline fun <T : Any, R> AttributeDataSource.Reference<T>.withoutValue(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    unset()
    return block()
}
