package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.ScraperBlock
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.function.Supplier

object OTelCoreModBlockEntityTypes {

    object ScraperBlockEntityType : BlockEntityType<ScraperBlockEntity>(::ScraperBlockEntity, emptySet(), null) {
        override fun isValid(blockState: BlockState): Boolean {
            return blockState.block is ScraperBlock
        }
    }

    private val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(OTelCoreMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE)

    val SCRAPER_BLOCK_ENTITY: RegistrySupplier<BlockEntityType<ScraperBlockEntity>> =
        registerBlockEntity("scraper") {
            ScraperBlockEntityType
        }

    fun init() {
        BLOCK_ENTITIES.register()
    }

    fun <T : BlockEntityType<*>> registerBlockEntity(
        name: String, blockEntity: Supplier<T>,
    ): RegistrySupplier<T> {
        return BLOCK_ENTITIES.register<T>(ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name), blockEntity)
    }
}
