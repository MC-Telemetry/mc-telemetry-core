package de.mctelemetry.core.commands.types

import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.attributes.IMappedAttributeKeyType
import de.mctelemetry.core.api.OTelCoreModAPI
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.resources.ResourceLocation

class LabelTypeArgumentType(
    context: CommandBuildContext,
) : ResourceArgument<IMappedAttributeKeyType<*, *>>(
    context,
    OTelCoreModAPI.AttributeTypeMappings,
) {

    companion object : IArgumentResolver<CommandSourceStack, IMappedAttributeKeyType<*, *>> {

        val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "label_type"),
            SingletonArgumentInfo.contextAware(::LabelTypeArgumentType),
        )

        private val examples = listOf(
            "mcotelcore:string",
            "mcotelcore:double",
        )

        override fun getValue(
            context: CommandContext<CommandSourceStack>,
            name: String,
        ): IMappedAttributeKeyType<*, *> {
            return getResource(
                context,
                name,
                OTelCoreModAPI.AttributeTypeMappings,
            ).value()
        }
    }


    override fun getExamples(): Collection<String> {
        return Companion.examples
    }
}
