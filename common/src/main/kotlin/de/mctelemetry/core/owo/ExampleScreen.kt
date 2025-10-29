package de.mctelemetry.core.owo

import dev.architectury.event.events.client.ClientTickEvent
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

@Environment(EnvType.CLIENT)
class ExampleScreen : BaseOwoScreen<FlowLayout>() {

    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    override fun build(rootComponent: FlowLayout) {
        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)
        rootComponent.child(
            Components.button(Component.literal("A button")) {

            } as io.wispforest.owo.ui.core.Component
        )
    }

    companion object {

        object ClientTickListener : ClientTickEvent.Client {

            private var filterLevel = 42
            override fun tick(instance: Minecraft) {
                val player = instance.player ?: return
                if(player.experienceLevel == filterLevel && instance.screen == null) {
                    filterLevel = if(filterLevel != 42) 42 else 41
                    instance.setScreen(ExampleScreen())
                }
            }
        }

        fun registerTestScreenListener() {
            ClientTickEvent.CLIENT_POST.register(ClientTickListener)
        }
    }
}
