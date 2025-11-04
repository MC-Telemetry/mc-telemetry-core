@file:Suppress("unused")

package de.mctelemetry.core.gametest.utils

import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.gametest.framework.GameTestTimeoutException

@PublishedApi
internal inline fun wrapRunnableRuntimeExceptions(crossinline block: () -> Unit) {
    try {
        block()
    } catch (ex: RuntimeException) {
        if (ex is GameTestAssertException || ex is GameTestTimeoutException) throw ex
        throw GameTestAssertException(
            "Exception during then-block."
        ).initCause(ex)
    }
}

inline fun GameTestSequence.thenWaitUntilC(crossinline block: () -> Unit) =
    thenWaitUntil { wrapRunnableRuntimeExceptions(block) }!!

inline fun GameTestSequence.thenWaitUntilC(assertedDelayTicks: Long, crossinline block: () -> Unit) =
    thenWaitUntil(assertedDelayTicks) { wrapRunnableRuntimeExceptions(block) }!!

inline fun GameTestSequence.thenExecuteC(crossinline block: () -> Unit) =
    thenExecute { wrapRunnableRuntimeExceptions(block) }!!

inline fun GameTestSequence.thenExecuteAfterC(durationTicks: Int, crossinline block: () -> Unit) =
    thenExecuteAfter(durationTicks) { wrapRunnableRuntimeExceptions(block) }!!

inline fun GameTestSequence.thenExecuteForC(durationTicks: Int, crossinline block: () -> Unit) =
    thenExecuteFor(durationTicks) { wrapRunnableRuntimeExceptions(block) }!!
