package de.mctelemetry.core.observations

import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.IMappedAttributeValueLookup
import de.mctelemetry.core.api.metrics.IObservationObserver
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.core.RegistryAccess
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentSkipListSet

abstract class ObservationSourceContainer<C> {

    abstract val observationStates: Map<IObservationSource<in C, *>, ObservationSourceState>

    abstract val context: C

    abstract val registryAccess: RegistryAccess
    abstract val instrumentManager: IInstrumentManager

    open fun createAttributeLookup(): IMappedAttributeValueLookup = IMappedAttributeValueLookup.empty()


    protected val dirtyRunningTracker: ConcurrentSkipListSet<ObservationSourceState> = ConcurrentSkipListSet()

    protected open fun setup() {
        val cleanupList: Queue<AutoCloseable> = ConcurrentLinkedDeque()
        try {
            for ((source, state) in observationStates) {
                lateinit var cleanup: AutoCloseable
                cleanup = AutoCloseable {
                    cleanupList.remove(cleanup)
                    try {
                        dirtyRunningTracker.remove(state)
                        state.unsubscribeFromDirty(::onDirty)
                    } finally {
                        state.close()
                    }
                }
                dirtyRunningTracker.add(state)
                cleanupList.add(cleanup)
                state.subscribeToDirty(::onDirty)
                doOnDirty(source, state)
                dirtyRunningTracker.remove(state)
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
        }
    }

    protected fun onDirty(sourceState: ObservationSourceState) {
        if (!dirtyRunningTracker.add(sourceState)) return
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
            dirtyRunningTracker.remove(sourceState)
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

        override fun observe(instrument: IInstrumentRegistration, recorder: IObservationObserver.Resolved) {
            assert(state.instrument === instrument)
            this@ObservationSourceContainer.observe(recorder, source)
        }

        override fun onRemove(instrument: IInstrumentRegistration) {
            assert(state.instrument === instrument)
            state.instrument = null
        }
    }

    open fun observe(observer: IObservationObserver.Resolved, source: IObservationSource<in C, *>) {
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
            val mappingResolver = ObservationMappingResolver(observer, configuration.mapping)
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

    open fun observe(observer: IObservationObserver.Resolved, filter: Set<IObservationSource<in C, *>>? = null) {
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
                    mappingResolver = ObservationMappingResolver(observer, configuration.mapping)
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
