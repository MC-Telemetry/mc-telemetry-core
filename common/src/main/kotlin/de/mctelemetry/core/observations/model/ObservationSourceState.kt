package de.mctelemetry.core.observations.model

import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentSubRegistration
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf

typealias ObservationSourceStateID = UByte

open class ObservationSourceState<SC, I : IObservationSourceInstance<SC, *, I>>(
    instance: I,
    val id: ObservationSourceStateID,
) : AutoCloseable, IInstrumentAvailabilityCallback<IInstrumentRegistration.Mutable<*>> {

    val source: IObservationSource<SC, out I> = instance.source
    open var instance: I
        get() = instanceField
        set(value) {
            setInstance(value)
        }
    protected var instanceField: I = instance

    open fun setInstance(value: I, silent: Boolean = false): Boolean {
        val oldValue = instanceField
        if (oldValue === value) {
            return false
        }
        instanceField = value
        if (!silent) triggerOnDirty()
        return true
    }

    protected val onDirtyListeners: MutableSet<(ObservationSourceState<SC, I>) -> Unit> = linkedSetOf()

    var cascadeUpdates: Boolean = false
        set(value) {
            val previous = field
            field = value
            if (previous == value) return
            if (value) {
                runUpdateCascade()
            }
        }

    protected open fun runUpdateCascade(silent: Boolean = false): Boolean {
        var modified = false
        run {
            val configuration = configuration
            val oldInstrument = instrument
            if (oldInstrument != null && oldInstrument != configuration?.instrument) {
                modified = setInstrument(null, silent = true)
            }
        }
        run {
            val instrument = instrument
            val oldSubRegistration = instrumentSubRegistration
            if (oldSubRegistration != null && oldSubRegistration.baseInstrument != instrument) {
                modified = setInstrumentSubRegistration(null, silent = true) || modified
            }
        }
        run {
            modified = updateDerivedErrorState(silent = true) || modified
        }
        if (modified && !silent) triggerOnDirty()
        return modified
    }

    open var configuration: ObservationSourceConfiguration?
        set(value) {
            setConfiguration(value)
        }
        get() = configurationField
    protected var configurationField: ObservationSourceConfiguration? = null

    protected open fun setConfiguration(
        value: ObservationSourceConfiguration?,
        silent: Boolean = false,
    ): Boolean {
        val oldValue = configurationField
        if (oldValue === value) {
            return false
        }
        configurationField = value
        try {
            if (cascadeUpdates) {
                if (instrument != value?.instrument) {
                    setInstrument(null, silent = true)
                }
            }
        } finally {
            if (!silent) triggerOnDirty()
        }
        return true
    }


    open fun shouldBeObserved(): Boolean {
        val configuration = configuration ?: return false
        if (configuration.instrument.name.isEmpty()) return false
        return when (errorState) {
            ObservationSourceErrorState.NotConfigured -> false
            ObservationSourceErrorState.Configured.Ok -> true
            is ObservationSourceErrorState.Configured.Warnings -> true
            is ObservationSourceErrorState.Configured.Errors -> false
        }
    }


    var errorState: ObservationSourceErrorState
        set(value) {
            setErrorState(value)
        }
        get() = errorStateField
    protected var errorStateField: ObservationSourceErrorState = ObservationSourceErrorState.Uninitialized

    protected open fun setErrorState(value: ObservationSourceErrorState, silent: Boolean = false): Boolean {
        val oldValue = errorStateField
        if (oldValue === value) {
            return false
        }
        errorStateField = value
        if (!silent) triggerOnDirty()
        return true
    }

    var instrument: IInstrumentRegistration?
        set(value) {
            setInstrument(value)
        }
        get() = instrumentField
    protected var instrumentField: IInstrumentRegistration? = null

    protected open fun setInstrument(
        value: IInstrumentRegistration?,
        silent: Boolean = false,
    ): Boolean {
        val oldValue = instrumentField
        if (oldValue === value) {
            return false
        }
        instrumentField = value
        try {
            if (cascadeUpdates) {
                setInstrumentSubRegistration(null, silent = true)
            }
        } finally {
            if (!silent) triggerOnDirty()
        }
        return true
    }

    protected var instrumentSubRegistration: IInstrumentSubRegistration<*>?
        set(value) {
            setInstrumentSubRegistration(value)
        }
        get() = instrumentSubRegistrationField
    protected var instrumentSubRegistrationField: IInstrumentSubRegistration<*>? = null

    val hasSubRegistration: Boolean
        get() = instrumentSubRegistrationField != null

    override fun close() {
        try {
            try {
                instrumentSubRegistration = null
            } finally {
                availabilityCallbackCloser = null
            }
        } finally {
            onDirtyListeners.clear()
        }
    }

    protected open fun setInstrumentSubRegistration(
        value: IInstrumentSubRegistration<*>?,
        silent: Boolean = false,
    ): Boolean {
        val oldValue = instrumentSubRegistrationField
        if (oldValue === value) {
            return false
        }
        instrumentSubRegistrationField = null
        try {
            try {
                oldValue?.close()
            } finally {
                if (cascadeUpdates) {
                    updateDerivedErrorState(silent = true)
                }
            }
            if (value != null) {
                // only store new callback if old callback could be closed (if it couldn't be closed, this whole method will
                // throw, signaling to the caller that they have to close the callback themselves
                instrumentSubRegistrationField = value
                if (cascadeUpdates) {
                    updateDerivedErrorState(silent = true)
                }
            }
        } finally {
            if (!silent) triggerOnDirty()
        }
        return true
    }

    protected var availabilityCallbackCloser: AutoCloseable? = null
        set(value) {
            val oldValue = field
            field = value
            oldValue?.close()
        }

    open fun updateRegistration(
        manager: IMutableInstrumentManager,
        instrumentSubRegistrationFactory: InstrumentSubRegistrationFactory<SC>,
    ) {
        val configuration = configuration ?: return
        val configurationInstrument = configuration.instrument
        val currentInstrument = instrument
        if (hasSubRegistration) {
            if (currentInstrument === configurationInstrument || currentInstrument?.name == configurationInstrument.name)
                return
        }
        val storedInstrument = manager.findLocalMutable(configurationInstrument.name)
        if (storedInstrument == null) {
            if (currentInstrument != null || hasSubRegistration) {
                this.instrument = null
            }
            if (availabilityCallbackCloser == null) {
                availabilityCallbackCloser = manager.addLocalMutableRegistrationCallback(this)
            }
        } else {
            this.bindMutableInstrument(
                storedInstrument, instrumentSubRegistrationFactory.createInstrumentCallback(
                    this, configuration, storedInstrument
                )
            )
        }
    }

    open fun <T : IInstrumentRegistration.Mutable<T>> bindMutableInstrument(
        instrument: T,
        callback: IInstrumentRegistration.Callback<T>,
    ) {
        setInstrument(instrument, silent = true)
        runWithExceptionCleanup(cleanup = { setInstrument(null) }) {
            val subRegistration = instrument.addCallback(callback = callback)
            runWithExceptionCleanup(cleanup = subRegistration::close) {
                setInstrumentSubRegistration(subRegistration)
                availabilityCallbackCloser = null
            }
        }
    }

    override fun instrumentAdded(
        manager: IInstrumentManager,
        instrument: IInstrumentRegistration.Mutable<*>,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (phase != IInstrumentAvailabilityCallback.Phase.POST) return
        if (this.instrument != null) return
        val configuration = this.configuration ?: return
        if (configuration.instrument.name.equals(instrument.name, ignoreCase = true)) {
            setInstrument(instrument, silent = false)
        }
    }

    override fun instrumentRemoved(
        manager: IInstrumentManager,
        instrument: IInstrumentRegistration.Mutable<*>,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        // unbinding of instruments is handled via the instrument-subregistrations.
    }

    @Suppress("SameParameterValue")
    protected open fun updateDerivedErrorState(
        silent: Boolean = false,
        errorStateBase: ObservationSourceErrorState = this.errorState,
    ): Boolean {
        if (!cascadeUpdates) return false
        var errorState = errorStateBase
        val configuration = configuration
        if (configuration == null || configuration.instrument.name.isEmpty()) {
            errorState = ObservationSourceErrorState.NotConfigured
        } else {
            val configuredErrorState = errorState as? ObservationSourceErrorState.Configured
                ?: ObservationSourceErrorState.Configured.Ok
            if (instrumentSubRegistration == null) {
                errorState = if (instrument == null) {
                    configuredErrorState
                        .withoutError(ObservationSourceErrorState.uninitializedError)
                        .withError(
                            TranslationKeys.Errors.observationsConfigurationInstrumentNotFound(
                                configuration.instrument.name
                            )
                        )
                } else {
                    configuredErrorState
                        .withoutTranslatableError(TranslationKeys.Errors.OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND)
                        .withError(ObservationSourceErrorState.uninitializedError)
                }
            } else {
                errorState = configuredErrorState
                    .withoutError(ObservationSourceErrorState.uninitializedError)
                    .withoutTranslatableError(
                        TranslationKeys.Errors.OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND
                    )
            }
        }
        return setErrorState(errorState, silent = silent)
    }

    fun resetErrorState(silent: Boolean = false): Boolean {
        return updateDerivedErrorState(silent = silent, ObservationSourceErrorState.Configured.Ok)
    }

    protected fun triggerOnDirty() {
        onDirtyListeners.forEach { listener ->
            listener(this)
        }
    }

    fun subscribeToDirty(block: (ObservationSourceState<SC, I>) -> Unit): AutoCloseable {
        onDirtyListeners.add(block)
        return AutoCloseable {
            unsubscribeFromDirty(block)
        }
    }

    fun unsubscribeFromDirty(block: (ObservationSourceState<SC, I>) -> Unit) {
        onDirtyListeners.remove(block)
    }

    open fun loadFromByteBuf(
        bb: RegistryFriendlyByteBuf,
        instrumentManager: IMutableInstrumentManager?,
        silent: Boolean = false,
    ) {
        loadFromTag(bb.readNbt() ?: return, bb.registryAccess(), instrumentManager, silent = silent)
    }

    open fun saveToByteBuf(bb: RegistryFriendlyByteBuf) {
        val tag = CompoundTag()
        saveToTag(tag)
        bb.writeNbt(tag)
    }

    open fun loadDelayedFromTag(
        tag: CompoundTag,
        holderLookupProvider: HolderLookup.Provider,
        initialSilent: Boolean = false,
        callbackSilent: Boolean = false,
    ): (IMutableInstrumentManager?) -> Unit {
        val errorState = loadErrorState(tag)
        val delayedConfiguration = ObservationSourceConfiguration.loadDelayedFromTag(
            tag.getCompound("configuration"),
            holderLookupProvider,
            instance.source,
        )
        var modified: Boolean
        runWithExceptionCleanup(::triggerOnDirty, runCleanup = !initialSilent) {
            modified = setErrorState(errorState, silent = true)
        }
        if (modified && !initialSilent) triggerOnDirty()
        return {
            var delayedHasModified: Boolean
            runWithExceptionCleanup(::triggerOnDirty, runCleanup = !callbackSilent) {
                delayedHasModified = setConfiguration(delayedConfiguration(it), silent = true)
                if (cascadeUpdates) {
                    delayedHasModified = runUpdateCascade(silent = true) || delayedHasModified
                }
            }
            if (delayedHasModified && !callbackSilent) triggerOnDirty()
        }
    }

    open fun loadFromTag(
        tag: CompoundTag,
        holderLookupProvider: HolderLookup.Provider,
        instrumentManager: IMutableInstrumentManager?,
        silent: Boolean = false,
    ) {
        val errorState = loadErrorState(tag)
        val configuration = ObservationSourceConfiguration.loadFromTag(
            tag.getCompound("configuration"),
            holderLookupProvider,
            instrumentManager,
            instance.source,
        )
        var modified: Boolean
        runWithExceptionCleanup(::triggerOnDirty, runCleanup = !silent) {
            modified = setErrorState(errorState, silent = true)
            modified = setConfiguration(configuration, silent = true) || modified
            if (cascadeUpdates) {
                modified = runUpdateCascade(silent = true) || modified
            }
        }
        if (modified && !silent) triggerOnDirty()
    }

    protected open fun loadErrorState(tag: CompoundTag): ObservationSourceErrorState {
        return ObservationSourceErrorState.fromTag(tag.getCompound("errorState"))
    }

    open fun saveToTag(tag: CompoundTag) {
        saveErrorState(tag)
        val configurationTag = configuration?.saveToTag()
        if (configurationTag != null) {
            tag.put("configuration", configurationTag)
        }
    }

    protected open fun saveErrorState(tag: CompoundTag) {
        val errorStateTag = CompoundTag().also(errorState::saveToTag)
        if (!errorStateTag.isEmpty)
            tag.put("errorState", errorStateTag)
    }

    interface InstrumentSubRegistrationFactory<out SC> {

        fun <T : IInstrumentRegistration.Mutable<T>> createInstrumentCallback(
            state: ObservationSourceState<in SC, *>,
            configuration: ObservationSourceConfiguration,
            instrument: IInstrumentRegistration.Mutable<*>,
        ): IInstrumentRegistration.Callback<T>
    }
}
