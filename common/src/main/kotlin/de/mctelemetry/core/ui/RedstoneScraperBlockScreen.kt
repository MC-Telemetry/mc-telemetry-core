package de.mctelemetry.core.ui

import de.mctelemetry.core.OTelCoreMod
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
class RedstoneScraperBlockScreen : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "source-listing"
        )
    )
) {

    override fun build(rootComponent: FlowLayout) {
        (rootComponent.childById(
            ButtonComponent::class.java as Class<Component>,
            "the-button"
        ) as ButtonComponent).onPress {
            println("XML Button")
        }
    }
}