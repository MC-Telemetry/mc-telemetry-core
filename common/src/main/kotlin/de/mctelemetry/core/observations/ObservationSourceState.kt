package de.mctelemetry.core.observations

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.utils.ExceptionComponent
import de.mctelemetry.core.utils.runWithExceptionCleanup
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.ThrowingComponent

open class ObservationSourceState(
    val source: IObservationSource<*, *>,
) : AutoCloseable {

    protected val onDirtyListeners: MutableSet<(ObservationSourceState) -> Unit> = linkedSetOf()

    open var configuration: ObservationSourceConfiguration?
        set(value) {
            setConfiguration(value)
        }
        get() = configurationField
    protected var configurationField: ObservationSourceConfiguration? = null

    protected open fun setConfiguration(value: ObservationSourceConfiguration?, silent: Boolean = false): Boolean {
        val oldValue = configurationField
        if (oldValue === value) {
            return false
        }
        configurationField = value
        try {
            if (Platform.getEnvironment() != Env.SERVER) {
                return true
            }
            val instrument = instrument
            if (instrument != null && instrument != value?.instrument) {
                setInstrument(value?.instrument as? IInstrumentRegistration, silent = true)
            }
        } finally {
            if (!silent) triggerOnDirty()
        }
        return true
    }


    fun shouldBeObserved(): Boolean {
        return errorState === ErrorState.Ok && configuration != null
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

    protected open fun setInstrument(value: IInstrumentRegistration?, silent: Boolean = false): Boolean {
        val oldValue = instrumentField
        if (oldValue === value) {
            return false
        }
        instrumentField = value
        try {
            if (Platform.getEnvironment() != Env.SERVER)
                return true
            setUnregisterCallback(null, silent = true)
        } finally {
            if (!silent) triggerOnDirty()
        }
        return true
    }

    protected var unregisterCallback: AutoCloseable?
        set(value) {
            setUnregisterCallback(value)
        }
        get() = unregisterCallbackField
    protected var unregisterCallbackField: AutoCloseable? = null

    override fun close() {
        unregisterCallback = null
    }

    protected open fun setUnregisterCallback(value: AutoCloseable?, silent: Boolean = false): Boolean {
        val oldValue = unregisterCallbackField
        if (oldValue === value) {
            return false
        }
        unregisterCallbackField = null
        try {
            try {
                oldValue?.close()
            } finally {
                // on server, always update errorState to reflect null callback
                if (Platform.getEnvironment() == Env.SERVER) {
                    setErrorState(errorState.withError(ErrorState.uninitializedError), silent = true)
                }
            }
            if (value != null) {
                // only store new callback if old callback could be closed (if it couldn't be closed, this whole method will
                // throw, signaling to the caller that they have to close the callback themselves
                unregisterCallbackField = value
                if (Platform.getEnvironment() == Env.SERVER) {
                    errorState = errorState.withoutError(ErrorState.uninitializedError)
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
            val closeCallback = instrument.addCallback(callback = callback)
            runWithExceptionCleanup(closeCallback::close) {
                setUnregisterCallback(closeCallback)
            }
        }
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

    open fun loadFromByteBuf(bb: RegistryFriendlyByteBuf) {
        loadFromTag(bb.readNbt() ?: return)
    }

    open fun saveToByteBuf(bb: RegistryFriendlyByteBuf) {
        val tag = CompoundTag()
        saveToTag(tag)
        bb.writeNbt(tag)
    }

    open fun loadFromTag(tag: CompoundTag) {
        val errorState = loadErrorState(tag)
        var modified: Boolean
        runWithExceptionCleanup(::triggerOnDirty) {
            modified = setErrorState(errorState, silent = true)
        }
        if (modified) triggerOnDirty()
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

    sealed class ErrorState {

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

        class Errors(
            override val errors: List<Component>,
            override val warnings: List<Component>,
        ) : ErrorState() {

            init {
                require(errors.isNotEmpty()) { "Errors must contain at least one element" }
            }

            constructor(errors: List<Component>) : this(errors, emptyList())
        }

        class Warnings(
            override val warnings: List<Component>,
        ) : ErrorState() {

            override val errors: List<Component> = emptyList()

            init {
                require(warnings.isNotEmpty()) { "Warnings must contain at least one element" }
            }
        }

        object Ok : ErrorState() {

            override val warnings: List<Component> = emptyList()
            override val errors: List<Component> = emptyList()
        }

        companion object {

            internal val uninitializedError = TranslationKeys.Errors.observationsUninitialized()
            val Uninitialized = Errors(listOf(uninitializedError))
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
