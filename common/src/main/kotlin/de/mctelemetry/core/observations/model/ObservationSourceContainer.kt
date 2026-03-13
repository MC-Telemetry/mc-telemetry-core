package de.mctelemetry.core.observations.model

import com.mojang.serialization.DynamicOps
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.utils.forEachRethrow
import de.mctelemetry.core.utils.runWithExceptionCleanup
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.ByteArraySet
import it.unimi.dsi.fastutil.bytes.ByteSet
import it.unimi.dsi.fastutil.bytes.ByteSets
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestTimeoutException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class ObservationSourceContainer<SO : Any> : AutoCloseable,
    ObservationSourceState.InstrumentSubRegistrationFactory<SO> {

    abstract val observationSources: Set<IObservationSource<in SO, *>>
    abstract val observationStates: Byte2ObjectMap<ObservationSourceState<in SO, *, *>>

    abstract val owner: SO?

    abstract val instrumentManager: IInstrumentManager

    open fun createAttributeLookup(): IAttributeValueStore = IAttributeValueStore.empty()

    protected val onStateAddedCallbacks: MutableSet<(ObservationSourceContainer<SO>, ObservationSourceState<in SO, *, *>) -> Unit> =
        linkedSetOf()


    protected val onStateRemovedCallbacks: MutableSet<(ObservationSourceContainer<SO>, ObservationSourceState<in SO, *, *>) -> Unit> =
        linkedSetOf()

    fun subscribeOnStateAdded(block: (ObservationSourceContainer<SO>, ObservationSourceState<in SO, *, *>) -> Unit): AutoCloseable {
        onStateAddedCallbacks.add(block)
        return AutoCloseable {
            unsubscribeOnStateAdded(block)
        }
    }

    fun unsubscribeOnStateAdded(block: (ObservationSourceContainer<SO>, ObservationSourceState<in SO, *, *>) -> Unit) {
        onStateAddedCallbacks.remove(block)
    }

    fun subscribeOnStateRemoved(block: (ObservationSourceContainer<SO>, ObservationSourceState<in SO, *, *>) -> Unit): AutoCloseable {
        onStateRemovedCallbacks.add(block)
        return AutoCloseable {
            unsubscribeOnStateRemoved(block)
        }
    }

    fun unsubscribeOnStateRemoved(block: (ObservationSourceContainer<SO>, ObservationSourceState<in SO, *, *>) -> Unit) {
        onStateRemovedCallbacks.remove(block)
    }

    protected fun triggerStateAdded(state: ObservationSourceState<in SO, *, *>) {
        onStateAddedCallbacks.forEachRethrow {
            it(this, state)
        }
    }

    protected fun triggerStateRemoved(state: ObservationSourceState<in SO, *, *>) {
        onStateRemovedCallbacks.forEachRethrow {
            it(this, state)
        }
    }

    protected val dirtyRunningTracker: ByteSet = ByteSets.synchronize(ByteArraySet(1))

    final override fun close() {
        close(false)
    }

    open fun close(silent: Boolean = false) {
        observationStates.values.forEachRethrow {
            it.close(silent)
        }
    }

    protected open fun setupCallback(state: ObservationSourceState<in SO, *, *>) {
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

    protected fun onDirty(sourceState: ObservationSourceState<in SO, *, *>) {
        if (!dirtyRunningTracker.add(sourceState.id.toByte())) return
        try {
            if (!sourceState.isClosed) {
                assert(
                    observationStates.getValue(
                        sourceState.id.toByte()
                    ) === sourceState
                )
            }
            doOnDirty(sourceState)
        } finally {
            dirtyRunningTracker.remove(sourceState.id.toByte())
        }
    }

    protected open fun doOnDirty(state: ObservationSourceState<in SO, *, *>) {
        if (state.cascadeUpdates && !state.isClosed) {
            val instrumentManager = instrumentManager
            if (instrumentManager is IMutableInstrumentManager) {
                runWithExceptionCleanup(cleanup = { state.instrument = null }) {
                    state.updateRegistration(instrumentManager, this)
                }
            }
            state.obtainObservationContext { owner ?: return }
        }
    }

    override fun <T : IInstrumentRegistration.Mutable<T>> createInstrumentCallback(
        state: ObservationSourceState<in SO, *, *>,
        configuration: ObservationSourceConfiguration,
        instrument: IInstrumentRegistration.Mutable<*>,
    ): IInstrumentRegistration.Callback<T> {
        @Suppress("UNCHECKED_CAST")
        return DefaultCallback(state)
    }

    protected inner class DefaultCallback(
        private val state: ObservationSourceState<in SO, *, *>,
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
        state: ObservationSourceState<in SO, *, *>,
        forceObservation: Boolean = false,
    ) {
        val owner = owner
        if (owner == null) {
            OTelCoreMod.logger.trace("Skipping observation of {} on {} due to missing owner", state, this)
            validateMapping(state, forceValidation = forceObservation)
            return
        }
        withValidMappingAndContext(state, owner, forceObservation = forceObservation) { mapping ->
            val mappingResolver = ObservationMappingResolver(recorder, mapping)
            state.doObservation(
                owner,
                createAttributeLookup(),
                mutableSetOf(),
                mapping,
                mappingResolver,
            )
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Resolved,
        filter: Set<ObservationSourceState<in SO, *, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        if (observationStates.isEmpty()) return
        val owner = owner
        if (owner == null) {
            OTelCoreMod.logger.trace("Skipping observations on {} due to missing owner", this)
            for (state in observationStates.values) {
                if (filter != null && state !in filter) continue
                validateMapping(state, forceValidation = forceObservation)
            }
            return
        }
        val attributeLookup = createAttributeLookup()
        var mappingResolver: ObservationMappingResolver? = null
        val unusedAttributesSet: MutableSet<AttributeDataSource<*>> = mutableSetOf()
        for (state in observationStates.values) {
            if (filter != null && state !in filter) continue
            try {
                if ((!forceObservation) && !state.shouldBeObserved()) continue
                withValidMappingAndContext(state, owner, forceObservation = forceObservation) { mapping ->
                    if (mappingResolver != null) {
                        mappingResolver.mapping = mapping
                    } else {
                        mappingResolver = ObservationMappingResolver(recorder, mapping)
                    }
                    state.doObservation(
                        owner,
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
        state: ObservationSourceState<in SO, *, *>,
        forceObservation: Boolean = false,
    ) {
        val owner = owner
        if (owner == null) {
            OTelCoreMod.logger.trace("Skipping observation of {} on {} due to missing owner", state, this)
            validateMapping(state, forceValidation = forceObservation)
            return
        }
        withValidMappingAndContext(state, owner, forceObservation = forceObservation) { mapping ->
            state.doObservation(
                owner,
                createAttributeLookup(),
                mutableSetOf(),
                mapping,
                recorder,
            )
        }
    }

    open fun observe(
        recorderFactory: (ObservationAttributeMapping, ObservationSourceState<in SO, *, *>) -> IObservationRecorder.Unresolved,
        state: ObservationSourceState<in SO, *, *>,
        forceObservation: Boolean = false,
    ) {
        val owner = owner
        if (owner == null) {
            OTelCoreMod.logger.trace("Skipping observation of {} on {} due to missing owner", state, this)
            validateMapping(state, forceValidation = forceObservation)
            return
        }
        withValidMappingAndContext(state, owner, forceObservation = forceObservation) { mapping ->
            state.doObservation(
                owner,
                createAttributeLookup(),
                mutableSetOf(),
                mapping,
                recorderFactory(mapping, state),
            )
        }
    }

    open fun observe(
        recorder: IObservationRecorder.Unresolved,
        filter: Set<ObservationSourceState<in SO, *, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        if (observationStates.isEmpty()) return
        val owner = owner
        if (owner == null) {
            OTelCoreMod.logger.trace("Skipping observations on {} due to missing owner", this)
            for (state in observationStates.values) {
                if (filter != null && state !in filter) continue
                validateMapping(state, forceValidation = forceObservation)
            }
            return
        }
        val attributeLookup = createAttributeLookup()
        val unusedAttributesSet: MutableSet<AttributeDataSource<*>> = mutableSetOf()
        for (state in observationStates.values) {
            if (filter != null && state !in filter) continue
            withValidMappingAndContext(state, owner, forceObservation = forceObservation) { mapping ->
                state.doObservation(
                    owner,
                    attributeLookup,
                    unusedAttributesSet,
                    mapping,
                    recorder,
                )
            }
        }
    }

    open fun observe(
        recorderFactory: (ObservationAttributeMapping, ObservationSourceState<in SO, *, *>) -> IObservationRecorder.Unresolved,
        filter: Set<ObservationSourceState<in SO, *, *>>? = null,
        forceObservation: Boolean = false,
    ) {
        if (observationStates.isEmpty()) return
        val owner = owner
        if (owner == null) {
            OTelCoreMod.logger.trace("Skipping observations on {} due to missing owner", this)
            for (state in observationStates.values) {
                if (filter != null && state !in filter) continue
                validateMapping(state, forceValidation = forceObservation)
            }
            return
        }
        val attributeLookup = createAttributeLookup()
        val unusedAttributesSet: MutableSet<AttributeDataSource<*>> = mutableSetOf()
        for (state in observationStates.values) {
            if (filter != null && state !in filter) continue
            withValidMappingAndContext(state, owner, forceObservation = forceObservation) { mapping ->
                state.doObservation(
                    owner,
                    attributeLookup,
                    unusedAttributesSet,
                    mapping,
                    recorderFactory(mapping, state),
                )
            }
        }
    }

    protected fun validateMapping(state: ObservationSourceState<in SO, *, *>, forceValidation: Boolean = false) {
        withValidMapping(state, forceValidation) {}
    }

    protected inline fun withValidMapping(
        state: ObservationSourceState<in SO, *, *>,
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

    protected inline fun withValidMappingAndContext(
        state: ObservationSourceState<in SO, *, *>,
        owner: SO,
        forceObservation: Boolean = false,
        observationBlock: (ObservationAttributeMapping) -> Unit
    ) {
        contract {
            callsInPlace(observationBlock, InvocationKind.AT_MOST_ONCE)
        }
        withValidMapping(state, forceObservation) { mapping ->
            //TODO: check if registration changes because of observation context change (can trigger dirty)
            if (!state.obtainObservationContext(owner)) {
                OTelCoreMod.logger.trace(
                    "Skipping observation of {} on {} for {} due to missing context",
                    state,
                    this,
                    owner
                )
                return
            }
            observationBlock(mapping)
        }
    }

    protected open fun <OC : AutoCloseable> ObservationSourceState<in SO, OC, *>.doObservation(
        sourceOwner: SO,
        parentStore: IAttributeValueStore,
        unusedAttributesSet: MutableSet<AttributeDataSource<*>>,
        mapping: ObservationAttributeMapping,
        recorder: IObservationRecorder.Unresolved,
    ) {
        val sourceInstance = instance
        context(sourceOwner, context ?: return) {
            val attributeStore = sourceInstance.createAttributeStore(parentStore)
            unusedAttributesSet.clear()
            mapping.findUnusedAttributeDataSources(attributeStore.references, unusedAttributesSet)
            recorder.onNewSource(sourceInstance)
            context(attributeStore) {
                sourceInstance.observe(recorder, unusedAttributesSet)
            }
        }
    }

    context(ops: DynamicOps<T>)
    abstract fun <T> addObservationSourceState(
        source: IObservationSource<in SO, *>,
        data: T? = null
    ): ObservationSourceState<in SO, *, *>

    abstract fun addObservationSourceState(
        instance: IObservationSourceInstance<in SO, *, *>
    ): ObservationSourceState<in SO, *, *>

    abstract fun removeObservationSourceState(id: ObservationSourceStateID): Boolean
}
