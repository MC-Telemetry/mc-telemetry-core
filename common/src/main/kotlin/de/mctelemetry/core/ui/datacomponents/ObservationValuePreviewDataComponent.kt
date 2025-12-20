package de.mctelemetry.core.ui.datacomponents

import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.MappedAttributeKeyMap
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.StringType.convertValueToString
import de.mctelemetry.core.commands.scrape.CommandScrapeCardinality
import de.mctelemetry.core.network.observations.container.observationrequest.RecordedObservationPoint
import de.mctelemetry.core.network.observations.container.observationrequest.RecordedObservations
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.style
import io.wispforest.owo.ui.component.LabelComponent
import net.minecraft.network.chat.Component

class ObservationValuePreviewDataComponent(
    private val label: LabelComponent,
) {

    var value: RecordedObservations? = null
        set(value) {
            if (field === value) return
            field = value
            updateComponentValue(value)
        }

    init {
        updateComponentValue(null)
    }

    private data class TextTooltipPair(val textComponent: Component, val tooltipComponent: Component? = null)

    private fun updateComponentValue(value: RecordedObservations?) {
        val (textComponent, tooltipComponent) = if (value == null) {
            pendingValueComponentPair
        } else {
            val (_, observationMap) = value
            val cardinalities =
                CommandScrapeCardinality.analyzeCardinality(observationMap.values.map { it.attributes.map })
                    ?.mapKeys { it.key.info }
            if (cardinalities == null) {
                noValueComponentPair
            } else {
                // analyzeCardinality returns non-null -> observationMap not empty
                val cardinalitiesWithoutCommon = cardinalities.filterValues { it > 1 }
                if (cardinalitiesWithoutCommon.isEmpty()) {
                    directComponentForValue(observationMap.values.first())
                } else {
                    componentForMultipleValues(
                        observationMap.values,
                        cardinalities,
                        cardinalitiesWithoutCommon,
                    )
                }
            }
        }
        label.text(textComponent)
        if (tooltipComponent != null) {
            label.tooltip(tooltipComponent)
        } else {
            label.tooltip(emptyList<Component>())
        }
    }

    companion object {

        private val pendingValueComponentPair = TextTooltipPair(
            IComponentDSLBuilder.buildComponent("???") {
                style {
                    isObfuscated = true
                }
            },
            IComponentDSLBuilder.buildComponent("Pending observation-data") {}//TODO: use translatable component
        )

        private val noValueComponentPair = TextTooltipPair(
            IComponentDSLBuilder.buildComponent("∅") {},
            IComponentDSLBuilder.buildComponent("No observations") {}//TODO: use translatable component
        )

        private fun attributeMapListing(
            map: MappedAttributeKeyMap<*>,
            limit: Int = Int.MAX_VALUE,
            initialLinebreak: Boolean = true,
            baseContent: String = "",
        ): Component {
            return IComponentDSLBuilder.buildComponent(baseContent) {
                var nextSeparator: String = if (initialLinebreak) "\n  - " else "  - "
                val totalSize = map.size
                for ((idx, attributeValue) in map.withIndex()) {
                    append(nextSeparator) {
                        nextSeparator = "\n  - "
                        if (idx >= limit) {
                            append("[")
                            append((totalSize - idx).toString())
                            append(" more]") //TODO: use translatable component
                            return@buildComponent
                        }
                        append(attributeValue.info.baseKey.key)
                        append("=")
                        append(attributeValue.convertValueToString()) {
                            style {
                                isItalic = true
                            }
                        }
                    }
                }
            }
        }

        private fun valueComponent(value: RecordedObservationPoint, details: Boolean = true): Component {
            return IComponentDSLBuilder.buildComponent {
                if (details) {
                    if (value.hasDouble) {
                        append(String.format("%.6fd", value.doubleValue))
                        if (value.hasLong)
                            append(" / " + value.longValue.toString() + "L")
                    } else if (value.hasLong) {
                        append(value.longValue.toString() + "L")
                    }
                } else {
                    if (value.hasDouble) {
                        append(String.format("%.2f", value.doubleValue))
                    } else if (value.hasLong) {
                        append(value.longValue.toString())
                    }
                }
            }
        }

        private fun linearAttributeValuesListing(
            values: Collection<RecordedObservationPoint>,
            commonAttributes: Set<MappedAttributeKeyInfo<*, *>>,
            limit: Int = Int.MAX_VALUE,
            initialLinebreak: Boolean = true,
            baseContent: String = "",
        ): Component {
            return IComponentDSLBuilder.buildComponent(baseContent) {
                var nextSeparator: String = if (initialLinebreak) "\n  - " else "  - "
                val totalSize = values.size
                for ((idx, valuePoint) in values.withIndex()) {
                    append(nextSeparator) {
                        nextSeparator = "\n  - "
                        if (idx >= limit) {
                            append("[")
                            append((totalSize - idx).toString())
                            append(" more]") //TODO: use translatable component
                            return@buildComponent
                        }
                        var firstAttributeValue = true
                        append("[")
                        for (attributeValue in valuePoint.attributes) {
                            if (attributeValue.info in commonAttributes) continue
                            if (firstAttributeValue) {
                                firstAttributeValue = false
                            } else {
                                append(", ")
                            }
                            append(attributeValue.info.baseKey.key)
                            append("=")
                            append(attributeValue.convertValueToString()) {
                                style {
                                    isItalic = true
                                }
                            }
                        }
                        append("]: ")
                        append(valueComponent(valuePoint, details = true))
                    }
                }
            }
        }

        private fun directComponentForValue(value: RecordedObservationPoint): TextTooltipPair {
            return TextTooltipPair(
                textComponent = valueComponent(value, details = false),
                tooltipComponent = IComponentDSLBuilder.buildComponent {
                    append(valueComponent(value, details = true))
                    append(attributeMapListing(value.attributes))
                }
            )
        }

        private fun componentForMultipleValues(
            values: Collection<RecordedObservationPoint>,
            cardinalities: Map<MappedAttributeKeyInfo<*, *>, Int>,
            cardinalitiesWithoutCommon: Map<MappedAttributeKeyInfo<*, *>, Int>,
            tooltipValuePointLimit: Int = 10,
        ): TextTooltipPair {
            val textComponent = IComponentDSLBuilder.buildComponent {
                append("[")
                for ((idx, cardinality) in cardinalities.values.withIndex()) {
                    if (idx != 0) {
                        append("⨉")
                    }
                    append(cardinality.toString())
                }
                append("]")
            }
            val tooltipComponent = IComponentDSLBuilder.buildComponent {
                val commonAttributeKeys: Set<MappedAttributeKeyInfo<*, *>>
                if (cardinalitiesWithoutCommon.size != cardinalities.size) {
                    val commonAttributeValues: MappedAttributeKeyMap<*> = MappedAttributeKeyMap(
                        values.first().attributes.filter { it.info !in cardinalitiesWithoutCommon }
                    )
                    commonAttributeKeys = commonAttributeValues.references.mapTo(mutableSetOf()) {it.info}
                    append(
                        attributeMapListing(
                            commonAttributeValues,
                            baseContent = "Common attributes:"
                        )
                    )
                    append("\n")
                } else {
                    commonAttributeKeys = emptySet()
                }
                append(
                    linearAttributeValuesListing(
                        values,
                        commonAttributes = commonAttributeKeys,
                        limit = tooltipValuePointLimit,
                        baseContent = "Values:"
                    )
                )
            }
            return TextTooltipPair(textComponent, tooltipComponent)
        }
    }
}
