package de.mctelemetry.core.utils

import org.jetbrains.annotations.Contract
import java.util.Queue
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Runs the [block] function, followed by [cleanup] IFF [block] threw an exception.
 *
 * The exception from [block] is temporarily caught, then [cleanup] is run.
 * If [cleanup] also throws an exception, it is added as suppressed to the exception from [block].
 * After [cleanup], the exception from [block] is rethrown.
 *
 * If [block] completes successfully, its value is returned and [cleanup] is not run.
 */
inline fun <T> runWithExceptionCleanup(cleanup: () -> Unit, block: () -> T): T {
    contract {
        callsInPlace(cleanup, InvocationKind.AT_MOST_ONCE)
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    try {
        return block()
    } catch (e: Exception) {
        try {
            cleanup()
        } catch (e2: Exception) {
            e.addSuppressed(e2)
        }
        throw e
    }
}

/**
 * Runs the [block] function, followed by [cleanup] IFF [block] threw an exception AND [runCleanup] is `true`.
 *
 * The exception from [block] is temporarily caught, then [cleanup] is run, if specified.
 * If [cleanup] also throws an exception, it is added as suppressed to the exception from [block].
 * After [cleanup], the exception from [block] is rethrown.
 *
 * If [block] completes successfully, its value is returned and [cleanup] is not run, even if [runCleanup] is `true`.
 */
inline fun <T> runWithExceptionCleanup(cleanup: () -> Unit, runCleanup: Boolean, block: () -> T): T {
    contract {
        callsInPlace(cleanup, InvocationKind.AT_MOST_ONCE)
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (!runCleanup)
        return block()
    try {
        return block()
    } catch (e: Exception) {
        try {
            cleanup()
        } catch (e2: Exception) {
            e.addSuppressed(e2)
        }
        throw e
    }
}

@JvmName("exceptionAccumulator")
operator fun <T : Exception> T.plus(other: Exception): T {
    this.addSuppressed(other)
    return this
}

/**
 * If the first exception is not null, adds the second exception to the first's suppressed list and returns the first.
 * If the first exception is null, return the second exception unmodified.
 *
 * Allows constructs like this:
 * ```kotlin
 * var exAccumulator: Exception?
 * try {
 *     // [...] first possibly failing block
 * } catch(ex: Exception) {
 *     exAccumulator += ex
 * }
 * try {
 *     // [...] second possibly failing block
 * } catch(ex: Exception) {
 *     exAccumulator += ex
 * }
 * // [...]
 * if (exAccumulator != null)
 *     // throw accumulated exceptions
 *     throw exAccumulator
 * ```
 */
@JvmName("exceptionAccumulatorNullable1")
operator fun <T : Exception> T?.plus(other: T): T {
    if (this == null) return other
    this.addSuppressed(other)
    return this
}

@JvmName("exceptionAccumulatorNullable2")
operator fun <T : Exception> T.plus(other: T?): T {
    if (other == null) return this
    this.addSuppressed(other)
    return this
}

@JvmName("exceptionAccumulatorNullable")
@Contract("null, null -> null; !null, _ -> !null; _, !null -> !null")
operator fun <T : Exception> T?.plus(other: T?): T? {
    if (this == null) return other
    if (other != null) {
        this.addSuppressed(other)
    }
    return this
}

@Contract("_, !null, _, _ -> !null")
@JvmName("consumeAllCollectNullable")
inline fun <T> MutableList<T>.consumeAllCollect(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): Exception? {
    contract {
        returns(null) implies (exceptionSeed == null)
        callsInPlace(onException)
        callsInPlace(block)
    }
    var exceptionAccumulator: Exception? = exceptionSeed
    while (this.isNotEmpty()) {
        val element = try {
            removeFirst()
        } catch (_: NoSuchElementException) {
            break
        }
        try {
            block(element)
        } catch (ex: Exception) {
            onException(element, ex)
            exceptionAccumulator += ex
        }
    }
    return exceptionAccumulator
}

@JvmName("consumeAllCollect")
inline fun <T, TE : Exception> MutableList<T>.consumeAllCollect(
    exceptionSeed: TE,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): TE {
    contract {
        callsInPlace(onException)
        callsInPlace(block)
    }
    @Suppress("UNCHECKED_CAST")
    return consumeAllCollect(exceptionSeed as Exception?, onException, block) as TE
}

@Contract("_, !null, _ -> !null")
@JvmName("closeConsumeAllCollectNullable")
inline fun <T : AutoCloseable> MutableList<T>.closeConsumeAllCollect(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): Exception? {
    contract {
        returns(null) implies (exceptionSeed == null)
        callsInPlace(onException)
    }
    return consumeAllCollect(exceptionSeed, onException, AutoCloseable::close)
}

@JvmName("closeConsumeAllCollect")
inline fun <T : AutoCloseable, TE : Exception> MutableList<T>.closeConsumeAllCollect(
    exceptionSeed: TE,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): TE {
    contract {
        callsInPlace(onException)
    }
    return consumeAllCollect(exceptionSeed, onException, AutoCloseable::close)
}


@Contract("_, !null, _, _ -> !null")
@JvmName("consumeAllCollectNullable")
inline fun <T> Queue<T>.consumeAllCollect(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): Exception? {
    contract {
        returns(null) implies (exceptionSeed == null)
        callsInPlace(onException)
        callsInPlace(block)
    }
    var exceptionAccumulator: Exception? = exceptionSeed
    while (true) {
        val element = try {
            remove()
        } catch (_: NoSuchElementException) {
            break
        }
        try {
            block(element)
        } catch (ex: Exception) {
            onException(element, ex)
            exceptionAccumulator += ex
        }
    }
    return exceptionAccumulator
}

@JvmName("consumeAllCollect")
inline fun <T, TE : Exception> Queue<T>.consumeAllCollect(
    exceptionSeed: TE,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): TE {
    contract {
        callsInPlace(onException)
        callsInPlace(block)
    }
    @Suppress("UNCHECKED_CAST")
    return consumeAllCollect(exceptionSeed as Exception?, onException, block) as TE
}

@Contract("_, !null, _ -> !null")
@JvmName("closeConsumeAllCollectNullable")
inline fun <T : AutoCloseable> Queue<T>.closeConsumeAllCollect(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): Exception? {
    contract {
        returns(null) implies (exceptionSeed == null)
        callsInPlace(onException)
    }
    return consumeAllCollect(exceptionSeed, onException, AutoCloseable::close)
}

@JvmName("closeConsumeAllCollect")
inline fun <T : AutoCloseable, TE : Exception> Queue<T>.closeConsumeAllCollect(
    exceptionSeed: TE,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): TE {
    contract {
        callsInPlace(onException)
    }
    return consumeAllCollect(exceptionSeed, onException, AutoCloseable::close)
}

@Contract("_, !null, _, _ -> !null")
@JvmName("forEachCollectNullable")
inline fun <T> Iterable<T>.forEachCollect(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): Exception? {
    contract {
        returns(null) implies (exceptionSeed == null)
        callsInPlace(onException)
        callsInPlace(block)
    }
    var exceptionAccumulator: Exception? = exceptionSeed
    forEach { element ->
        try {
            block(element)
        } catch (e: Exception) {
            onException(element, e)
            exceptionAccumulator += e
        }
    }
    return exceptionAccumulator
}

@JvmName("forEachCollect")
inline fun <T, TE : Exception> Iterable<T>.forEachCollect(
    exceptionSeed: TE,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): TE {
    contract {
        callsInPlace(onException)
        callsInPlace(block)
    }
    @Suppress("UNCHECKED_CAST")
    return forEachCollect(exceptionSeed as Exception?, onException, block) as TE
}

@Contract("_, !null, _ -> !null")
@JvmName("closeAllCollectNullable")
inline fun <T : AutoCloseable> Iterable<T>.closeAllCollect(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): Exception? {
    contract {
        returns(null) implies (exceptionSeed == null)
        callsInPlace(onException)
    }
    return forEachCollect(exceptionSeed, onException, AutoCloseable::close)
}

@Contract("_, !null, _ -> !null")
@JvmName("closeAllCollect")
inline fun <T : AutoCloseable, TE : Exception> Iterable<T>.closeAllCollect(
    exceptionSeed: TE,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): TE {
    contract {
        callsInPlace(onException)
    }
    return forEachCollect(exceptionSeed, onException, AutoCloseable::close)
}


@Contract("_, !null, _, _ -> fail")
@JvmName("consumeAllRethrowNullable")
inline fun <T> MutableList<T>.consumeAllRethrow(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
) {
    contract {
        returns() implies (exceptionSeed == null)
        callsInPlace(onException)
        callsInPlace(block)
    }
    throw consumeAllCollect(exceptionSeed, onException, block) ?: return
}

@JvmName("consumeAllRethrow")
inline fun <T> MutableList<T>.consumeAllRethrow(
    exceptionSeed: Exception,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): Nothing {
    contract {
        callsInPlace(onException)
        callsInPlace(block)
    }
    throw consumeAllCollect(exceptionSeed, onException, block)
}

@Contract("_, !null, _ -> fail")
@JvmName("closeConsumeAllRethrowNullable")
inline fun <T : AutoCloseable> MutableList<T>.closeConsumeAllRethrow(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
) {
    contract {
        returns() implies (exceptionSeed == null)
        callsInPlace(onException)
    }
    consumeAllRethrow(exceptionSeed, onException, AutoCloseable::close)
}

@JvmName("closeConsumeAllRethrow")
inline fun <T : AutoCloseable> MutableList<T>.closeConsumeAllRethrow(
    exceptionSeed: Exception,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): Nothing {
    contract {
        callsInPlace(onException)
    }
    consumeAllRethrow(exceptionSeed, onException, AutoCloseable::close)
}


@Contract("_, !null, _, _ -> fail")
@JvmName("consumeAllRethrowNullable")
inline fun <T> Queue<T>.consumeAllRethrow(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
) {
    contract {
        returns() implies (exceptionSeed == null)
        callsInPlace(onException)
        callsInPlace(block)
    }
    throw consumeAllCollect(exceptionSeed, onException, block) ?: return
}

@JvmName("consumeAllRethrow")
inline fun <T> Queue<T>.consumeAllRethrow(
    exceptionSeed: Exception,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): Nothing {
    contract {
        callsInPlace(onException)
        callsInPlace(block)
    }
    throw consumeAllCollect(exceptionSeed, onException, block)
}

@Contract("_, !null, _ -> fail")
@JvmName("closeConsumeAllRethrowNullable")
inline fun <T : AutoCloseable> Queue<T>.closeConsumeAllRethrow(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
) {
    contract {
        returns() implies (exceptionSeed == null)
        callsInPlace(onException)
    }
    consumeAllRethrow(exceptionSeed, onException, AutoCloseable::close)
}

@JvmName("closeConsumeAllRethrow")
inline fun <T : AutoCloseable> Queue<T>.closeConsumeAllRethrow(
    exceptionSeed: Exception,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): Nothing {
    contract {
        callsInPlace(onException)
    }
    consumeAllRethrow(exceptionSeed, onException, AutoCloseable::close)
}


@Contract("_, !null, _, _ -> fail")
@JvmName("forEachRethrowNullable")
inline fun <T> Iterable<T>.forEachRethrow(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
) {
    contract {
        returns() implies (exceptionSeed == null)
        callsInPlace(onException)
        callsInPlace(block)
    }
    throw forEachCollect(exceptionSeed, onException, block) ?: return
}

@JvmName("forEachRethrow")
inline fun <T> Iterable<T>.forEachRethrow(
    exceptionSeed: Exception,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
    block: (T) -> Unit,
): Nothing {
    contract {
        callsInPlace(onException)
        callsInPlace(block)
    }
    throw forEachCollect(exceptionSeed, onException, block)
}

@Contract("_, !null, _ -> fail")
@JvmName("closeAllRethrowNullable")
inline fun <T : AutoCloseable> Iterable<T>.closeAllRethrow(
    exceptionSeed: Exception? = null,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
) {
    contract {
        returns() implies (exceptionSeed == null)
        callsInPlace(onException)
    }
    forEachRethrow(exceptionSeed, onException, AutoCloseable::close)
}

@JvmName("closeAllRethrow")
inline fun <T : AutoCloseable> Iterable<T>.closeAllRethrow(
    exceptionSeed: Exception,
    onException: (element: T, exception: Exception) -> Unit = { _, _ -> },
): Nothing {
    contract {
        callsInPlace(onException)
    }
    forEachRethrow(exceptionSeed, onException, AutoCloseable::close)
}
