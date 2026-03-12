package de.mctelemetry.core.items

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.component.OTelCoreModComponents
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import java.util.function.Function
import java.util.function.Supplier

object OTelCoreModItems {
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(OTelCoreMod.MOD_ID, Registries.ITEM)

    val OBSERVATION_MODULE: RegistrySupplier<Item> = registerItem("observation_module") {
        GeoSubModelItem(
            Item.Properties().`arch$tab`(OTelCoreMod.OTEL_TAB),
            it,
            includeStatusLayer = true,
        )
    }

    val FRAME: RegistrySupplier<Item> = registerItem("frame") {
        GeoSubModelItem(
            Item.Properties().`arch$tab`(OTelCoreMod.OTEL_TAB),
            it,
            includeStatusLayer = false,
        )
    }

    val NBT_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("nbt_scraper") {
        ScraperBlockItem(
            OTelCoreModBlocks.NBT_SCRAPER_BLOCK.get(),
            Item.Properties().`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    val CONTAINER_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("container_scraper") {
        ScraperBlockItem(
            OTelCoreModBlocks.CONTAINER_SCRAPER_BLOCK.get(),
            Item.Properties()
                .`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    val REDSTONE_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("redstone_scraper") {
        ScraperBlockItem(
            OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(),
            Item.Properties()
                .`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    fun init() {
        ITEMS.register()
    }

    private fun registerItem(name: String, item: Function<ResourceLocation, Item>): RegistrySupplier<Item> {
        val id = ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name)
        return ITEMS.register(id) { item.apply(id) };
    }
}
