package de.mctelemetry.core.blocks

import de.mctelemetry.core.OTelCoreMod
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import java.util.function.Supplier

object OTelCoreModBlocks {
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(OTelCoreMod.MOD_ID, Registries.BLOCK)

    val REDSTONE_SCRAPER_BLOCK: RegistrySupplier<RedstoneScraperBlock> = registerBlock("redstone_scraper_block") {
        RedstoneScraperBlock(BlockBehaviour.Properties.of())
    }

    val ITEM_SCRAPER_BLOCK: RegistrySupplier<ItemScraperBlock> = registerBlock("item_scraper_block") {
        ItemScraperBlock(BlockBehaviour.Properties.of())
    }

    val FLUID_SCRAPER_BLOCK: RegistrySupplier<FluidScraperBlock> = registerBlock("fluid_scraper_block") {
        FluidScraperBlock(BlockBehaviour.Properties.of())
    }

    val ENERGY_SCRAPER_BLOCK: RegistrySupplier<EnergyScraperBlock> = registerBlock("energy_scraper_block") {
        EnergyScraperBlock(BlockBehaviour.Properties.of())
    }

    fun init() {
        BLOCKS.register()
    }

    private fun <T:Block> registerBlock(name: String, block: Supplier<T>): RegistrySupplier<T> {
        return BLOCKS.register(ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name), block)
    }
}
