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

    val TELEMETRY_CORE: RegistrySupplier<Item> = registerItem("telemetry_core") {
        Item(Item.Properties().`arch$tab`(OTelCoreMod.OTEL_TAB))
    }

    val NBT_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("nbt_scraper") {
        BlockItem(
            OTelCoreModBlocks.NBT_SCRAPER_BLOCK.get(),
            Item.Properties().`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    val CONTAINER_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("container_scraper") {
        BlockItem(
            OTelCoreModBlocks.CONTAINER_SCRAPER_BLOCK.get(),
            Item.Properties()
                .`arch$tab`(OTelCoreMod.OTEL_TAB)
                .component(OTelCoreModComponents.GENERATE_SINGLETON_STATES.get(), true)
        )
    }

    val REDSTONE_SCRAPER_BLOCK: RegistrySupplier<Item> = registerItem("redstone_scraper") {
        BlockItem(
            OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get(),
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
