package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestTimeoutException
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.collections.iterator
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class ObservationSourceContainer<C> : AutoCloseable, ObservationSourceState.InstrumentSubRegistrationFactory {

    abstract val observationStates: Map<IObservationSource<in C, *>, ObservationSourceState>

    abstract val context: C

    abstract val instrumentManager: IInstrumentManager

    open fun createAttributeLookup(): IMappedAttributeValueLookup = IMappedAttributeValueLookup.empty()


    protected val dirtyRunningTracker: ConcurrentSkipListSet<IObservationSource<*, *>> = ConcurrentSkipListSet(
        compareBy { it.id.location() }
    )

    override fun close() {
        observationStates.values.fold<ObservationSourceState, Exception?>(null) { acc, state ->
            try {
                state.close()
                acc
            } catch (ex: Exception) {
                acc + ex
            }
        }?.let { throw it }
    }

    protected open fun setup() {
        val cleanupList: Queue<AutoCloseable> = ConcurrentLinkedDeque()
        try {
            for ((source, state) in observationStates) {
                lateinit var cleanup: AutoCloseable
                cleanup = AutoCloseable {
                    cleanupList.remove(cleanup)
                    try {
                        dirtyRunningTracker.remove(state.source)
                        state.unsubscribeFromDirty(::onDirty)
                    } finally {
                        state.close()
                    }
                }
                dirtyRunningTracker.add(state.source)
                cleanupList.add(cleanup)
                state.subscribeToDirty(::onDirty)
                doOnDirty(source, state)
                dirtyRunningTracker.remove(state.source)
            }
            cleanupList.clear()
        } catch (ex: Exception) {
            var exAcc: Exception = ex
            while (cleanupList.isNotEmpty()) {
                val cleanup = cleanupList.remove()
                try {
                    cleanup.close()
                } catch (e: Exception) {
                    exAcc += e
                }
            }
            throw ex
        }
    }

    protected fun onDirty(sourceState: ObservationSourceState) {
        if (!dirtyRunningTracker.add(sourceState.source)) return
        try {
            val source = sourceState.source
            assert(
                observationStates.getValue(
                    @Suppress("UNCHECKED_CAST")
                    (source as IObservationSource<in C, *>)
                ) === sourceState
            )
            doOnDirty(source, sourceState)
        } finally {
            dirtyRunningTracker.remove(sourceState.source)
        }
    }

    protected open fun doOnDirty(source: IObservationSource<in C, *>, state: ObservationSourceState) {
        if (state.cascadeUpdates) {
            val instrumentManager = instrumentManager
            if (instrumentManager is IMutableInstrumentManager) {
                runWithExceptionCleanup(cleanup = { state.instrument = null }) {
                    state.updateRegistration(instrumentManager, this)
                }
            }
        }
    }

    override fun <T : IInstrumentRegistration.Mutable<T>> createInstrumentCallback(
        source: IObservationSource<*, *>,
        state: ObservationSourceState,
        configuration: ObservationSourceConfiguration,
        instrument: IInstrumentRegistration.Mutable<*>,
    ): IInstrumentRegistration.Callback<T> {
        @Suppress("UNCHECKED_CAST")
        return DefaultCallback(source as IObservationSource<C, *>, state)
    }

    protected inner class DefaultCallback(
        private val source: IObservationSource<in C, *>,
        private val state: ObservationSourceState,
    ) : IInstrumentRegistration.Callback<IInstrumentRegistration> {

        override fun observe(instrument: IInstrumentRegistration, recorder: IObservationRecorder.Resolved) {
            assert(state.instrument === instrument)
            this@ObservationSourceContainer.observe(recorder, source)
        }

        override fun onRemove(instrument: IInstrumentRegistration) {
            assert(state.instrument === instrument)
            state.instrument = null
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Resolved,
        source: IObservationSource<in C, *>,
        forceObservation: Boolean = false,
    ) {
        val state = observationStates.getValue(source)
        withValidMapping(state, forceObservation = forceObservation) { mapping ->
            val mappingResolver = ObservationMappingResolver(recorder, mapping)
            doObservation(
                source,
                context,
                createAttributeLookup(),
                mutableSetOf(),
                mapping,
                mappingResolver,
            )
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Resolved,
        filter: Set<IObservationSource<in C, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        val attributeLookup = createAttributeLookup()
        val context = context
        var mappingResolver: ObservationMappingResolver? = null
        val unusedAttributesSet: MutableSet<MappedAttributeKeyInfo<*, *>> = mutableSetOf()
        for ((source, state) in observationStates) {
            if (filter != null && source !in filter) continue
            try {
                if ((!forceObservation) && !state.shouldBeObserved()) continue
                withValidMapping(state, forceObservation = forceObservation) { mapping ->
                    if (mappingResolver != null) {
                        mappingResolver.mapping = mapping
                    } else {
                        mappingResolver = ObservationMappingResolver(recorder, mapping)
                    }
                    doObservation(
                        source,
                        context,
                        attributeLookup,
                        unusedAttributesSet,
                        mapping,
                        mappingResolver,
                    )
                }
            } catch (e: RuntimeException) {
                if (e is GameTestAssertException || e is GameTestTimeoutException) throw e
                state.errorState = (state.errorState as? ObservationSourceErrorState.Configured
                    ?: ObservationSourceErrorState.Configured.Ok).withException(e)
            }
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Unresolved,
        source: IObservationSource<in C, *>,
        forceObservation: Boolean = false,
    ) {
        val state = observationStates.getValue(source)
        withValidMapping(state, forceObservation = forceObservation) { mapping ->
            doObservation(
                source,
                context,
                createAttributeLookup(),
                mutableSetOf(),
                mapping,
                recorder,
            )
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Unresolved,
        filter: Set<IObservationSource<in C, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        val attributeLookup = createAttributeLookup()
        val context = context
        val unusedAttributesSet: MutableSet<MappedAttributeKeyInfo<*, *>> = mutableSetOf()
        for ((source, state) in observationStates) {
            if (filter != null && source !in filter) continue
            withValidMapping(state, forceObservation = forceObservation) { mapping ->
                doObservation(
                    source,
                    context,
                    attributeLookup,
                    unusedAttributesSet,
                    mapping,
                    recorder,
                )
            }
        }
    }

    protected inline fun withValidMapping(
        state: ObservationSourceState,
        forceObservation: Boolean = false,
        observationBlock: (ObservationAttributeMapping) -> Unit,
    ) {
        contract {
            callsInPlace(observationBlock, InvocationKind.AT_MOST_ONCE)
        }
        try {
            if ((!forceObservation) && !state.shouldBeObserved()) return
            val configuration = state.configuration ?: return
            val instrument = configuration.instrument
            val mapping = configuration.mapping
            val validationError = mapping.validate(instrument.attributes.values)
            if (validationError != null) {
                state.errorState = (state.errorState as? ObservationSourceErrorState.Configured
                    ?: ObservationSourceErrorState.Configured.Ok).withError(validationError)
                return
            }
            observationBlock(mapping)
        } catch (e: RuntimeException) {
            if (e is GameTestAssertException || e is GameTestTimeoutException) throw e
            state.errorState = (state.errorState as? ObservationSourceErrorState.Configured
                ?: ObservationSourceErrorState.Configured.Ok).withException(e)
        }
    }

    protected open fun <A : IMappedAttributeValueLookup> doObservation(
        source: IObservationSource<in C, A>,
        context: C,
        parentLookup: IMappedAttributeValueLookup,
        unusedAttributesSet: MutableSet<MappedAttributeKeyInfo<*, *>>,
        mapping: ObservationAttributeMapping,
        recorder: IObservationRecorder.Unresolved,
    ) {
        val lookup = source.createAttributeLookup(context, parentLookup)
        unusedAttributesSet.clear()
        mapping.findUnusedAttributes(lookup.attributeKeys, unusedAttributesSet)
        recorder.onNewSource(source)
        source.observe(context, recorder, lookup, unusedAttributesSet)
    }
}
