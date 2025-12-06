package de.mctelemetry.core.gametest.utils.observation

import com.mojang.datafixers.util.Either
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.instruments.DuplicateInstrumentException
import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.gametest.utils.IGameTestHelperFinalizer
import de.mctelemetry.core.gametest.utils.IGameTestHelperFinalizer.Companion.finalizer
import de.mctelemetry.core.gametest.utils.assertNotNullC
import de.mctelemetry.core.gametest.utils.failC
import de.mctelemetry.core.gametest.utils.getBlockEntityC
import de.mctelemetry.core.gametest.utils.observation.InstrumentGameTestHelper.Companion.instruments
import de.mctelemetry.core.gametest.utils.server
import de.mctelemetry.core.gametest.utils.thenExecuteAfterC
import de.mctelemetry.core.observations.model.ObservationSourceConfiguration
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import de.mctelemetry.core.observations.model.ObservationSourceErrorState.Companion.asException
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.ArgLazy
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestAssertPosException
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestSequence
import net.minecraft.world.level.block.entity.BlockEntity
import kotlin.collections.get
import kotlin.collections.iterator
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.streams.asSequence

class InstrumentGameTestHelper(
    val gameTestHelper: GameTestHelper,
) {

    val testName: String = gameTestHelper.testInfo.testName

    val worldInstruments: IServerWorldInstrumentManager = gameTestHelper.server.instrumentManager!!

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
        val observationSourceMap: Map<String, List<Pair<ObservationSourceContainerBlockEntity, ObservationSourceState>>> =
            BlockPos.betweenClosedStream(gameTestHelper.bounds.contract(1.0, 1.0, 1.0))
                .asSequence()
                .mapNotNull { blockPos ->
                    gameTestHelper.level.getBlockEntity(blockPos) as? ObservationSourceContainerBlockEntity
                }
                .flatMap { blockEntity ->
                    blockEntity.observationStates.values.mapNotNull { state ->
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
        val originalConfigurations = ArrayDeque<Pair<ObservationSourceState, ObservationSourceConfiguration?>>()
        gameTestHelper.finalizer(registerFinalizer) {
            var exceptionAccumulator: Exception? = null
            do {
                val (state, config) = originalConfigurations.removeFirstOrNull() ?: break
                try {
                    state.configuration = config
                } catch (ex: Exception) {
                    exceptionAccumulator += ex
                }
            } while (true)
            if (exceptionAccumulator != null) throw exceptionAccumulator
        }
        return runWithExceptionCleanup(cleanup = {
            var exceptionAccumulator: Exception? = null
            do {
                val (state, config) = originalConfigurations.removeFirstOrNull() ?: break
                try {
                    state.configuration = config
                } catch (ex: Exception) {
                    exceptionAccumulator += ex
                }
            } while (true)
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
                    val oldConfig = state.configuration!!
                    state.configuration = oldConfig.copy(
                        instrument = instrument
                    )
                    originalConfigurations.add(state to oldConfig)
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
        ).also(this::observe).assertSawAll()
    }


    fun IDoubleInstrumentRegistration.assertRecordsSingle(
        attributes: Attributes,
        value: Double,
        allowPreferred: Boolean = true,
        doubleMargin: Double = AssertionObservationRecorder.DEFAULT_DOUBLE_MARGIN,
    ) {
        AssertionObservationRecorder.Single.assertRecordsDouble(
            gameTestHelper = gameTestHelper,
            name = this.name,
            doubleValue = value,
            attributes = attributes,
            allowPreferred = allowPreferred,
            source = null,
            requireSourceMatch = false,
            doubleMargin = doubleMargin,
        ).also(this::observe).assertSawAll()
    }

    fun IInstrumentRegistration.assertRecordsNone(supportsFloating: Boolean) {
        AssertionObservationRecorder.None(gameTestHelper, this.name, supportsFloating).also(this::observe)
    }

    fun ILongInstrumentRegistration.assertRecordsNone() {
        assertRecordsNone(supportsFloating = false)
    }

    fun IDoubleInstrumentRegistration.assertRecordsNone() {
        assertRecordsNone(supportsFloating = true)
    }

    fun IInstrumentRegistration.assertRecords(
        supportsFloating: Boolean,
        requireAll: Boolean = true,
        doubleMargin: Double = AssertionObservationRecorder.DEFAULT_DOUBLE_MARGIN,
        block: IAssertionObservationRecorderBuilder.() -> Unit,
    ) {
        val asserter =
            AssertionObservationRecorder.buildAssertionRecorder(
                gameTestHelper,
                this.name,
                supportsFloating,
                doubleMargin = doubleMargin,
                block
            )
        observe(asserter)
        if (requireAll) {
            asserter.assertSawAll()
        }
    }

    fun ILongInstrumentRegistration.assertRecords(
        requireAll: Boolean = true,
        block: IAssertionObservationRecorderBuilder.ForLong.() -> Unit,
    ) {
        val asserter = AssertionObservationRecorder.buildAssertionRecorderLong(gameTestHelper, this.name, block)
        observe(asserter)
        if (requireAll) {
            asserter.assertSawAll()
        }
    }


    fun IDoubleInstrumentRegistration.assertRecords(
        requireAll: Boolean = true,
        doubleMargin: Double = AssertionObservationRecorder.DEFAULT_DOUBLE_MARGIN,
        block: IAssertionObservationRecorderBuilder.ForDouble.() -> Unit,
    ) {
        val asserter = AssertionObservationRecorder.buildAssertionRecorderDouble(
            gameTestHelper,
            this.name,
            doubleMargin = doubleMargin,
            block
        )
        observe(asserter)
        if (requireAll) {
            asserter.assertSawAll()
        }
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
            val states = containerEntity.observationStatesIfInitialized
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

        val GameTestHelper.instruments: InstrumentGameTestHelper by ArgLazy(::InstrumentGameTestHelper)
    }
}

internal fun GameTestHelper.withConfiguredStartupSequence(
    useDouble: (originalName: String) -> Boolean = { false },
    suffixLimit: Int = 1024,
    overrideExisting: Boolean = true,
    customizer: IGaugeInstrumentBuilder<*>.(originalName: String, isDouble: Boolean) -> Unit = { _, _ -> },
    sequenceCustomizer: (
        GameTestSequence,
        Map<String, Either<ILongInstrumentRegistration.Mutable<*>, IDoubleInstrumentRegistration.Mutable<*>>>,
    ) -> Unit = { _, _ -> },
) {
    return withConfiguredStartupSequence(
        useDouble,
        suffixLimit,
        overrideExisting,
        customizer,
        sequenceCustomizer = { sequence, instruments, _ ->
            sequenceCustomizer(sequence, instruments)
        }) {}
}

internal fun <T : Any> GameTestHelper.withConfiguredStartupSequence(
    useDouble: (originalName: String) -> Boolean = { false },
    suffixLimit: Int = 1024,
    overrideExisting: Boolean = true,
    customizer: IGaugeInstrumentBuilder<*>.(originalName: String, isDouble: Boolean) -> Unit = { _, _ -> },
    sequenceCustomizer: (
        GameTestSequence,
        Map<String, Either<ILongInstrumentRegistration.Mutable<*>, IDoubleInstrumentRegistration.Mutable<*>>>,
        () -> T,
    ) -> Unit = { _, _, _ -> },
    block: (Map<String, Either<ILongInstrumentRegistration.Mutable<*>, IDoubleInstrumentRegistration.Mutable<*>>>) -> T,
) {
    val result: MutableMap<String,
            Either<ILongInstrumentRegistration.Mutable<*>,
                    IDoubleInstrumentRegistration.Mutable<*>>> = mutableMapOf()
    var blockResult: T? = null
    val sequence: GameTestSequence = startSequence()
        .thenExecuteAfterC(2) {
            forEveryBlockInStructure { blockPos ->
                val state = getBlockState(blockPos)
                if ((!state.hasBlockEntity()) ||
                    getBlockEntity<BlockEntity>(blockPos) !is ObservationSourceContainerBlockEntity
                ) return@forEveryBlockInStructure
                val errorStateValue = state.getValue(ObservationSourceContainerBlock.ERROR)
                if (errorStateValue != ObservationSourceErrorState.Type.Errors) {
                    failC(
                        "Unexpected error state: Expected ${ObservationSourceErrorState.Type.Errors}, got $errorStateValue",
                        blockPos
                    )
                }
            }
            result.putAll(
                instruments.configureObservationContainers(
                    useDouble = useDouble,
                    suffixLimit = suffixLimit,
                    overrideExisting = overrideExisting,
                    customizer = customizer,
                )
            )
        }.thenExecuteAfterC(2) {
            forEveryBlockInStructure { blockPos ->
                val state = getBlockState(blockPos)
                if (!state.hasBlockEntity()) return@forEveryBlockInStructure
                val entity = getBlockEntity<BlockEntity>(blockPos)
                if (entity !is ObservationSourceContainerBlockEntity) return@forEveryBlockInStructure
                val errorStateValue = state.getValue(ObservationSourceContainerBlock.ERROR)
                if (errorStateValue != ObservationSourceErrorState.Type.Ok) {
                    val problems = entity.observationStatesIfInitialized.orEmpty()
                        .mapValues { (_, value) ->
                            value.errorState.withoutWarning(ObservationSourceErrorState.notConfiguredWarning)
                        }
                        .filterValues { it != ObservationSourceErrorState.Ok }
                    if (problems.isEmpty()) {
                        failC("Unexpected error state without stored errors/warnings: $errorStateValue", blockPos)
                    }
                    val exception = (
                            // first all errors, then all warnings
                            // (first component is used as exception base, the rest are only added as suppressed)
                            (problems.values.flatMap { it.errors }) + (problems.values.flatMap { it.warnings })
                            ).distinct().asException()
                    if (exception is GameTestAssertPosException) throw exception
                    val message = exception.message ?: exception::class.java.simpleName
                    throw GameTestAssertPosException(
                        message,
                        absolutePos(blockPos),
                        blockPos,
                        tick
                    ).also {
                        it.addSuppressed(exception)
                    }
                }
            }
            blockResult = block(result)
        }
    sequenceCustomizer(sequence, result) { blockResult!! }
}
