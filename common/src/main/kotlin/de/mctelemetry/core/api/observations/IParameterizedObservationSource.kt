package de.mctelemetry.core.api.observations

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import net.minecraft.network.chat.Component

interface IParameterizedObservationSource<SC, I : IObservationSourceInstance<SC, *, I>> : IObservationSource<SC, I> {

    val parameters: Map<String, Parameter<*>>

    context(parameters: ParameterMap)
    fun instanceFromParameters(): I
    fun makeParameterMap(): ParameterMap = ParameterMap(parameters)

    interface Parameter<T> {
        val argumentType: ArgumentType<T>
        val name: String
        val displayName: Component
            get() = Component.literal(name)
        val comment: Component? get() = null
        val optional: Boolean
            get() = false

        fun validate(value: T, context: Any? = null) {}

        interface WithDefault<T> : Parameter<T> {
            val default: T
            override val optional: Boolean
                get() = true
        }

        companion object {

            inline fun <I : IObservationSourceInstance<*, *, I>> IParameterizedObservationSource<*, I>.buildFromParameters(
                block: context(ParameterMap) () -> Unit
            ): I {
                val map = makeParameterMap()
                return context(map) {
                    instanceFromParameters()
                }
            }

            private data class ParameterInstance<T>(
                override val name: String,
                override val argumentType: ArgumentType<T>,
                override val displayName: Component = Component.literal(name),
                override val comment: Component? = null,
                override val optional: Boolean = false,
            ) : Parameter<T>


            private data class ParameterInstanceWithDefault<T>(
                override val name: String,
                override val argumentType: ArgumentType<T>,
                override val default: T,
                override val displayName: Component = Component.literal(name),
                override val comment: Component? = null,
            ) : WithDefault<T> {
                override val optional: Boolean = true
            }

            private data class ValidatingParameterInstance<T>(
                override val name: String,
                override val argumentType: ArgumentType<T>,
                override val displayName: Component = Component.literal(name),
                override val comment: Component? = null,
                override val optional: Boolean = false,
                val validator: (T, context: Any?) -> Unit
            ) : Parameter<T> {
                override fun validate(value: T, context: Any?) = validator(value, context)
            }


            private data class ValidatingParameterInstanceWithDefault<T>(
                override val name: String,
                override val argumentType: ArgumentType<T>,
                override val default: T,
                override val displayName: Component = Component.literal(name),
                override val comment: Component? = null,
                val validator: (T, context: Any?) -> Unit
            ) : WithDefault<T> {
                override fun validate(value: T, context: Any?) = validator(value, context)
                override val optional: Boolean = false

                init {
                    // default must pass the validator when invoked without context
                    validator(
                        default,
                        null
                    )
                }
            }

            operator fun <T> invoke(
                name: String,
                argumentType: ArgumentType<T>,
                displayName: Component = Component.literal(name),
                comment: Component? = null,
                optional: Boolean = false,
            ): Parameter<T> = ParameterInstance(name, argumentType, displayName, comment, optional)


            operator fun <T> invoke(
                name: String,
                argumentType: ArgumentType<T>,
                default: T,
                displayName: Component = Component.literal(name),
                comment: Component? = null,
            ): WithDefault<T> = ParameterInstanceWithDefault(name, argumentType, default, displayName, comment)

            operator fun <T> invoke(
                name: String,
                argumentType: ArgumentType<T>,
                displayName: Component = Component.literal(name),
                comment: Component? = null,
                optional: Boolean = false,
                validator: (T, context: Any?) -> Unit
            ): Parameter<T> =
                ValidatingParameterInstance(name, argumentType, displayName, comment, optional, validator)


            operator fun <T> invoke(
                name: String,
                argumentType: ArgumentType<T>,
                default: T,
                displayName: Component = Component.literal(name),
                comment: Component? = null,
                validator: (T, context: Any?) -> Unit
            ): WithDefault<T> =
                ValidatingParameterInstanceWithDefault(name, argumentType, default, displayName, comment, validator)

            context(parameterMap: ParameterMap)
            fun <T> Parameter<T>.set(value: T, context: Any? = null) {
                validate(value, context)
                parameterMap[this] = value
            }

            context(parameterMap: ParameterMap)
            fun <T> Parameter<T>.setFromText(reader: StringReader, context: Any? = null) {
                val value = if (context != null)
                    argumentType.parse(reader, context)
                else
                    argumentType.parse(reader)
                set(value, context)
            }

            context(parameterMap: ParameterMap)
            fun <T> Parameter<T>.setFromText(text: String, context: Any? = null) {
                setFromText(StringReader(text), context)
            }

            context(parameterMap: ParameterMap)
            fun <T> Parameter<T>.get(): T? {
                return parameterMap[this]
            }

            context(parameterMap: ParameterMap)
            val <T> Parameter<T>.isSet: Boolean
                get() = this in parameterMap
        }
    }

    interface ParameterMap {
        operator fun <T> set(parameter: Parameter<T>, value: T)
        fun <T> remove(parameter: Parameter<T>)
        operator fun <T> get(parameter: Parameter<T>): T
        fun <T> getOrElse(parameter: Parameter<T>, default: T): T {
            return if (parameter in this) get(parameter)
            else default
        }
        fun <T> getOrNull(parameter: Parameter<T>): T? {
            return if(parameter in this) get(parameter)
            else null
        }

        operator fun contains(parameter: Parameter<*>): Boolean
        val availableParameters: Map<String, Parameter<*>>

        companion object {

            operator fun invoke(parameters: Map<String, Parameter<*>>): ParameterMap {
                return DefaultMutableMap(parameters)
            }

            inline fun <T> ParameterMap.getOrElse(parameter: Parameter<T>, default:(Parameter<T>) -> T): T {
                return if(parameter in this)
                    get(parameter)
                else
                    default(parameter)
            }

            private class DefaultMutableMap(
                override val availableParameters: Map<String, Parameter<*>>,
            ) : ParameterMap {

                init {
                    for ((k, v) in availableParameters) {
                        require(v.name == k) { "Name does not match key for parameter $k->${v.name} ($v)" }
                    }
                }

                val data: MutableMap<Parameter<*>, Any?> = mutableMapOf()
                override fun <T> set(
                    parameter: Parameter<T>,
                    value: T
                ) {
                    data[parameter] = value
                }

                override fun <T> remove(parameter: Parameter<T>) {
                    data.remove(parameter)
                }

                override fun <T> get(parameter: Parameter<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    // `data` only hold entries in the form of `Parameter<T> : T`
                    return data.getValue(parameter) as T
                }

                override fun contains(parameter: Parameter<*>): Boolean {
                    return parameter in data
                }

                override fun <T> getOrElse(
                    parameter: Parameter<T>,
                    default: T
                ): T {
                    return super.getOrElse(parameter, default)
                }

                override fun <T> getOrNull(parameter: Parameter<T>): T? {
                    return super.getOrNull(parameter)
                }

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as DefaultMutableMap

                    if (availableParameters != other.availableParameters) return false
                    if (data != other.data) return false

                    return true
                }

                override fun hashCode(): Int {
                    return availableParameters.hashCode() // dont include `data` because it is mutable
                }
            }
        }
    }
}
