package de.mctelemetry.core.ui

import de.mctelemetry.core.OTelCoreMod
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.registry.menu.MenuRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.inventory.MenuType
import java.util.function.Supplier


object OTelCoreModMenuTypes {
    val MENU_TYPES: DeferredRegister<MenuType<*>> =
        DeferredRegister.create(OTelCoreMod.MOD_ID, Registries.MENU)

    val REDSTONE_SCRAPER_BLOCK: RegistrySupplier<MenuType<RedstoneScraperBlockMenu>> = registerMenuType(
        "redstone_scraper_block"
    ) {
        MenuType(
            MenuType.MenuSupplier(::RedstoneScraperBlockMenu),
            FeatureFlagSet.of()
        )
    }

    fun init() {
        MENU_TYPES.register()

            ClientLifecycleEvent.CLIENT_STARTED.register(ClientLifecycleEvent.ClientState { _ ->
                MenuRegistry.registerScreenFactory(
                    REDSTONE_SCRAPER_BLOCK.get(),
                    MenuRegistry.ScreenFactory(::RedstoneScraperBlockScreen))
            })
    }

    fun <T : MenuType<*>> registerMenuType(name: String, menuType: Supplier<T>): RegistrySupplier<T> {
        return MENU_TYPES.register<T>(ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, name), menuType)
    }
}