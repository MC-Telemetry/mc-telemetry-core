package de.mctelemetry.core.observations.model

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.IInstrumentSubRegistration
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import de.mctelemetry.core.utils.ExceptionComponent
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.ThrowingComponent
import net.minecraft.util.StringRepresentable

open class ObservationSourceState(
    val source: IObservationSource<*, *>,
) : AutoCloseable {

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
            ErrorState.Ok -> true
            is ErrorState.Warnings -> true
            is ErrorState.Errors -> false
        }
    }


    var errorState: ErrorState
        set(value) {
            setErrorState(value)
        }
        get() = errorStateField
    protected var errorStateField: ErrorState = ErrorState.Uninitialized

    protected open fun setErrorState(value: ErrorState, silent: Boolean = false): Boolean {
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

    override fun close() {
        instrumentSubRegistration = null
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

    open fun <T : IInstrumentRegistration.Mutable<T>> bindMutableInstrument(
        instrument: T,
        callback: IInstrumentRegistration.Callback<T>,
    ) {
        setInstrument(instrument, silent = true)
        runWithExceptionCleanup(cleanup = { setInstrument(null) }) {
            val subRegistration = instrument.addCallback(callback = callback)
            runWithExceptionCleanup(cleanup = subRegistration::close) {
                setInstrumentSubRegistration(subRegistration)
            }
        }
    }

    @Suppress("SameParameterValue")
    protected open fun updateDerivedErrorState(silent: Boolean = false): Boolean {
        if (!cascadeUpdates) return false
        var errorState = this.errorState
        val configuration = configuration
        if (configuration == null || configuration.instrument.name.isEmpty()) {
            errorState = errorState
                .withoutError(ErrorState.uninitializedError)
                .withWarning(ErrorState.notConfiguredWarning)
        } else {
            errorState = errorState.withoutWarning(ErrorState.notConfiguredWarning)
            if (instrumentSubRegistration == null) {
                errorState = errorState.withError(ErrorState.uninitializedError)
            } else {
                errorState = errorState.withoutError(ErrorState.uninitializedError)
            }
        }
        return setErrorState(errorState, silent = silent)
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
        instrumentManager: IInstrumentManager,
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
    ): (IInstrumentManager) -> Unit {
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
        instrumentManager: IInstrumentManager,
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

    protected open fun loadErrorState(tag: CompoundTag): ErrorState {
        val errorStateTag = tag.getCompound("errorState")
        if (errorStateTag == null) {
            return ErrorState.Ok
        } else {
            val errorsTag = tag.get("errors") as? ListTag
            val warningsTag = tag.get("warnings") as? ListTag
            val errorComponents = errorsTag?.map {
                ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, it).orThrow.first
            }
            val warningComponents = warningsTag?.map {
                ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, it).orThrow.first
            }
            return if (errorComponents.isNullOrEmpty()) {
                if (warningComponents.isNullOrEmpty()) {
                    ErrorState.Ok
                } else {
                    ErrorState.Warnings(warningComponents)
                }
            } else {
                ErrorState.Errors(errorComponents, warningComponents.orEmpty())
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
        if (errorState != ErrorState.Ok) {
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

    sealed class ErrorState(val type: Type) {

        enum class Type(private val serializedName: String) : StringRepresentable {
            Ok("ok"),
            Warnings("warnings"),
            Errors("errors"),
            ;

            override fun getSerializedName() = serializedName
        }

        abstract val warnings: List<Component>
        abstract val errors: List<Component>

        fun withWarning(warning: Component): ErrorState {
            val warnings = warnings
            if (warning in warnings) return this
            val newWarnings = warnings + warning
            val errors = errors
            return if (errors.isNotEmpty()) {
                Errors(errors = errors, warnings = newWarnings)
            } else {
                Warnings(warnings = newWarnings)
            }
        }

        fun withoutWarning(warning: Component): ErrorState {
            val warnings = warnings
            if (warning !in warnings) return this
            val newWarnings = warnings - warning
            val errors = errors
            return if (errors.isNotEmpty()) {
                Errors(errors = errors, warnings = newWarnings)
            } else if (newWarnings.isNotEmpty()) {
                Warnings(warnings = newWarnings)
            } else {
                Ok
            }
        }

        fun withError(error: Component): ErrorState {
            val errors = errors
            if (error in errors) return this
            val newErrors = errors + error
            return Errors(errors = newErrors, warnings = warnings)
        }

        fun withoutError(error: Component): ErrorState {
            val errors = errors
            if (error !in errors) return this
            val newErrors = errors - error
            val warnings = warnings
            return if (newErrors.isNotEmpty()) {
                Errors(errors = newErrors, warnings = warnings)
            } else if (warnings.isNotEmpty()) {
                Warnings(warnings = warnings)
            } else {
                Ok
            }
        }

        fun withException(ex: Exception): ErrorState {
            return if (ex is ThrowingComponent) {
                withError(ex.component)
            } else if (errors.none {
                    it is ExceptionComponent && (
                            it.exception == ex ||
                                    (it.exception.javaClass == ex.javaClass && it.exception.message == ex.message)
                            )
                }) {
                withError(ExceptionComponent(ex))
            } else this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ErrorState

            if (type != other.type) return false
            if (warnings != other.warnings) return false
            if (errors != other.errors) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + warnings.hashCode()
            result = 31 * result + errors.hashCode()
            return result
        }


        class Errors(
            override val errors: List<Component>,
            override val warnings: List<Component>,
        ) : ErrorState(Type.Errors) {

            init {
                require(errors.isNotEmpty()) { "Errors must contain at least one element" }
            }

            constructor(errors: List<Component>) : this(errors, emptyList())

            override fun toString(): String {
                return "ErrorState.Errors(errors=$errors, warnings=$warnings)"
            }
        }

        class Warnings(
            override val warnings: List<Component>,
        ) : ErrorState(Type.Warnings) {

            override val errors: List<Component> = emptyList()

            init {
                require(warnings.isNotEmpty()) { "Warnings must contain at least one element" }
            }

            override fun toString(): String {
                return "ErrorState.Warnings($warnings)"
            }
        }

        object Ok : ErrorState(Type.Ok) {

            override val warnings: List<Component> = emptyList()
            override val errors: List<Component> = emptyList()

            override fun toString(): String {
                return "ErrorState.Ok"
            }
        }

        companion object {

            internal val uninitializedError = TranslationKeys.Errors.observationsUninitialized()
            internal val notConfiguredWarning = TranslationKeys.Errors.observationsNotConfigured()
            val Uninitialized = Errors(listOf(uninitializedError))
            val NotConfigured = Warnings(listOf(notConfiguredWarning))
            val CODEC: Codec<ErrorState> =
                Codec.pair(ComponentSerialization.CODEC.listOf(), ComponentSerialization.CODEC.listOf()).xmap(
                    {
                        val warnings = it.first
                        val errors = it.second
                        when {
                            errors.isNotEmpty() -> Errors(errors = errors, warnings = warnings)
                            warnings.isNotEmpty() -> Warnings(warnings = warnings)
                            else -> Ok
                        }
                    },
                    {
                        Pair.of(it.warnings, it.errors)
                    }
                )
        }
    }
}
