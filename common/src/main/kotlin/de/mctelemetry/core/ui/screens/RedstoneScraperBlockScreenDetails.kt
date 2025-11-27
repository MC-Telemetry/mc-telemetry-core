package de.mctelemetry.core.ui.screens

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IMappedAttributeKeySet
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.client.IClientInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.network.observations.container.settings.C2SObservationSourceSettingsUpdatePayload
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import de.mctelemetry.core.observations.model.ObservationSourceConfiguration
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.ui.components.SelectBoxComponent
import de.mctelemetry.core.ui.components.SuggestingTextBoxComponent
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.childWidgetByIdOrThrow
import de.mctelemetry.core.utils.childByIdOrThrow
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.getValue
import de.mctelemetry.core.utils.setValue
import dev.architectury.networking.NetworkManager
import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import io.wispforest.owo.util.Observable
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

    private val instrumentNameObservable: Observable<String> = Observable.of(instrumentName)
    var instrumentName by instrumentNameObservable

    val instrumentObservable: Observable<IInstrumentDefinition?> =
        Observable.of(findMatchingMetric(instrumentName))
    var instrument by instrumentObservable

    private var layout: FlowLayout? = null

    init {
        instrumentNameObservable.observe {
            findMatchingMetric(it).let { foundInstrument ->
                if (instrument != foundInstrument)
                    instrument = foundInstrument
            }
        }
        instrumentObservable.observe {
            if (it != null && this.instrumentName != it.name)
                this.instrumentName = it.name

            test()
        }

    }

    val instrumentAttributes: Map<String, MappedAttributeKeyInfo<*, *>>?
        get() = instrument?.attributes
    val mappingObservable: Observable<ObservationAttributeMapping> = Observable.of(mapping)
    var mapping by mappingObservable

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
        instrumentNameObservable.observe {
            if (it == metricNameTextBox.value) return@observe
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
                instrument = exactMatch
            } else if (candidates.isNotEmpty()) {
                metricNameTextBox.updateSuggestions(candidates.map { it.name })
                if (metricNameTextBox.suggestionIndex < 0) {
                    metricNameTextBox.suggestionIndex = 0
                }
                metricNameTextBox.setTextColor(CommonColors.YELLOW) // partial matches found
                instrument = null
            } else if (Validators.validateOTelName(text) == null) {
                metricNameTextBox.updateSuggestions(emptyList())
                metricNameTextBox.setTextColor(CommonColors.GRAY) // metric not found, but valid
                instrument = null
            } else {
                metricNameTextBox.updateSuggestions(emptyList())
                metricNameTextBox.setTextColor(CommonColors.RED) // metric not found and name invalid
                instrument = null
            }
        }
        metricNameTextBox.onChanged().subscribe(::onMetricNameTextBoxTextChanged)
        if (metricNameTextBox.value == instrumentName)
            onMetricNameTextBoxTextChanged(instrumentName)
        else
            metricNameTextBox.text(instrumentName)

        layout = rootComponent.childByIdOrThrow<FlowLayout>("attribute-mapping")
        test()
    }

    fun test() {
        val l = layout ?: return

        val observationSourceAttributes = sourceAttributes.attributeKeys
        val instrumentAttributes = instrumentAttributes?.values?.toList() ?: emptyList()

        l.clearChildren()

        for (observationSourceAttribute in observationSourceAttributes) {
            val row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
            row.verticalAlignment(VerticalAlignment.CENTER)
            row.padding(Insets.of(4))

            val attributeName = Components.label(buildComponent { +observationSourceAttribute.baseKey.key })
            attributeName.horizontalSizing(Sizing.fill(50))
            row.child(attributeName)

            val attributeMapping = SelectBoxComponent(instrumentAttributes, mapping.mapping.firstNotNullOfOrNull { (instrument,source) ->
                if(source == observationSourceAttribute) instrument
                else null
            }) { old, new ->
                if(new != null) {
                    mapping = ObservationAttributeMapping(
                        mapping.mapping + (new to observationSourceAttribute)
                    )
                } else if(old != null) {
                    mapping = ObservationAttributeMapping(
                        mapping.mapping - old
                    )
                }
                println(new)
            }
            attributeMapping.horizontalSizing(Sizing.fill(50))
            row.child(attributeMapping)

            l.child(row)
        }
    }
}
