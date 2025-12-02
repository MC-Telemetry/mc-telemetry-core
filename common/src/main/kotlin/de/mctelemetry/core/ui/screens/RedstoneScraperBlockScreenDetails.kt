package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IMappedAttributeKeySet
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.client.IClientInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.network.observations.container.settings.C2SObservationSourceSettingsUpdatePayload
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import de.mctelemetry.core.observations.model.ObservationSourceConfiguration
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.ui.components.AttributeMappingComponent
import de.mctelemetry.core.ui.components.SuggestingTextBoxComponent
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import de.mctelemetry.core.utils.childByIdOrThrow
import dev.architectury.networking.NetworkManager
import io.github.pixix4.kobserve.base.ObservableProperty
import io.github.pixix4.kobserve.property.mapBinding
import io.github.pixix4.kobserve.property.property
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.CommonColors
import java.util.regex.PatternSyntaxException

@Environment(EnvType.CLIENT)
class RedstoneScraperBlockScreenDetails(
    val parent: Screen,
    val globalPos: GlobalPos,
    val sourceState: ObservationSourceState,
    instrumentName: String,
    mapping: ObservationAttributeMapping = sourceState.configuration?.mapping ?: ObservationAttributeMapping.empty(),
    val sourceAttributes: IMappedAttributeKeySet = sourceState.source.attributes
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java, DataSource.asset(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "source-listing-details"
        )
    )
) {

    constructor(
        parent: Screen,
        position: GlobalPos,
        sourceState: ObservationSourceState,
        configuration: ObservationSourceConfiguration? = sourceState.configuration,
        sourceAttributes: IMappedAttributeKeySet = sourceState.source.attributes,
    ) : this(
        parent,
        position,
        sourceState,
        configuration?.instrument?.name.orEmpty(),
        configuration?.mapping ?: ObservationAttributeMapping.empty(),
        sourceAttributes,
    )

    val source: IObservationSource<*, *>
        get() = sourceState.source

    private val instrumentNameObservable: ObservableProperty<String> = property(instrumentName)
    var instrumentName by instrumentNameObservable

    val instrumentObservable = instrumentNameObservable.mapBinding { findMatchingMetric(it) }
    val instrument by instrumentObservable

    val instrumentAttributesObservable = instrumentObservable.mapBinding { it?.attributes }
    val mappingProperty: ObservableProperty<ObservationAttributeMapping> = property(mapping)
    var mapping by mappingProperty

    companion object {

        private fun findMatchingMetric(name: String): IClientInstrumentManager.IClientInstrumentDefinition? {
            return IClientWorldInstrumentManager.clientWorldInstrumentManager!!.findGlobal(name)
        }

        private fun findMatchingMetrics(name: String): List<IClientInstrumentManager.IClientInstrumentDefinition> {
            val searchRegex = if (name.startsWith('~')) {
                try {
                    name.substring(1).toRegex()
                } catch (_: PatternSyntaxException) {
                    return emptyList()
                }
            } else {
                Regex.escape(name).toRegex(RegexOption.IGNORE_CASE)
            }
            return IClientWorldInstrumentManager.clientWorldInstrumentManager!!.findGlobal(
                searchRegex
            ).mapNotNull {
                (searchRegex.find(it.name) ?: return@mapNotNull null) to it
            }.sortedWith(
                compareBy<Pair<MatchResult, IMetricDefinition>> { (match, _) ->
                    match.range.first
                }.thenBy { (_, instrument) ->
                    instrument.name
                }
            ).mapTo(mutableListOf()) { it.second }
        }
    }

    fun makeConfiguration(): ObservationSourceConfiguration {
        return ObservationSourceConfiguration(
            instrument = instrument ?: IInstrumentDefinition.Record(instrumentName),
            mapping = mapping,
        )
    }

    private fun sendToServer(allowDelete: Boolean = true) {
        NetworkManager.sendToServer(
            C2SObservationSourceSettingsUpdatePayload(
                globalPos,
                source,
                makeConfiguration().takeUnless {
                    allowDelete && it.instrument.name.isEmpty()
                })
        )
    }

    private fun deleteFromServer() {
        NetworkManager.sendToServer(
            C2SObservationSourceSettingsUpdatePayload(
                globalPos,
                source,
                null,
            )
        )
    }

    override fun build(rootComponent: FlowLayout) {
        val backButton = rootComponent.childWidgetByIdOrThrow<ButtonComponent>("back")
        backButton.onPress {
            sendToServer(allowDelete = true)
            Minecraft.getInstance().setScreen(parent)
        }

        val observationSourceNameLabel = rootComponent.childByIdOrThrow<LabelComponent>("observation-source-name")
        observationSourceNameLabel.text(TranslationKeys.ObservationSources[source])

        val metricNameTextBox = rootComponent.childWidgetByIdOrThrow<SuggestingTextBoxComponent>("metric-name")
        metricNameTextBox.setMaxLength(OTelCoreModAPI.Limits.INSTRUMENT_NAME_MAX_LENGTH)
        instrumentNameObservable.onChange {
            val it = instrumentName
            if (it == metricNameTextBox.value) return@onChange
            val cursorPosition = metricNameTextBox.cursorPosition
            metricNameTextBox.text(it)
            metricNameTextBox.moveCursorTo(cursorPosition, false)
        }
        fun onMetricNameTextBoxTextChanged(text: String) {
            instrumentName = text

            val candidates = findMatchingMetrics(text)
            val exactMatch = candidates.firstOrNull { it.name == text }
            if (exactMatch != null) {
                metricNameTextBox.setTextColor(TextBoxComponent.DEFAULT_TEXT_COLOR) // exact match found
                metricNameTextBox.updateSuggestions(emptyList())
            } else if (candidates.isNotEmpty()) {
                metricNameTextBox.updateSuggestions(candidates.map { it.name })
                if (metricNameTextBox.suggestionIndex < 0) {
                    metricNameTextBox.suggestionIndex = 0
                }
                metricNameTextBox.setTextColor(CommonColors.YELLOW) // partial matches found
            } else if (Validators.validateOTelName(text) == null) {
                metricNameTextBox.updateSuggestions(emptyList())
                metricNameTextBox.setTextColor(CommonColors.GRAY) // metric not found, but valid
            } else {
                metricNameTextBox.updateSuggestions(emptyList())
                metricNameTextBox.setTextColor(CommonColors.RED) // metric not found and name invalid
            }
        }
        metricNameTextBox.onChanged().subscribe(::onMetricNameTextBoxTextChanged)
        if (metricNameTextBox.value == instrumentName)
            onMetricNameTextBoxTextChanged(instrumentName)
        else
            metricNameTextBox.text(instrumentName)

        val layout = rootComponent.childByIdOrThrow<FlowLayout>("attribute-mapping")
        layout.child(AttributeMappingComponent(sourceAttributes, instrumentAttributesObservable, mappingProperty))
    }
}
