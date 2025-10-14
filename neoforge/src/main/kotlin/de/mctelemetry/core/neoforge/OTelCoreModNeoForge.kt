package de.mctelemetry.core.neoforge

import com.mojang.brigadier.arguments.ArgumentType
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IMappedAttributeKeyType
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.blocks.entities.RubyBlockEntity
import de.mctelemetry.core.commands.types.ArgumentTypes
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.core.WritableRegistry
import net.minecraft.core.registries.Registries
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Suppress("unused")
@Mod(OTelCoreMod.MOD_ID)
object OTelCoreModNeoForge {

    fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>, I : ArgumentTypeInfo<A, T>>
            ArgumentTypes.PreparedArgumentTypeRegistration<A, T, I>.register(defReg: DeferredRegister<ArgumentTypeInfo<*, *>>) {
        ArgumentTypeInfos.registerByClass<A, T, I>(infoClass, info)
        defReg.register<I>(this.id.path) { -> info }
    }

    private fun createRegistries(event: NewRegistryEvent) {
        val attributeKeyRegistry =
            RegistryBuilder(OTelCoreModAPI.AttributeTypeMappings).create() as WritableRegistry<IMappedAttributeKeyType<*, *>>
        OTelCoreMod.registerAttributeTypes(attributeKeyRegistry)
        event.register(attributeKeyRegistry)
    }

    private fun registerCallbacks() {
        FORGE_BUS.addListener(ServerStoppingEvent::class.java) { event: ServerStoppingEvent ->
            RubyBlockEntity.Ticker.unregisterAll()
        }
        MOD_BUS.addListener(::createRegistries)
    }

    private fun registerContent() {
        OTelCoreModBlockEntityTypesNeoForge.init()

        DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, OTelCoreMod.MOD_ID).let { defReg ->
            ArgumentTypes.register {
                it.register(defReg)
            }
            defReg.register(MOD_BUS)
        }
    }


    init {
        registerCallbacks()
        OTelCoreMod.init()
        registerContent()
    }
}
