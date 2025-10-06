package de.mctelemetry.core

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.function.Supplier


object OTelCoreMod {

    const val MOD_ID = "mcotelcore"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(MOD_ID, Registries.ITEM);

    var RUBY: RegistrySupplier<Item>? = null

    fun init() {
        RUBY = registerItem("ruby", {
            Item(Item.Properties().`arch$tab`(CreativeModeTabs.INGREDIENTS))
        })

        ITEMS.register()
    }

    fun registerItem(name: String, item: Supplier<Item>): RegistrySupplier<Item> {
        return ITEMS.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item);
    }
}
