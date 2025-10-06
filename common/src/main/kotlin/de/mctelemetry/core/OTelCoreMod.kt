package de.mctelemetry.core

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.*
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.function.Supplier


object OTelCoreMod {

    const val MOD_ID = "mcotelcore"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB)
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(MOD_ID, Registries.BLOCK)
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(MOD_ID, Registries.ITEM);

    val OTEL_TAB: RegistrySupplier<CreativeModeTab>

    val RUBY_BLOCK: RegistrySupplier<Block> = registerBlock("ruby_block", {
        Block(BlockBehaviour.Properties.of())
    })
    val RUBY: RegistrySupplier<Item> = registerItem("ruby_block", {
        BlockItem(RUBY_BLOCK.get(), Item.Properties().`arch$tab`(OTEL_TAB))
    })

    init {
        OTEL_TAB = TABS.register(
            "mcotelcore_tab",
            {
                CreativeTabRegistry.create(
                    Component.translatable("category.mcotelcore_tab"),
                    Supplier { ItemStack(RUBY) }
                )
            }
        )
    }

    fun init() {
        TABS.register()
        BLOCKS.register()
        ITEMS.register()
    }

    fun registerItem(name: String, item: Supplier<Item>): RegistrySupplier<Item> {
        return ITEMS.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item);
    }

    fun registerBlock(name: String, block: Supplier<Block?>?): RegistrySupplier<Block> {
        return BLOCKS.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), block)
    }
}
