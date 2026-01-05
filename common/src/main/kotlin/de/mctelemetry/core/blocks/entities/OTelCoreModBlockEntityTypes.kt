package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import java.util.function.Supplier

object OTelCoreModBlockEntityTypes {

    private val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(OTelCoreMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE)

    val SCRAPER_BLOCK_ENTITY: RegistrySupplier<BlockEntityType<ScraperBlockEntity>> =
        registerBlockEntity("scraper_block") {
            BlockEntityType.Builder
                .of(
                    ::ScraperBlockEntity,
                    OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(),
                    OTelCoreModBlocks.ITEM_SCRAPER_BLOCK.get(),
                    OTelCoreModBlocks.FLUID_SCRAPER_BLOCK.get(),
                    OTelCoreModBlocks.ENERGY_SCRAPER_BLOCK.get()
                )
                .build(
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // null is actually acceptable here
                    null
                )
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
