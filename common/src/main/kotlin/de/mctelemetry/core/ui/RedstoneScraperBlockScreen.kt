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

        val list = rootComponent.childById(
            FlowLayout::class.java as Class<Component>,
            "list"
        ) as FlowLayout

        list.child(
            this.model.expandTemplate(
                FlowLayout::class.java,
                "list-row@${OTelCoreMod.MOD_ID}:source-listing",
                mapOf("observation-source-name" to "Power 1", "observation-source-preview" to "12")
            )
        )

        list.child(
            this.model.expandTemplate(
                FlowLayout::class.java,
                "list-row@${OTelCoreMod.MOD_ID}:source-listing",
                mapOf("observation-source-name" to "Power 2", "observation-source-preview" to "23")
            )
        )

        list.child(
            this.model.expandTemplate(
                FlowLayout::class.java,
                "list-row@${OTelCoreMod.MOD_ID}:source-listing",
                mapOf("observation-source-name" to "Power 3", "observation-source-preview" to "34")
            )
        )
    }
}