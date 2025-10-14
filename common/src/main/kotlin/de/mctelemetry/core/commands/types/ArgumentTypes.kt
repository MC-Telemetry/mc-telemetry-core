package de.mctelemetry.core.commands.types

import com.mojang.brigadier.arguments.ArgumentType
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.resources.ResourceLocation

object ArgumentTypes {

    data class PreparedArgumentTypeRegistration<
            A : ArgumentType<*>,
            T : ArgumentTypeInfo.Template<A>,
            I : ArgumentTypeInfo<A, T>,
            >(
        val id: ResourceLocation,
        val info: I,
        val infoClass: Class<A>,
    ) {
                companion object {
                    inline operator fun <
                            reified A : ArgumentType<*>,
                            T : ArgumentTypeInfo.Template<A>,
                            I : ArgumentTypeInfo<A, T>
                            > invoke(
                        id: ResourceLocation,
                        info: I,
                    ): PreparedArgumentTypeRegistration<A,T,I> {
                        return PreparedArgumentTypeRegistration(
                            id,
                            info,
                            infoClass = A::class.java,
                        )
                    }
                }
            }

    val ALL_CUSTOM: Collection<PreparedArgumentTypeRegistration<*,*,*>> = listOf(
        MetricNameArgumentType.registration,
        LabelNameArgumentType.registration,
        LabelStringValueMapArgumentType.registration,
        LabelValueArgumentType.registration,
        LabelDefinitionArgumentType.registration,
        LabelTypeArgumentType.registration,
    )
    inline fun register(registrationBlock: (PreparedArgumentTypeRegistration<*, *, *>) -> Unit) {
        ALL_CUSTOM.forEach(registrationBlock)
    }
}
