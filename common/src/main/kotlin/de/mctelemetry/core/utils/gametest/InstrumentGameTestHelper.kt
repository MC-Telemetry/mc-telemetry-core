@file:OptIn(ExperimentalContracts::class)

package de.mctelemetry.core.utils.gametest

import com.mojang.datafixers.util.Either
import de.mctelemetry.core.api.metrics.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.metrics.DuplicateInstrumentException
import de.mctelemetry.core.api.metrics.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.ILongInstrumentRegistration
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.metrics.managar.gaugeWorldInstrument
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes
import de.mctelemetry.core.blocks.observation.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.gametest.IGameTestHelperFinalizer.Companion.finalizer
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestAssertPosException
import net.minecraft.gametest.framework.GameTestHelper
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KProperty
import kotlin.streams.asSequence

class InstrumentGameTestHelper(
    val gameTestHelper: GameTestHelper,
) {

    val testName: String = gameTestHelper.testInfo.testName

    val worldInstruments: IWorldInstrumentManager = gameTestHelper.server.instrumentManager!!

    inline fun createMutableLongGaugeWorldInstrument(
        baseName: String = "",
        suffixLimit: Int = 1024,
        registerFinalizer: IGameTestHelperFinalizer.FinalizerRegistration = IGameTestHelperFinalizer.FinalizerRegistration.TEST_END,
        block: IGaugeInstrumentBuilder<*>.() -> Unit,
    ): ILongInstrumentRegistration.Mutable<*> {
        contract {
            callsInPlace(block, InvocationKind.UNKNOWN)
        }
        var suffix = 1
        val instrumentBaseName = getTestInstrumentName(testName, baseName)
        val mutableRegistration = run {
            var lastException: DuplicateInstrumentException?
            do {
                try {
                    return@run worldInstruments.gaugeWorldInstrument(
                        name = "$instrumentBaseName.$suffix",
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
        if (registerFinalizer != IGameTestHelperFinalizer.FinalizerRegistration.NONE) {
            runWithExceptionCleanup(cleanup = mutableRegistration::close) {
                gameTestHelper.finalizer(registerFinalizer) {
                    mutableRegistration.close()
                }
            }
        }
        return mutableRegistration
    }

    inline fun <T> useMutableLongGaugeWorldInstrument(
        baseName: String = "",
        customization: IGaugeInstrumentBuilder<*>.() -> Unit = {},
        suffixLimit: Int = 1024,
        block: ILongInstrumentRegistration.Mutable<*>.() -> T,
    ): T {
        contract {
            callsInPlace(customization, InvocationKind.UNKNOWN)
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return createMutableLongGaugeWorldInstrument(
            baseName = baseName,
            suffixLimit = suffixLimit,
            registerFinalizer = IGameTestHelperFinalizer.FinalizerRegistration.NONE,
            block = customization
        ).use(block)
    }

    inline fun createMutableDoubleGaugeWorldInstrument(
        baseName: String = "",
        suffixLimit: Int = 1024,
        registerFinalizer: IGameTestHelperFinalizer.FinalizerRegistration = IGameTestHelperFinalizer.FinalizerRegistration.TEST_END,
        block: IGaugeInstrumentBuilder<*>.() -> Unit,
    ): IDoubleInstrumentRegistration.Mutable<*> {
        contract {
            callsInPlace(block, InvocationKind.UNKNOWN)
        }
        var suffix = 1
        val instrumentBaseName = getTestInstrumentName(testName, baseName)
        val mutableRegistration = run {
            var lastException: DuplicateInstrumentException?
            do {
                try {
                    return@run worldInstruments.gaugeWorldInstrument(
                        name = "$instrumentBaseName.$suffix",
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
        if (registerFinalizer != IGameTestHelperFinalizer.FinalizerRegistration.NONE) {
            runWithExceptionCleanup(cleanup = mutableRegistration::close) {
                gameTestHelper.finalizer(registerFinalizer) {
                    mutableRegistration.close()
                }
            }
        }
        return mutableRegistration
    }

    inline fun <T> useMutableDoubleGaugeWorldInstrument(
        baseName: String = "",
        customization: IGaugeInstrumentBuilder<*>.() -> Unit = {},
        suffixLimit: Int = 1024,
        block: IDoubleInstrumentRegistration.Mutable<*>.() -> T,
    ): T {
        contract {
            callsInPlace(customization, InvocationKind.UNKNOWN)
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return createMutableDoubleGaugeWorldInstrument(
            baseName = baseName,
            suffixLimit = suffixLimit,
            registerFinalizer = IGameTestHelperFinalizer.FinalizerRegistration.NONE,
            block = customization
        ).use(block)
    }

    fun configureObservationContainers(
        useDouble: (originalName: String) -> Boolean = { false },
        suffixLimit: Int = 1024,
        overrideExisting: Boolean = true,
        registerFinalizer: IGameTestHelperFinalizer.FinalizerRegistration = IGameTestHelperFinalizer.FinalizerRegistration.TEST_END,
        customizer: IGaugeInstrumentBuilder<*>.(originalName: String, isDouble: Boolean) -> Unit = { _, _ -> },
    ): Map<String, Either<ILongInstrumentRegistration.Mutable<*>, IDoubleInstrumentRegistration.Mutable<*>>> {
        val observationSourceEntityType = OTelCoreModBlockEntityTypes.OBSERVATION_SOURCE_CONTAINER_BLOCK_ENTITY.get()
        val observationSourceMap: Map<String, List<Pair<ObservationSourceContainerBlockEntity, ObservationSourceState>>> =
            BlockPos.MutableBlockPos.betweenClosedStream(gameTestHelper.bounds.contract(1.0, 1.0, 1.0))
                .asSequence()
                .mapNotNull { blockPos ->
                    gameTestHelper.level.getBlockEntity(blockPos, observationSourceEntityType).getOrNull()
                }
                .flatMap { blockEntity ->
                    blockEntity.observationStates!!.values.mapNotNull { state ->
                        if (state.configuration == null) return@mapNotNull null
                        if ((!overrideExisting) && (state.instrument != null)) return@mapNotNull null
                        blockEntity to state
                    }
                }.groupBy { (_, state) -> state.configuration!!.instrument.name }
        for ((baseName, statePairs) in observationSourceMap) {
            val (baseEntity, firstState) = statePairs.first() // all statePairs have at least 1 element because of groupBy
            val baseSourceAttributes = firstState.configuration!!.mapping.instrumentAttributes
            for ((testEntity, testState) in statePairs.drop(1)) {
                val testSourceAttributes = testState.configuration!!.mapping.instrumentAttributes
                require(testSourceAttributes == baseSourceAttributes) {
                    """Incompatible instrumentAttributes for $baseName between
                            | ${firstState.source.id.location()}@$baseEntity@${gameTestHelper.relativePos(baseEntity.blockPos)}
                            | and
                            | ${testState.source.id.location()}@$testEntity@${gameTestHelper.relativePos(testEntity.blockPos)}:
                            | $baseSourceAttributes != $testSourceAttributes""".trimMargin()
                }
            }
        }
        // two passes through observationSourceMap to first check all, then instantiate all
        val completedInstruments = ArrayDeque<AutoCloseable>(observationSourceMap.size)
        return runWithExceptionCleanup(cleanup = {
            var exceptionAccumulator: Exception? = null
            do {
                val instrument = completedInstruments.removeFirstOrNull() ?: break
                try {
                    instrument.close()
                } catch (ex: Exception) {
                    exceptionAccumulator += ex
                }
            } while (true)
            if (exceptionAccumulator != null) throw exceptionAccumulator
        }) {
            observationSourceMap.mapValues { (baseName, statePairs) ->
                val shouldUseDouble = useDouble(baseName)
                val (_, baseState) = statePairs.first()
                val instrumentAttributes: Set<MappedAttributeKeyInfo<*, *>> =
                    baseState.configuration!!.mapping.instrumentAttributes
                val result: Either<
                        ILongInstrumentRegistration.Mutable<*>,
                        IDoubleInstrumentRegistration.Mutable<*>,
                        >
                if (shouldUseDouble) {
                    result = Either.right(
                        createMutableDoubleGaugeWorldInstrument(
                            baseName,
                            suffixLimit = suffixLimit,
                            registerFinalizer = registerFinalizer,
                        ) {
                            attributes = instrumentAttributes.toList()
                            customizer(baseName, true)
                        })
                } else {
                    result = Either.left(
                        createMutableLongGaugeWorldInstrument(
                            baseName,
                            suffixLimit = suffixLimit,
                            registerFinalizer = registerFinalizer,
                        ) {
                            attributes = instrumentAttributes.toList()
                            customizer(baseName, false)
                        })
                }
                val instrument: IInstrumentRegistration.Mutable<*> = Either.unwrap(result)
                completedInstruments.add(instrument)
                for ((_, state) in statePairs) {
                    state.configuration = state.configuration!!.copy(
                        instrument = instrument
                    )
                }
                result
            }
        }
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


    fun IDoubleInstrumentRegistration.assertRecordsSingle(
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

    fun IInstrumentRegistration.assertRecordsNone(supportsFloating: Boolean) {
        AssertionObservationRecorder.None(gameTestHelper, this.name, supportsFloating).also(this::observe)
    }

    fun formatGlobalBlockPos(blockPos: BlockPos): List<String> = BuiltinAttributeKeyTypes.GlobalPosType.format(
        gameTestHelper.level.dimension(),
        gameTestHelper.absolutePos(blockPos)
    )


    fun formatBlockPos(blockPos: BlockPos): List<Long> = BuiltinAttributeKeyTypes.BlockPosType.format(
        gameTestHelper.absolutePos(blockPos)
    )

    fun formatDirection(direction: Direction): String = BuiltinAttributeKeyTypes.DirectionType.format(
        gameTestHelper.testRotation.rotate(direction)
    )

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


        fun getTestInstrumentName(testName: String, baseName: String = ""): String {
            val partiallySanitizedName = makeSnakeCase(testName)
            val testNamespaceName =
                if (Validators.validateOTelName(partiallySanitizedName, stopAtInvalid = false) == null)
                    partiallySanitizedName
                else
                    "test_${testName.hashCode().toHexString()}".also {
                        Validators.parseOTelName(it, stopAtInvalid = false)
                    }
            if (baseName.isBlank()) {
                return testNamespaceName
            } else {
                Validators.parseOTelName(baseName, stopAtInvalid = false)
                return ("$testNamespaceName.$baseName").also {
                    Validators.parseOTelName(it, stopAtInvalid = false)
                }
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
