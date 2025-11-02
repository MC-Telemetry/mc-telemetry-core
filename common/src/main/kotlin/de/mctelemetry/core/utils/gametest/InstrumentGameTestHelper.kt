@file:OptIn(ExperimentalContracts::class)

package de.mctelemetry.core.utils.gametest

import de.mctelemetry.core.api.metrics.DuplicateInstrumentException
import de.mctelemetry.core.api.metrics.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.metrics.ILongInstrumentRegistration
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.metrics.managar.gaugeWorldInstrument
import de.mctelemetry.core.blocks.observation.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.gametest.IGameTestHelperFinalizer.Companion.finalizer
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestAssertPosException
import net.minecraft.gametest.framework.GameTestHelper
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty

class InstrumentGameTestHelper(
    val gameTestHelper: GameTestHelper,
) {

    val testName: String = gameTestHelper.testInfo.testName

    val worldInstruments: IWorldInstrumentManager = gameTestHelper.server.instrumentManager!!

    inline fun createMutableLongGaugeWorldInstrument(
        suffixLimit: Int = 1024,
        registerFinalizer: Boolean = true,
        block: IGaugeInstrumentBuilder<*>.() -> Unit,
    ): ILongInstrumentRegistration.Mutable<ILongInstrumentRegistration.Mutable<*>> {
        contract {
            callsInPlace(block, InvocationKind.UNKNOWN)
        }
        var suffix = 1
        val baseName = getTestInstrumentName(testName)
        val mutableRegistration = run {
            var lastException: DuplicateInstrumentException?
            do {
                try {
                    return@run worldInstruments.gaugeWorldInstrument(
                        name = "$baseName.$suffix",
                        block = block
                    ).apply {
                        withPersistent(false)
                    }.registerMutableOfLong()
                } catch (ex: DuplicateInstrumentException) {
                    suffix++
                    lastException = ex
                }
            } while (suffix < suffixLimit)
            throw lastException
        }
        if (registerFinalizer) {
            runWithExceptionCleanup(cleanup = mutableRegistration::close) {
                gameTestHelper.finalizer.add {
                    mutableRegistration.close()
                }
            }
        }
        return mutableRegistration
    }

    inline fun <T> useMutableLongGaugeWorldInstrument(
        customization: IGaugeInstrumentBuilder<*>.() -> Unit = {},
        suffixLimit: Int = 1024,
        block: ILongInstrumentRegistration.Mutable<ILongInstrumentRegistration.Mutable<*>>.() -> T,
    ): T {
        contract {
            callsInPlace(customization, InvocationKind.UNKNOWN)
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return createMutableLongGaugeWorldInstrument(
            suffixLimit = suffixLimit,
            registerFinalizer = false,
            block = customization
        ).use(block)
    }

    inline fun createMutableDoubleGaugeWorldInstrument(
        suffixLimit: Int = 1024,
        registerFinalizer: Boolean = true,
        block: IGaugeInstrumentBuilder<*>.() -> Unit,
    ): IDoubleInstrumentRegistration.Mutable<IDoubleInstrumentRegistration.Mutable<*>> {
        contract {
            callsInPlace(block, InvocationKind.UNKNOWN)
        }
        var suffix = 1
        val baseName = getTestInstrumentName(testName)
        val mutableRegistration = run {
            var lastException: DuplicateInstrumentException?
            do {
                try {
                    return@run worldInstruments.gaugeWorldInstrument(
                        name = "$baseName.$suffix",
                        block = block
                    ).apply {
                        withPersistent(false)
                    }.registerMutableOfDouble()
                } catch (ex: DuplicateInstrumentException) {
                    suffix++
                    lastException = ex
                }
            } while (suffix < suffixLimit)
            throw lastException
        }
        if (registerFinalizer) {
            runWithExceptionCleanup(cleanup = mutableRegistration::close) {
                gameTestHelper.finalizer.add {
                    mutableRegistration.close()
                }
            }
        }
        return mutableRegistration
    }

    inline fun <T> useMutableDoubleGaugeWorldInstrument(
        customization: IGaugeInstrumentBuilder<*>.() -> Unit = {},
        suffixLimit: Int = 1024,
        block: IDoubleInstrumentRegistration.Mutable<IDoubleInstrumentRegistration.Mutable<*>>.() -> T,
    ): T {
        contract {
            callsInPlace(customization, InvocationKind.UNKNOWN)
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return createMutableDoubleGaugeWorldInstrument(
            suffixLimit = suffixLimit,
            registerFinalizer = false,
            block = customization
        ).use(block)
    }

    fun ILongInstrumentRegistration.assertRecordsSingle(
        attributes: Attributes,
        value: Long,
        allowPreferred: Boolean = true,
    ) {
        AssertionObservationRecorder.Single.assertRecordsLong(
            gameTestHelper = gameTestHelper,
            name = this.name,
            longValue = value,
            attributes = attributes,
            allowPreferred = allowPreferred,
            source = null,
            requireSourceMatch = false
        ).also(this::observe).assertSawValue()
    }


    fun ILongInstrumentRegistration.assertRecordsSingle(
        attributes: Attributes,
        value: Double,
        allowPreferred: Boolean = true,
    ) {
        AssertionObservationRecorder.Single.assertRecordsDouble(
            gameTestHelper = gameTestHelper,
            name = this.name,
            doubleValue = value,
            attributes = attributes,
            allowPreferred = allowPreferred,
            source = null,
            requireSourceMatch = false
        ).also(this::observe).assertSawValue()
    }

    companion object {

        private val upperCaseRegex = Regex("""[A-Z]+""")

        private fun makeSnakeCase(input: String): String {
            return input.replace('/', '.').replace(upperCaseRegex) { match ->
                val length = match.value.length
                val matchReplaceString = if (length == 1) {
                    match.value.lowercase()
                } else {
                    match.value.substring(0, length - 1).lowercase() + '_' + match.value[length - 1].lowercase()
                }
                if (match.range.start == 0 || input[match.range.start - 1].let {
                        it == '_' || it == '.'
                    })
                    matchReplaceString
                else
                    "_$matchReplaceString"
            }
        }


        fun getTestInstrumentName(testName: String): String {
            val partiallySanitizedName = makeSnakeCase(testName)
            return if (Validators.validateOTelName(partiallySanitizedName, stopAtInvalid = false) != null)
                partiallySanitizedName
            else
                "test_${testName.hashCode().toHexString()}".also {
                    Validators.parseOTelName(it, stopAtInvalid = false)
                }
        }

        internal inline fun <T> GameTestHelper.configureObservationSource(
            pos: BlockPos,
            observationSource: IObservationSource<*, *>,
            block: (ObservationSourceState) -> T,
        ): T {
            val containerEntity: ObservationSourceContainerBlockEntity = this.getBlockEntityC(pos)
            val states = containerEntity.observationStates
            try {
                assertNotNullC(states, "Expected container to be configured on $containerEntity")
                val state = states[observationSource]
                assertNotNullC(state, "Expected state to be available for $observationSource on $containerEntity")
                return block(state)
            } catch (ex: GameTestAssertException) {
                if (ex is GameTestAssertPosException) throw ex
                throw GameTestAssertPosException(
                    ex.message!!,
                    absolutePos(pos),
                    pos,
                    tick,
                )
            }
        }

        private class ArgLazy<T, R : Any>(val block: (T) -> R) {

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

        val GameTestHelper.instruments: InstrumentGameTestHelper by ArgLazy(::InstrumentGameTestHelper)
    }
}
