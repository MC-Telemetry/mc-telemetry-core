package de.mctelemetry.core.ui

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
class RedstoneScraperBlockScreenDetails(
    val parent: Screen,
    val source: DemoObservationSource,
    val metrics: List<DemoMetric>
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "source-listing-details"
        )
    )
) {
    override fun build(rootComponent: FlowLayout) {
        val backButton = rootComponent.childById(
            ButtonComponent::class.java as Class<Component>,
            "back"
        ) as ButtonComponent
        backButton.onPress {
            Minecraft.getInstance().setScreen(parent)
        }

        val observationSourceNameLabel = rootComponent.childById(
            LabelComponent::class.java as Class<Component>,
            "observation-source-name"
        ) as LabelComponent
        observationSourceNameLabel.text(buildComponent(source.name) {})

        val metricNameTextBox = rootComponent.childById(
            TextBoxComponent::class.java as Class<Component>,
            "metric-name"
        ) as TextBoxComponent
        metricNameTextBox.text(source.metric)
        metricNameTextBox.onChanged().subscribe {
            source.metric = it
        }
    }
}