package de.mctelemetry.core.ui.datacomponents

import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.MappedAttributeKeyMap
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.StringType.convertValueToString
import de.mctelemetry.core.commands.scrape.CommandScrapeCardinality
import de.mctelemetry.core.network.observations.container.observationrequest.RecordedObservationPoint
import de.mctelemetry.core.network.observations.container.observationrequest.RecordedObservations
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.onHoverShowText
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

    private fun updateComponentValue(value: RecordedObservations?) {
        if (value == null) {
            label.text(pendingValueComponent)
        } else {
            val (_, observationMap) = value
            val cardinalities =
                CommandScrapeCardinality.analyzeCardinality(observationMap.values.map { it.attributes.map })
            if (cardinalities == null) {
                label.text(noValueComponent)
            } else {
                // analyzeCardinality returns non-null -> observationMap not empty
                val cardinalitiesWithoutCommon = cardinalities.filterValues { it > 1 }
                if (cardinalitiesWithoutCommon.isEmpty()) {
                    label.text(directComponentForValue(observationMap.values.first()))
                } else {
                    label.text(
                        componentForMultipleValues(
                            observationMap.values,
                            cardinalities,
                            cardinalitiesWithoutCommon,
                        )
                    )
                }
            }
        }
    }

    companion object {

        private val pendingValueComponent = IComponentDSLBuilder.buildComponent("???") {
            style {
                isObfuscated = true
                onHoverShowText("Pending observation-data") //TODO: use translatable component
            }
        }

        private val noValueComponent = IComponentDSLBuilder.buildComponent("∅") {
            style {
                onHoverShowText("No observations") //TODO: use translatable component
            }
        }

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
                        append(String.format("%.2f",value.doubleValue))
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

        private fun directComponentForValue(value: RecordedObservationPoint): Component {
            return IComponentDSLBuilder.buildComponent {
                append(valueComponent(value, details = false))
                style {
                    onHoverShowText {
                        append(valueComponent(value, details = true))
                        append(attributeMapListing(value.attributes))
                    }
                }
            }
        }

        private fun componentForMultipleValues(
            values: Collection<RecordedObservationPoint>,
            cardinalities: Map<MappedAttributeKeyInfo<*, *>, Int>,
            cardinalitiesWithoutCommon: Map<MappedAttributeKeyInfo<*, *>, Int>,
            tooltipValuePointLimit: Int = 10,
        ): Component {
            return IComponentDSLBuilder.buildComponent {
                append("[")
                for ((idx, cardinality) in cardinalities.values.withIndex()) {
                    if (idx != 0) {
                        append("⨉")
                    }
                    append(cardinality.toString())
                }
                append("]")
                style {
                    onHoverShowText {
                        val commonAttributeKeys: Set<MappedAttributeKeyInfo<*, *>>
                        if (cardinalitiesWithoutCommon.size != cardinalities.size) {
                            val commonAttributeValues: MappedAttributeKeyMap<*> = MappedAttributeKeyMap(
                                values.first().attributes.filter { it.info !in cardinalitiesWithoutCommon }
                            )
                            commonAttributeKeys = commonAttributeValues.keys
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
                }
            }
        }
    }
}
