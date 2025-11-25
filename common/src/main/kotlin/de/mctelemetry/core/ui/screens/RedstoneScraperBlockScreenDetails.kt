package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import de.mctelemetry.core.utils.childByIdOrThrow
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
class RedstoneScraperBlockScreenDetails(
    val parent: Screen,
    val source: DemoObservationSource,
    val metrics: List<DemoMetric>,
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "source-listing-details"
        )
    )
) {

    override fun build(rootComponent: FlowLayout) {
        val backButton = rootComponent.childWidgetByIdOrThrow<ButtonComponent>("back")
        backButton.onPress {
            Minecraft.getInstance().setScreen(parent)
        }

        val observationSourceNameLabel = rootComponent.childByIdOrThrow<LabelComponent>("observation-source-name")
        observationSourceNameLabel.text(buildComponent(source.name) {})

        val metricNameTextBox = rootComponent.childWidgetByIdOrThrow<TextBoxComponent>("metric-name")
        metricNameTextBox.text(source.metric)
        metricNameTextBox.onChanged().subscribe {
            source.metric = it
        }
    }
}
