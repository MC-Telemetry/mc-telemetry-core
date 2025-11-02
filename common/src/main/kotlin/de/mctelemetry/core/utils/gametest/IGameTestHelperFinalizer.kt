package de.mctelemetry.core.utils.gametest

import de.mctelemetry.core.utils.plus
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.GameTestListener
import net.minecraft.gametest.framework.GameTestRunner
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.IntFunction
import kotlin.jvm.optionals.getOrNull

interface IGameTestHelperFinalizer : MutableCollection<IGameTestHelperFinalizer.Callback> {

    fun interface Callback {

        fun onComplete(result: Throwable?)
    }


    @Deprecated("This declaration is redundant in Kotlin and might be removed soon")
    override fun <T : Any?> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?>? {
        return super.toArray(generator)
    }

    companion object {

        val GameTestInfo.finalizer: IGameTestHelperFinalizer
            get() {
                val existingFinalizer = listeners.filter { it is IGameTestHelperFinalizer }
                    .findAny()
                    .getOrNull() as IGameTestHelperFinalizer?
                if (existingFinalizer != null) {
                    return existingFinalizer
                }
                return Impl().also {
                    addListener(it)
                }
            }
        val GameTestHelper.finalizer: IGameTestHelperFinalizer
            get() = testInfo.finalizer
    }

    private class Impl private constructor(
        private val callbacks: ConcurrentLinkedQueue<Callback>,
    ) : IGameTestHelperFinalizer, GameTestListener, MutableCollection<Callback> by callbacks {

        constructor() : this(ConcurrentLinkedQueue())

        private fun callOnComplete(result: Throwable?) {
            var completionInvocationException: Exception? = null
            do {
                val element = callbacks.poll()
                if (element == null) break
                try {
                    element.onComplete(result)
                } catch (ex: Exception) {
                    completionInvocationException += ex
                }
            } while (true)
            if (completionInvocationException != null) throw completionInvocationException
        }

        override fun testStructureLoaded(gameTestInfo: GameTestInfo) {}

        override fun testPassed(
            gameTestInfo: GameTestInfo,
            gameTestRunner: GameTestRunner,
        ) {
            callOnComplete(null)
        }

        override fun testFailed(
            gameTestInfo: GameTestInfo,
            gameTestRunner: GameTestRunner,
        ) {
            val testError: Throwable? = gameTestInfo.error
            if (testError == null) {
                val error = NullPointerException("gameTestInfo.error is null in testFailed-Callback")
                try {
                    callOnComplete(error)
                } catch (ex: Exception) {
                    error.addSuppressed(ex)
                    throw error
                }
            } else {
                callOnComplete(testError)
            }
        }

        override fun testAddedForRerun(
            gameTestInfo: GameTestInfo,
            gameTestInfo2: GameTestInfo,
            gameTestRunner: GameTestRunner,
        ) {
        }
    }

}
