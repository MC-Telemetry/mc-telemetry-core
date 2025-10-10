package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import java.util.function.Supplier

object OTelCoreModBlockEntityTypes {
    private val BLOCK_ENTITIES
            : DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create<BlockEntityType<*>>(OTelCoreMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE)

    lateinit var RUBY_BLOCK_ENTITY: RegistrySupplier<BlockEntityType<RubyBlockEntity>>

    fun writeRegister() {
        BLOCK_ENTITIES.register()
    }

    fun <T : BlockEntityType<*>> registerBlockEntity(
        name: String, blockEntity: Supplier<T>
    ): RegistrySupplier<T> {
        return BLOCK_ENTITIES.register<T>(ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name), blockEntity)
    }
}