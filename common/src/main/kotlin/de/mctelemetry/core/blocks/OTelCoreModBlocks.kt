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

    val REDSTONE_SCRAPER_BLOCK: RegistrySupplier<Block> = registerBlock("redstone_scraper_block") {
        RedstoneScraperBlock(BlockBehaviour.Properties.of())
    }

    fun init() {
        BLOCKS.register()
    }

    private fun registerBlock(name: String, block: Supplier<Block?>?): RegistrySupplier<Block> {
        return BLOCKS.register(ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name), block)
    }
}