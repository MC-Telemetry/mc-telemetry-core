package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.utils.closeAllRethrow
import de.mctelemetry.core.utils.forEachRethrow
import de.mctelemetry.core.utils.runWithExceptionCleanup
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.ByteArraySet
import it.unimi.dsi.fastutil.bytes.ByteSet
import it.unimi.dsi.fastutil.bytes.ByteSets
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestTimeoutException
import net.minecraft.nbt.Tag
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class ObservationSourceContainer<SC> : AutoCloseable,
    ObservationSourceState.InstrumentSubRegistrationFactory<SC> {

    abstract val observationSources: Set<IObservationSource<in SC, *>>
    abstract val observationStates: Byte2ObjectMap<ObservationSourceState<in SC, *>>

    abstract val context: SC

    abstract val instrumentManager: IInstrumentManager

    open fun createAttributeLookup(): IAttributeValueStore = IAttributeValueStore.empty()

    protected val onStateAddedCallbacks: MutableSet<(ObservationSourceContainer<SC>, ObservationSourceState<in SC, *>) -> Unit> =
        linkedSetOf()


    protected val onStateRemovedCallbacks: MutableSet<(ObservationSourceContainer<SC>, ObservationSourceState<in SC, *>) -> Unit> =
        linkedSetOf()

    fun subscribeOnStateAdded(block: (ObservationSourceContainer<SC>, ObservationSourceState<in SC, *>) -> Unit): AutoCloseable {
        onStateAddedCallbacks.add(block)
        return AutoCloseable {
            unsubscribeOnStateAdded(block)
        }
    }

    fun unsubscribeOnStateAdded(block: (ObservationSourceContainer<SC>, ObservationSourceState<in SC, *>) -> Unit) {
        onStateAddedCallbacks.remove(block)
    }

    fun subscribeOnStateRemoved(block: (ObservationSourceContainer<SC>, ObservationSourceState<in SC, *>) -> Unit): AutoCloseable {
        onStateRemovedCallbacks.add(block)
        return AutoCloseable {
            unsubscribeOnStateRemoved(block)
        }
    }

    fun unsubscribeOnStateRemoved(block: (ObservationSourceContainer<SC>, ObservationSourceState<in SC, *>) -> Unit) {
        onStateRemovedCallbacks.remove(block)
    }

    protected fun triggerStateAdded(state: ObservationSourceState<in SC, *>) {
        onStateAddedCallbacks.forEachRethrow {
            it(this, state)
        }
    }

    protected fun triggerStateRemoved(state: ObservationSourceState<in SC, *>) {
        onStateRemovedCallbacks.forEachRethrow {
            it(this, state)
        }
    }

    protected val dirtyRunningTracker: ByteSet = ByteSets.synchronize(ByteArraySet(1))

    override fun close() {
        observationStates.values.closeAllRethrow()
    }

    protected open fun setupCallback(state: ObservationSourceState<in SC, *>) {
        dirtyRunningTracker.add(state.id.toByte())
        state.subscribeToDirty(::onDirty)
        runWithExceptionCleanup({ state.unsubscribeFromDirty(::onDirty) }) {
            try {
                doOnDirty(state)
            } finally {
                dirtyRunningTracker.remove(state.id.toByte())
            }
        }
    }

    protected open fun setupCallbacks() {
        for (state in observationStates.values) {
            runWithExceptionCleanup(state::close) {
                setupCallback(state)
            }
        }
    }

    protected fun onDirty(sourceState: ObservationSourceState<in SC, *>) {
        if (!dirtyRunningTracker.add(sourceState.id.toByte())) return
        try {
            assert(
                observationStates.getValue(
                    sourceState.id.toByte()
                ) === sourceState
            )
            doOnDirty(sourceState)
        } finally {
            dirtyRunningTracker.remove(sourceState.id.toByte())
        }
    }

    protected open fun doOnDirty(state: ObservationSourceState<in SC, *>) {
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
        state: ObservationSourceState<in SC, *>,
        configuration: ObservationSourceConfiguration,
        instrument: IInstrumentRegistration.Mutable<*>,
    ): IInstrumentRegistration.Callback<T> {
        @Suppress("UNCHECKED_CAST")
        return DefaultCallback(state)
    }

    protected inner class DefaultCallback(
        private val state: ObservationSourceState<in SC, *>,
    ) : IInstrumentRegistration.Callback<IInstrumentRegistration> {

        override fun observe(instrument: IInstrumentRegistration, recorder: IObservationRecorder.Resolved) {
            assert(state.instrument === instrument)
            this@ObservationSourceContainer.observe(recorder, state)
        }

        override fun onRemove(instrument: IInstrumentRegistration) {
            assert(state.instrument === instrument)
            state.instrument = null
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Resolved,
        state: ObservationSourceState<in SC, *>,
        forceObservation: Boolean = false,
    ) {
        withValidMapping(state, forceObservation = forceObservation) { mapping ->
            val mappingResolver = ObservationMappingResolver(recorder, mapping)
            doObservation(
                state.instance,
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
        filter: Set<ObservationSourceState<in SC, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        val attributeLookup = createAttributeLookup()
        val context = context
        var mappingResolver: ObservationMappingResolver? = null
        val unusedAttributesSet: MutableSet<AttributeDataSource<*>> = mutableSetOf()
        for (state in observationStates.values) {
            if (filter != null && state !in filter) continue
            try {
                if ((!forceObservation) && !state.shouldBeObserved()) continue
                withValidMapping(state, forceObservation = forceObservation) { mapping ->
                    if (mappingResolver != null) {
                        mappingResolver.mapping = mapping
                    } else {
                        mappingResolver = ObservationMappingResolver(recorder, mapping)
                    }
                    doObservation(
                        state.instance,
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
        state: ObservationSourceState<in SC, *>,
        forceObservation: Boolean = false,
    ) {
        withValidMapping(state, forceObservation = forceObservation) { mapping ->
            doObservation(
                state.instance,
                context,
                createAttributeLookup(),
                mutableSetOf(),
                mapping,
                recorder,
            )
        }
    }

    open fun observe(
        recorderFactory: (ObservationAttributeMapping, ObservationSourceState<in SC, *>) -> IObservationRecorder.Unresolved,
        state: ObservationSourceState<in SC, *>,
        forceObservation: Boolean = false,
    ) {
        withValidMapping(state, forceObservation = forceObservation) { mapping ->
            doObservation(
                state.instance,
                context,
                createAttributeLookup(),
                mutableSetOf(),
                mapping,
                recorderFactory(mapping, state),
            )
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Unresolved,
        filter: Set<ObservationSourceState<in SC, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        val attributeLookup = createAttributeLookup()
        val context = context
        val unusedAttributesSet: MutableSet<AttributeDataSource<*>> = mutableSetOf()
        for (state in observationStates.values) {
            if (filter != null && state !in filter) continue
            withValidMapping(state, forceObservation = forceObservation) { mapping ->
                doObservation(
                    state.instance,
                    context,
                    attributeLookup,
                    unusedAttributesSet,
                    mapping,
                    recorder,
                )
            }
        }
    }

    open fun observe(
        recorderFactory: (ObservationAttributeMapping, ObservationSourceState<in SC, *>) -> IObservationRecorder.Unresolved,
        filter: Set<ObservationSourceState<in SC, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        val attributeLookup = createAttributeLookup()
        val context = context
        val unusedAttributesSet: MutableSet<AttributeDataSource<*>> = mutableSetOf()
        for (state in observationStates.values) {
            if (filter != null && state !in filter) continue
            withValidMapping(state, forceObservation = forceObservation) { mapping ->
                doObservation(
                    state.instance,
                    context,
                    attributeLookup,
                    unusedAttributesSet,
                    mapping,
                    recorderFactory(mapping, state),
                )
            }
        }
    }

    protected inline fun withValidMapping(
        state: ObservationSourceState<in SC, *>,
        forceObservation: Boolean = false,
        observationBlock: (ObservationAttributeMapping) -> Unit,
    ) {
        contract {
            callsInPlace(observationBlock, InvocationKind.AT_MOST_ONCE)
        }
        try {
            if ((!forceObservation) && !state.shouldBeObserved()) return
            val configuration = state.configuration ?: return
            val instrument = state.instrument ?: return
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

    protected open fun <AS : IAttributeValueStore.Mutable> doObservation(
        sourceInstance: IObservationSourceInstance<in SC, AS, *>,
        sourceContext: SC,
        parentStore: IAttributeValueStore,
        unusedAttributesSet: MutableSet<AttributeDataSource<*>>,
        mapping: ObservationAttributeMapping,
        recorder: IObservationRecorder.Unresolved,
    ) {
        context(sourceContext) {
            val attributeStore = sourceInstance.createAttributeStore(parentStore)
            unusedAttributesSet.clear()
            mapping.findUnusedAttributeDataSources(attributeStore.references, unusedAttributesSet)
            recorder.onNewSource(sourceInstance)
            context(attributeStore) {
                sourceInstance.observe(recorder, unusedAttributesSet)
            }
        }
    }

    abstract fun addObservationSourceState(
        source: IObservationSource<in SC, *>,
        data: Tag? = null
    ): ObservationSourceState<in SC, *>

    abstract fun addObservationSourceState(
        instance: IObservationSourceInstance<in SC, *, *>
    ): ObservationSourceState<in SC, *>

    abstract fun removeObservationSourceState(id: ObservationSourceStateID): Boolean
}
