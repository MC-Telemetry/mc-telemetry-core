package de.mctelemetry.core.fabric

import com.mojang.brigadier.arguments.ArgumentType
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.blocks.observation.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.commands.types.ArgumentTypes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.event.registry.RegistryAttribute
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
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register { blockEntity, level ->
            if(blockEntity !is ObservationSourceContainerBlockEntity) return@register
            OTelCoreMod.logger.trace(
                "Detected BlockEntityLoad-Event for {}, scheduling tick at {}",
                blockEntity,
                blockEntity.blockPos
            )
            level.scheduleTick(blockEntity.blockPos, blockEntity.blockState.block, 1)
        }
    }

    private fun registerContent() {
        val attributeTypeRegistry =
            FabricRegistryBuilder.createSimple(OTelCoreModAPI.AttributeTypeMappings)
                .attribute(RegistryAttribute.SYNCED)
                .attribute(RegistryAttribute.MODDED)
                .buildAndRegister()
        val observationSourceRegistry =
            FabricRegistryBuilder.createSimple(OTelCoreModAPI.ObservationSources)
                .attribute(RegistryAttribute.SYNCED)
                .attribute(RegistryAttribute.MODDED)
                .buildAndRegister()
        OTelCoreModBlockEntityTypesFabric.init()
        OTelCoreMod.registerAttributeTypes(attributeTypeRegistry)
        OTelCoreMod.registerObservationSources(observationSourceRegistry)
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
