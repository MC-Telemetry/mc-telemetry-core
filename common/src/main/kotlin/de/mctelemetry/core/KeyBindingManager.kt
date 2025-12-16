package de.mctelemetry.core

import de.mctelemetry.core.ui.screens.InstrumentManagerScreen
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

@Environment(EnvType.CLIENT)
object KeyBindingManager {
    private val INSTRUMENT_MANAGER_MAPPING =
        KeyMapping("key.owo-ui-academy.begin", GLFW.GLFW_KEY_O, "key.categories.misc")

    fun register() {
        KeyMappingRegistry.register(INSTRUMENT_MANAGER_MAPPING)
        ClientTickEvent.CLIENT_POST.register {
            while (INSTRUMENT_MANAGER_MAPPING.consumeClick()) {
                it.setScreen(InstrumentManagerScreen())
            }
        }
    }
}