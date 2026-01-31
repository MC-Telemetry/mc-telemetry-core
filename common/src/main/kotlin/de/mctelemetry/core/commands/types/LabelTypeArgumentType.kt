package de.mctelemetry.core.commands.types

import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.resources.ResourceLocation

class LabelTypeArgumentType(
    context: CommandBuildContext,
) : ResourceArgument<IAttributeKeyTypeTemplate<*, *>>(
    context,
    OTelCoreModAPI.AttributeTypeMappings,
) {

    companion object : IArgumentResolver<CommandSourceStack, IAttributeKeyTypeTemplate<*, *>> {

        val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "label_type"),
            SingletonArgumentInfo.contextAware(::LabelTypeArgumentType),
        )

        private val examples = listOf(
            "string",
            "minecraft:global_position",
        )

        override fun getValue(
            context: CommandContext<CommandSourceStack>,
            name: String,
        ): IAttributeKeyTypeTemplate<*, *> {
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
