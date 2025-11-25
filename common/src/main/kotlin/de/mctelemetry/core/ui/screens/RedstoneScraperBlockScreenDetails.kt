package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.manager.client.IClientInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import de.mctelemetry.core.utils.childByIdOrThrow
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.getValue
import de.mctelemetry.core.utils.setValue
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.util.Observable
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.CommonColors

@Environment(EnvType.CLIENT)
class RedstoneScraperBlockScreenDetails(
    val parent: Screen,
    val source: DemoObservationSource,
    instrument: IClientInstrumentManager.IClientInstrumentDefinition? = null,
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "source-listing-details"
        )
    )
) {

    val instrumentObservable: Observable<IClientInstrumentManager.IClientInstrumentDefinition?> =
        Observable.of(instrument)
    var instrument by instrumentObservable
    private val instrumentAttributesObservable: Observable<Map<String, MappedAttributeKeyInfo<*, *>>?> =
        Observable.of(instrument?.attributes)
    var instrumentAttributes by instrumentAttributesObservable

    init {
        instrumentObservable.observe {
            instrumentAttributes = it?.attributes
        }
    }

    companion object {

        private fun findMatchingMetrics(name: String): List<IClientInstrumentManager.IClientInstrumentDefinition> {
            return if (name.startsWith('~')) {
                IClientWorldInstrumentManager.clientWorldInstrumentManager!!.findGlobal(
                    name.substring(1).toRegex()
                ).toList()
            } else {
                val literalRegex = Regex.escape(name).toRegex(RegexOption.IGNORE_CASE)
                IClientWorldInstrumentManager.clientWorldInstrumentManager!!.findGlobal(
                    (Regex.escape(name) + ".*").toRegex(RegexOption.IGNORE_CASE)
                ).mapNotNull {
                    (literalRegex.find(it.name) ?: return@mapNotNull null) to it
                }.sortedBy { it.first.range.first }.mapTo(mutableListOf()) { it.second }
            }
        }
    }

    override fun build(rootComponent: FlowLayout) {
        val backButton = rootComponent.childWidgetByIdOrThrow<ButtonComponent>("back")
        backButton.onPress {
            Minecraft.getInstance().setScreen(parent)
        }

        val observationSourceNameLabel = rootComponent.childByIdOrThrow<LabelComponent>("observation-source-name")
        observationSourceNameLabel.text(buildComponent(source.name) {})

        val metricNameTextBox = rootComponent.childWidgetByIdOrThrow<TextBoxComponent>("metric-name")
        metricNameTextBox.text(instrument?.name ?: source.metric)
        metricNameTextBox.onChanged().subscribe { text ->
            source.metric = text
            val candidates = findMatchingMetrics(text)
            val exactMatch = candidates.firstOrNull { it.name == text }
            if (exactMatch != null) {
                metricNameTextBox.setTextColor(14737632) // Default color, exact match found
                instrument = exactMatch
            } else if (candidates.isNotEmpty()) {
                metricNameTextBox.setTextColor(CommonColors.YELLOW) // partial matches found
                instrument = null
            } else if (Validators.validateOTelName(text) == null) {
                metricNameTextBox.setTextColor(CommonColors.GRAY) // metric not found, but valid
                instrument = null
            } else {
                metricNameTextBox.setTextColor(CommonColors.RED) // metric not found and name invalid
                instrument = null
            }
        }
    }
}
