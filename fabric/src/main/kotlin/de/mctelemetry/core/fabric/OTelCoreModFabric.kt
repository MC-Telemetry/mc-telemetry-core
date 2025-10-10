package de.mctelemetry.core.fabric

import com.mojang.brigadier.arguments.ArgumentType
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.entities.RubyBlockEntity
import de.mctelemetry.core.commands.types.ArgumentTypes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.commands.synchronization.ArgumentTypeInfo

object OTelCoreModFabric : ModInitializer {
    fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>, I: ArgumentTypeInfo<A,T>>
            ArgumentTypes.PreparedArgumentTypeRegistration<A,T,I>.register() {
        ArgumentTypeRegistry.registerArgumentType(
            id,
            infoClass,
            info,
        )
    }

    override fun onInitialize() {
        OTelCoreMod.init()
        OTelCoreModBlockEntityTypesFabric.init()

        ArgumentTypes.register {
            it.register()
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            RubyBlockEntity.Ticker.unregisterAll()
        }
    }
}
