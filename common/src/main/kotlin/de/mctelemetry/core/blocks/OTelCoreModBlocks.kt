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

    val NBT_SCRAPER_BLOCK: RegistrySupplier<NbtScraperBlock> = registerBlock("nbt_scraper") {
        NbtScraperBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(3.5f))
    }

    val CONTAINER_SCRAPER_BLOCK: RegistrySupplier<ContainerScraperBlock> = registerBlock("container_scraper") {
        ContainerScraperBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(3.5f))
    }

    val REDSTONE_SCRAPER_BLOCK: RegistrySupplier<RedstoneScraperBlock> = registerBlock("redstone_scraper") {
        RedstoneScraperBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(3.5f))
    }

    fun init() {
        BLOCKS.register()
    }

    private fun <T:Block> registerBlock(name: String, block: Supplier<T>): RegistrySupplier<T> {
        return BLOCKS.register(ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name), block)
    }
}
