package de.mctelemetry.core.observations.model

import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentSubRegistration
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.ComponentSerialization

open class ObservationSourceState(
    val source: IObservationSource<*, *>,
) : AutoCloseable, IInstrumentAvailabilityCallback<IInstrumentRegistration.Mutable<*>> {

    protected val onDirtyListeners: MutableSet<(ObservationSourceState) -> Unit> = linkedSetOf()

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
            ObservationSourceErrorState.Ok -> true
            is ObservationSourceErrorState.Warnings -> true
            is ObservationSourceErrorState.Errors -> false
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
            instrumentSubRegistration = null
        } finally {
            availabilityCallbackCloser = null
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
        instrumentSubRegistrationFactory: InstrumentSubRegistrationFactory,
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
                    source, this, configuration, storedInstrument
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
            errorState = errorState
                .withoutError(ObservationSourceErrorState.uninitializedError)
                .withoutTranslatableError(TranslationKeys.Errors.ERRORS_OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND)
                .withWarning(ObservationSourceErrorState.notConfiguredWarning)
        } else {
            errorState = errorState.withoutWarning(ObservationSourceErrorState.notConfiguredWarning)
            if (instrumentSubRegistration == null) {
                errorState = if (instrument == null) {
                    errorState
                        .withoutError(ObservationSourceErrorState.uninitializedError)
                        .withError(
                            TranslationKeys.Errors.observationsConfigurationInstrumentNotFound(
                                configuration.instrument.name
                            )
                        )
                } else {
                    errorState
                        .withoutTranslatableError(TranslationKeys.Errors.ERRORS_OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND)
                        .withError(ObservationSourceErrorState.uninitializedError)
                }
            } else {
                errorState = errorState
                    .withoutError(ObservationSourceErrorState.uninitializedError)
                    .withoutTranslatableError(
                        TranslationKeys.Errors.ERRORS_OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND
                    )
            }
        }
        return setErrorState(errorState, silent = silent)
    }

    fun resetErrorState(silent: Boolean = false): Boolean {
        return updateDerivedErrorState(silent = silent, ObservationSourceErrorState.Ok)
    }

    protected fun triggerOnDirty() {
        onDirtyListeners.forEach { listener ->
            listener(this)
        }
    }

    fun subscribeToDirty(block: (ObservationSourceState) -> Unit) {
        onDirtyListeners.add(block)
    }

    fun unsubscribeFromDirty(block: (ObservationSourceState) -> Unit) {
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
            holderLookupProvider
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
        val errorStateTag = tag.getCompound("errorState")
        if (errorStateTag == null) {
            return ObservationSourceErrorState.Ok
        } else {
            val errorsTag = errorStateTag.get("errors") as? ListTag
            val warningsTag = errorStateTag.get("warnings") as? ListTag
            val errorComponents = errorsTag?.map {
                val component = ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, it).orThrow.first
                if (component.contents == ObservationSourceErrorState.uninitializedError.contents)
                    ObservationSourceErrorState.uninitializedError
                else
                    component
            }
            val warningComponents = warningsTag?.map {
                val component = ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, it).orThrow.first
                if (component.contents == ObservationSourceErrorState.notConfiguredWarning.contents)
                    ObservationSourceErrorState.notConfiguredWarning
                else
                    component
            }
            return if (errorComponents.isNullOrEmpty()) {
                if (warningComponents.isNullOrEmpty()) {
                    ObservationSourceErrorState.Ok
                } else {
                    ObservationSourceErrorState.Warnings(warningComponents)
                }
            } else {
                ObservationSourceErrorState.Errors(errorComponents, warningComponents.orEmpty())
            }
        }
    }

    open fun saveToTag(tag: CompoundTag) {
        saveErrorState(tag)
        val configurationTag = configuration?.saveToTag()
        if (configurationTag != null) {
            tag.put("configuration", configurationTag)
        }
    }

    protected open fun saveErrorState(tag: CompoundTag) {
        val errorState = errorState
        if (errorState != ObservationSourceErrorState.Ok) {
            tag.put("errorState", CompoundTag().apply {
                if (errorState.warnings.isNotEmpty()) {
                    put("warnings", ListTag().apply {
                        errorState.warnings.forEach {
                            add(ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, it).orThrow)
                        }
                    })
                }
                if (errorState.errors.isNotEmpty()) {
                    put("errors", ListTag().apply {
                        errorState.errors.forEach {
                            add(ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, it).orThrow)
                        }
                    })
                }
            })
        }
    }

    interface InstrumentSubRegistrationFactory {

        fun <T : IInstrumentRegistration.Mutable<T>> createInstrumentCallback(
            source: IObservationSource<*, *>,
            state: ObservationSourceState,
            configuration: ObservationSourceConfiguration,
            instrument: IInstrumentRegistration.Mutable<*>,
        ): IInstrumentRegistration.Callback<T>
    }
}
