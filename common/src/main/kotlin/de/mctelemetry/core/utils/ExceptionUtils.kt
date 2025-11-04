package de.mctelemetry.core.utils

import kotlin.contracts.ExperimentalContracts
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
@OptIn(ExperimentalContracts::class)
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
@OptIn(ExperimentalContracts::class)
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
operator fun Exception?.plus(other: Exception): Exception {
    if (this == null) return other
    this.addSuppressed(other)
    return this
}
