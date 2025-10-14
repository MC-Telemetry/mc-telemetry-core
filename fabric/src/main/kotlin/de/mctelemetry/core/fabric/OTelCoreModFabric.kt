package de.mctelemetry.core.fabric

import com.mojang.brigadier.arguments.ArgumentType
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.commands.types.ArgumentTypes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.minecraft.commands.synchronization.ArgumentTypeInfo

object OTelCoreModFabric : ModInitializer {

    private fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>, I : ArgumentTypeInfo<A, T>>
            ArgumentTypes.PreparedArgumentTypeRegistration<A, T, I>.register() {
        ArgumentTypeRegistry.registerArgumentType(
            id,
            infoClass,
            info,
        )
    }

    private fun registerCallbacks() {
    }

    private fun registerContent() {
        val attributeTypeRegistry =
            FabricRegistryBuilder.createSimple(OTelCoreModAPI.AttributeTypeMappings).buildAndRegister()
        OTelCoreModBlockEntityTypesFabric.init()
        OTelCoreMod.registerAttributeTypes(attributeTypeRegistry)
        ArgumentTypes.register {
            it.register()
        }
    }

    override fun onInitialize() {
        registerCallbacks()
        OTelCoreMod.init()
        registerContent()
    }
}
