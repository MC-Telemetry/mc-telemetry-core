package de.mctelemetry.core.items

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.component.OTelCoreModComponents
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import java.util.function.Supplier

object OTelCoreModItems {
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(OTelCoreMod.MOD_ID, Registries.ITEM)

    val REDSTONE_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("redstone_scraper") {
        BlockItem(
            OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(),
            Item.Properties()
                .`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    val ITEM_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("item_scraper") {
        BlockItem(
            OTelCoreModBlocks.ITEM_SCRAPER_BLOCK.get(),
            Item.Properties()
                .`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    val FLUID_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("fluid_scraper") {
        BlockItem(
            OTelCoreModBlocks.FLUID_SCRAPER_BLOCK.get(),
            Item.Properties().`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    val ENERGY_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("energy_scraper") {
        BlockItem(
            OTelCoreModBlocks.ENERGY_SCRAPER_BLOCK.get(),
            Item.Properties()
                .`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    fun init() {
        ITEMS.register()
    }

    private fun registerItem(name: String, item: Supplier<Item>): RegistrySupplier<Item> {
        return ITEMS.register(ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name), item);
    }
}
