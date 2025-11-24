package de.mctelemetry.core.ui

import de.mctelemetry.core.OTelCoreMod
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

data class DemoObservationSource(
    var name: String,
    var value: String,
    var attributes: List<DemoObservationSourceAttribute>,
    var metric: String = ""
)

data class DemoObservationSourceAttribute(var name: String)
data class DemoMetric(var name: String, var attributes: List<DemoMetricAttribute>)
data class DemoMetricAttribute(var name: String)

@Environment(EnvType.CLIENT)
class RedstoneScraperBlockScreen : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "source-listing"
        )
    )
) {

    val sources = listOf(
        DemoObservationSource("Power 1", "12", listOf()),
        DemoObservationSource("Power 2", "23", listOf()),
        DemoObservationSource("Power 3", "34", listOf()),
        DemoObservationSource("Power 4", "45", listOf())
    )

    val metrics = listOf(
        DemoMetric("srs.redstone.value", listOf()),
        DemoMetric("cae.limestone.output", listOf()),
        DemoMetric("cae.portal.duration", listOf()),
    )

    override fun build(rootComponent: FlowLayout) {
        val list = rootComponent.childById(
            FlowLayout::class.java as Class<Component>,
            "list"
        ) as FlowLayout

        for (row in sources) {
            val template = model.expandTemplate(
                FlowLayout::class.java,
                "list-row@${OTelCoreMod.MOD_ID}:source-listing",
                mapOf("observation-source-name" to row.name, "observation-source-value" to row.value)
            )

            list.child(template)

            val editButton = template.childById(
                ButtonComponent::class.java as Class<Component>,
                "observation-source-edit"
            ) as ButtonComponent
            editButton.onPress {
                Minecraft.getInstance().setScreen(RedstoneScraperBlockScreenDetails(this, row, metrics))
            }
        }
    }
}