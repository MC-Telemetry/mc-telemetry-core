package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.IMappedAttributeValueLookup
import de.mctelemetry.core.api.metrics.IObservationRecorder
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.collections.iterator

abstract class ObservationSourceContainer<C> : AutoCloseable {

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
        runWithExceptionCleanup(cleanup = { state.instrument = null }) {
            updateRegistration(source, state)
        }
    }

    protected open fun updateRegistration(source: IObservationSource<in C, *>, state: ObservationSourceState) {
        val configuration = state.configuration ?: return
        val configurationInstrument = configuration.instrument
        val stateInstrument = state.instrument
        if (stateInstrument === configurationInstrument || stateInstrument?.name == configurationInstrument.name)
            return
        val storedInstrument = instrumentManager.findLocalMutable(configurationInstrument.name)
        if (storedInstrument == null) {
            state.instrument = null
        } else {
            state.bindMutableInstrument(storedInstrument, getCallback(source, state, configuration, storedInstrument))
        }
    }

    protected open fun getCallback(
        source: IObservationSource<in C, *>,
        state: ObservationSourceState,
        configuration: ObservationSourceConfiguration,
        instrument: IInstrumentRegistration.Mutable<*>,
    ): IInstrumentRegistration.Callback<IInstrumentRegistration.Mutable<*>> {
        return DefaultCallback(source, state)
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

    open fun observe(recorder: IObservationRecorder.Resolved, source: IObservationSource<in C, *>) {
        val state = observationStates.getValue(source)
        try {
            if (!state.shouldBeObserved()) return
            val configuration = state.configuration ?: return
            val instrument = configuration.instrument
            val mapping = configuration.mapping
            val validationError = mapping.validate(instrument.attributes.values)
            if (validationError != null) {
                state.errorState = state.errorState.withError(validationError)
                return
            }
            val mappingResolver = ObservationMappingResolver(recorder, configuration.mapping)
            doObservation(
                source,
                context,
                createAttributeLookup(),
                mutableSetOf(),
                mappingResolver,
            )
        } catch (e: RuntimeException) {
            state.errorState = state.errorState.withException(e)
        }
    }

    open fun observe(recorder: IObservationRecorder.Resolved, filter: Set<IObservationSource<in C, *>>? = null) {
        val attributeLookup = createAttributeLookup()
        val context = context
        var mappingResolver: ObservationMappingResolver? = null
        val unusedAttributesSet: MutableSet<MappedAttributeKeyInfo<*, *>> = mutableSetOf()
        for ((source, state) in observationStates) {
            if (filter != null && source !in filter) continue
            try {
                if (!state.shouldBeObserved()) continue
                val configuration = state.configuration ?: continue
                val instrument = configuration.instrument
                val mapping = configuration.mapping
                val validationError = mapping.validate(instrument.attributes.values)
                if (validationError != null) {
                    state.errorState = state.errorState.withError(validationError)
                    continue
                }
                if (mappingResolver != null) {
                    mappingResolver.mapping = configuration.mapping
                } else {
                    mappingResolver = ObservationMappingResolver(recorder, configuration.mapping)
                }
                doObservation(
                    source,
                    context,
                    attributeLookup,
                    unusedAttributesSet,
                    mappingResolver,
                )
            } catch (e: RuntimeException) {
                state.errorState = state.errorState.withException(e)
            }
        }
    }

    protected open fun <A : IMappedAttributeValueLookup> doObservation(
        source: IObservationSource<in C, A>,
        context: C,
        parentLookup: IMappedAttributeValueLookup,
        unusedAttributesSet: MutableSet<MappedAttributeKeyInfo<*, *>>,
        observer: ObservationMappingResolver,
    ) {
        val lookup = source.createAttributeLookup(context, parentLookup)
        unusedAttributesSet.clear()
        observer.mapping.findUnusedAttributes(lookup.keys, unusedAttributesSet)
        source.observe(context, observer, lookup, unusedAttributesSet)
    }
}
