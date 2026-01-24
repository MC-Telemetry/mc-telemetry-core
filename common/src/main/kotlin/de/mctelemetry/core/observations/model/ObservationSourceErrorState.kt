package de.mctelemetry.core.observations.model

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.observations.model.ObservationSourceErrorState.Configured.Errors
import de.mctelemetry.core.observations.model.ObservationSourceErrorState.Configured.Ok
import de.mctelemetry.core.observations.model.ObservationSourceErrorState.Configured.Warnings
import de.mctelemetry.core.utils.ExceptionComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.ThrowingComponent
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.util.StringRepresentable
import java.util.Optional
import kotlin.jvm.optionals.getOrElse

sealed class ObservationSourceErrorState(val type: Type) {

    enum class Type(private val serializedName: String) : StringRepresentable {
        NotConfigured("notconfigured"),
        Ok("ok"),
        Warnings("warnings"),
        Errors("errors"),
        ;

        override fun getSerializedName() = serializedName
    }

    object NotConfigured : ObservationSourceErrorState(Type.NotConfigured)

    sealed class Configured(type: Type) : ObservationSourceErrorState(type) {

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

        fun withWarning(warning: Component): Configured {
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

        fun withoutWarning(warning: Component): Configured {
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

        fun withoutTranslatableWarning(translationKey: String): Configured {
            val warning = warnings.firstOrNull { it is TranslatableContents && it.key == translationKey }
            if (warning == null) return this
            return withoutWarning(warning)
        }

        fun withError(error: Component): Configured {
            val errors = errors
            if (error in errors) return this
            val newErrors = errors + error
            return Errors(errors = newErrors, warnings = warnings)
        }

        fun withoutError(error: Component): Configured {
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

        fun withoutTranslatableError(translationKey: String): Configured {
            val error = errors.firstOrNull { component ->
                (component is TranslatableContents && component.key == translationKey)
                        || ((component.contents as? TranslatableContents)?.key == translationKey)
            }
            if (error == null) return this
            return withoutError(error)
        }

        fun withException(ex: Exception): Configured {
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

            other as Configured

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
        ) : Configured(Type.Errors) {

            init {
                require(errors.isNotEmpty()) { "Errors must contain at least one element" }
            }

            constructor(errors: List<Component>) : this(errors, emptyList())

            override fun toString(): String {
                val warnings = warnings
                return if (warnings.isEmpty()) {
                    "ObservationSourceErrorState.Configured.Errors(errors=$errors)"
                } else {
                    "ObservationSourceErrorState.Configured.Errors(errors=$errors, warnings=$warnings)"
                }
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
        ) : Configured(Type.Warnings) {

            override val errors: List<Component> = emptyList()

            init {
                require(warnings.isNotEmpty()) { "Warnings must contain at least one element" }
            }

            override fun toString(): String {
                return "ObservationSourceErrorState.Configured.Warnings($warnings)"
            }

            override fun errorsAsException(): Nothing? = null

            override fun errorsOrWarningsAsException(): Exception = warnings.asException()

            override fun throwErrors() {}
            override fun throwErrorsOrWarnings(): Nothing {
                throw warnings.asException()
            }
        }

        object Ok : Configured(Type.Ok) {

            override val warnings: List<Component> = emptyList()
            override val errors: List<Component> = emptyList()

            override fun toString(): String {
                return "ObservationSourceErrorState.Configured.Ok"
            }

            override fun errorsAsException(): Nothing? = null
            override fun errorsOrWarningsAsException(): Nothing? = null

            override fun throwErrors() {}
            override fun throwErrorsOrWarnings() {}
        }

        companion object {

            operator fun invoke(
                errors: List<Component>,
                warnings: List<Component>,
            ) = when {
                errors.isNotEmpty() -> Errors(errors, warnings)
                warnings.isNotEmpty() -> Warnings(warnings)
                else -> Ok
            }
        }
    }


    companion object {

        internal val uninitializedError = TranslationKeys.Errors.observationsUninitialized()
        val Uninitialized = Errors(listOf(uninitializedError))

        val CODEC: Codec<ObservationSourceErrorState> = RecordCodecBuilder.create {
            it.group(
                Codec.BOOL.optionalFieldOf("ok", false).forGetter { errorState -> errorState == Ok },
                ComponentSerialization.CODEC.listOf()
                    .optionalFieldOf("errors")
                    .validate { errors ->
                        if (errors.isPresent && errors.get().isEmpty())
                            DataResult.error { "Errors must not be empty when present" }
                        else DataResult.success(errors)
                    }
                    .forGetter { errorState ->
                        if (errorState is Configured && errorState != Ok){
                            val errors = errorState.errors
                            if(errors.isEmpty())
                                Optional.empty()
                            else
                                Optional.of(errors)
                        }
                        else Optional.empty()
                    },
                ComponentSerialization.CODEC.listOf()
                    .optionalFieldOf("warnings")
                    .validate { warnings ->
                        if (warnings.isPresent && warnings.get().isEmpty())
                            DataResult.error { "Warnings must not be empty when present" }
                        else DataResult.success(warnings)
                    }
                    .forGetter { errorState ->
                        if (errorState is Configured && errorState != Ok) {
                            val warnings = errorState.warnings
                            if(warnings.isEmpty())
                                Optional.empty()
                            else
                                Optional.of(warnings)
                        }
                        else Optional.empty()
                    },
            ).apply(it) { ok, errorsOptional, warningsOptional ->
                val errors = errorsOptional.getOrElse(::emptyList)
                val warnings = warningsOptional.getOrElse(::emptyList)
                if (errors.isNotEmpty()) {
                    Errors(errors, warnings)
                } else if (warnings.isNotEmpty()) {
                    Warnings(warnings)
                } else if (ok) {
                    Ok
                } else {
                    NotConfigured
                }
            }
        }

        fun Collection<Component>.asException(): Exception {
            val size = this.size
            require(size > 0)
            return map { it.asException() }.reduce { acc, next ->
                acc.addSuppressed(next)
                acc
            }
        }

        fun Component.asException(): Exception {
            if (this is ExceptionComponent) return this.exception
            if (this is Exception) return this
            return ThrowingComponent(this)
        }
    }
}
