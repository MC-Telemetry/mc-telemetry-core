package de.mctelemetry.core.observations.model

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.utils.ExceptionComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.ThrowingComponent
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.util.StringRepresentable

sealed class ObservationSourceErrorState(val type: Type) {

    enum class Type(private val serializedName: String) : StringRepresentable {
        Ok("ok"),
        Warnings("warnings"),
        Errors("errors"),
        ;

        override fun getSerializedName() = serializedName
    }

    abstract val warnings: List<Component>
    abstract val errors: List<Component>

    abstract fun errorsAsException(): Exception?
    abstract fun errorsOrWarningsAsException(): Exception?
    open fun throwErrors() {
        throw errorsAsException() ?: return
    }

    open fun throwErrorsOrWarnings() {
        throw errorsOrWarningsAsException() ?: return
    }

    fun withWarning(warning: Component): ObservationSourceErrorState {
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

    fun withoutWarning(warning: Component): ObservationSourceErrorState {
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

    fun withoutTranslatableWarning(translationKey: String): ObservationSourceErrorState {
        val warning = warnings.firstOrNull { it is TranslatableContents && it.key == translationKey }
        if (warning == null) return this
        return withoutWarning(warning)
    }

    fun withError(error: Component): ObservationSourceErrorState {
        val errors = errors
        if (error in errors) return this
        val newErrors = errors + error
        return Errors(errors = newErrors, warnings = warnings)
    }

    fun withoutError(error: Component): ObservationSourceErrorState {
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

    fun withoutTranslatableError(translationKey: String): ObservationSourceErrorState {
        val error = errors.firstOrNull { component -> (component is TranslatableContents && component.key == translationKey)
                || ((component.contents as? TranslatableContents)?.key == translationKey) }
        if (error == null) return this
        return withoutError(error)
    }

    fun withException(ex: Exception): ObservationSourceErrorState {
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

        other as ObservationSourceErrorState

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
    ) : ObservationSourceErrorState(Type.Errors) {

        init {
            require(errors.isNotEmpty()) { "Errors must contain at least one element" }
        }

        constructor(errors: List<Component>) : this(errors, emptyList())

        override fun toString(): String {
            return "ObservationSourceErrorState.Errors(errors=$errors, warnings=$warnings)"
        }

        override fun errorsAsException(): Exception = errors.asException()

        override fun errorsOrWarningsAsException(): Exception = (errors + warnings).asException()

        override fun throwErrors(): Nothing {
            throw errors.asException()
        }

        override fun throwErrorsOrWarnings(): Nothing {
            throw (errors + warnings).asException()
        }
    }

    class Warnings(
        override val warnings: List<Component>,
    ) : ObservationSourceErrorState(Type.Warnings) {

        override val errors: List<Component> = emptyList()

        init {
            require(warnings.isNotEmpty()) { "Warnings must contain at least one element" }
        }

        override fun toString(): String {
            return "ObservationSourceErrorState.Warnings($warnings)"
        }

        override fun errorsAsException(): Nothing? = null

        override fun errorsOrWarningsAsException(): Exception = warnings.asException()

        override fun throwErrors() {}
        override fun throwErrorsOrWarnings(): Nothing {
            throw warnings.asException()
        }
    }

    object Ok : ObservationSourceErrorState(Type.Ok) {

        override val warnings: List<Component> = emptyList()
        override val errors: List<Component> = emptyList()

        override fun toString(): String {
            return "ObservationSourceErrorState.Ok"
        }

        override fun errorsAsException(): Nothing? = null
        override fun errorsOrWarningsAsException(): Nothing? = null

        override fun throwErrors() {}
        override fun throwErrorsOrWarnings() {}
    }

    companion object {

        internal val uninitializedError = TranslationKeys.Errors.observationsUninitialized()
        internal val notConfiguredWarning = TranslationKeys.Errors.observationsNotConfigured()
        val Uninitialized = Errors(listOf(uninitializedError))
        val NotConfigured = Warnings(listOf(notConfiguredWarning))
        val CODEC: Codec<ObservationSourceErrorState> =
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

        operator fun invoke(
            errors: List<Component>,
            warnings: List<Component>,
        ) = when {
            errors.isNotEmpty() -> Errors(errors, warnings)
            warnings.isNotEmpty() -> Warnings(warnings)
            else -> Ok
        }

        internal fun Collection<Component>.asException(): Exception {
            val size = this.size
            require(size > 0)
            return map { it.asException() }.reduce { acc, next ->
                acc.addSuppressed(next)
                acc
            }
        }

        internal fun Component.asException(): Exception {
            if (this is ExceptionComponent) return this.exception
            if (this is Exception) return this
            return ThrowingComponent(this)
        }
    }
}
