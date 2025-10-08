package de.mctelemetry.core.commands.scrape

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.commands.types.MetricNameArgumentType
import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import de.mctelemetry.core.exporters.metrics.ObjectMetricReconverter
import de.mctelemetry.core.utils.commanddsl.CommandDSLBuilder
import de.mctelemetry.core.utils.commanddsl.argument
import de.mctelemetry.core.utils.commanddsl.invoke
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

class CommandScrape(
    val metricsAccessor: MetricsAccessor? = MetricsAccessor.INSTANCE,
) {

    val command: CommandNode<CommandSourceStack>

    init {
        command = CommandDSLBuilder.buildCommand("scrape") {
            requires {
                it.hasPermission(2)
            }
            "info" {
                requires { it.hasPermission(2) }
                executes(::commandScrapeInfo)
                argument("metric", MetricNameArgumentType) {
                    requires { it.hasPermission(2) }
                    executes(::commandScrapeInfo)
                }
            }
            "cardinality" {
                requires { it.hasPermission(2) }
                executes(::commandScrapeCardinality)
                argument("metric", MetricNameArgumentType) {
                    requires { it.hasPermission(2) }
                    executes(::commandScrapeCardinality)
                }
            }
        }
    }

    private fun infoComponent(definition: ObjectMetricReconverter.MetricDefinitionReadback): MutableComponent {
        return Component.literal(definition.name).apply {
            val unit = definition.unit
            if (unit.isNotEmpty()) {
                append(" (")
                append(unit)
                append("): ")
            } else {
                append("") // retain relative position of following components
                append("")
                append(": ")
            }
            append(definition.type)
            if (definition.description.isNotBlank()) {
                append(" # ")
                append(definition.description)
            } else {
                append("")
                append("")
            }
        }
    }

    private fun cardinalityComponent(data: ObjectMetricReconverter.MetricDataReadback): Pair<MutableComponent, Long> {
        if (data.data.isEmpty())
            return Component.literal(data.name).apply {
                append(": ")
                append("0")
            } to 0
        val values: MutableMap<String, MutableSet<String>> = mutableMapOf()

        var isFirstDataPoint = true
        data.data.forEach { labels, _ ->
            (values.keys - labels.keys).forEach { missingKey ->
                values.getValue(missingKey).add("")
            }
            if (!isFirstDataPoint) {
                (labels.keys - values.keys).forEach { newKey ->
                    values.getValue(newKey).add("")
                }
            }
            labels.forEach { (labelKey, labelValue) ->
                values.getOrPut(labelKey, ::mutableSetOf).add(labelValue)
            }
            isFirstDataPoint = false
        }

        val totalSize = values.values.fold(1L) { acc, values -> acc * values.size }
        return Component.literal(data.name).apply {
            append("[")
            var isFirst = true
            values.values.forEach {
                if (!isFirst) {
                    append("⨉")
                }
                append(it.size.toString())
                isFirst = false
            }
            append("]: ")
            append(totalSize.toString())
        } to totalSize
    }

    fun commandScrapeInfo(context: CommandContext<CommandSourceStack>): Int {
        if (metricsAccessor == null) {
            context.source.sendFailure(TranslationKeys.Errors.metricsAccessorMissing())
            return -2
        }
        val metricNameFilter = try {
            MetricNameArgumentType.getMetricName(context, "metric")
        } catch (ex: IllegalArgumentException) {
            if (ex.message?.startsWith("No such argument") == true)
                null
            else
                throw ex
        }
        val definitions: Map<String, ObjectMetricReconverter.MetricDefinitionReadback> =
            if (metricNameFilter == null) {
                metricsAccessor.collectDefinitions()
            } else {
                val result = metricsAccessor.collectDefinition(metricNameFilter)
                if (result == null) emptyMap()
                else mapOf(result.name to result)
            }
        val resultComponentFactory: () -> Component = {
            Component.empty().apply {
                var first = true
                for (definition in definitions.values) {
                    append(Component.empty().apply {
                        if (!first)
                            append("\n")
                        append(infoComponent(definition))
                        first = false
                    })
                }
            }
        }
        if (definitions.isEmpty()) {
            if (metricNameFilter != null) {
                context.source.sendFailure(TranslationKeys.Commands.metricNameNotFound(metricNameFilter))
                return -1
            } else {
                context.source.sendSuccess(TranslationKeys.Commands::noMetrics, false)
                return 0
            }
        } else {
            context.source.sendSuccess(resultComponentFactory, false)
            return definitions.size
        }
    }

    fun commandScrapeCardinality(context: CommandContext<CommandSourceStack>): Int {
        if (metricsAccessor == null) {
            context.source.sendFailure(TranslationKeys.Errors.metricsAccessorMissing())
            return -2
        }
        val metricNameFilter = try {
            MetricNameArgumentType.getMetricName(context, "metric")
        } catch (ex: IllegalArgumentException) {
            if (ex.message?.startsWith("No such argument") == true)
                null
            else
                throw ex
        }
        val data: Map<String, ObjectMetricReconverter.MetricDataReadback> =
            if (metricNameFilter == null) {
                metricsAccessor.collect()
            } else {
                val result = metricsAccessor.collectNamed(metricNameFilter)
                if (result == null) emptyMap()
                else mapOf(result.name to result)
            }
        var cardinalitySum: Long = -1
        val resultComponentFactory: () -> Component = {
            Component.empty().apply {
                    var first = true
                    for (dataEntry in data.values) {
                        append(Component.empty().apply {
                            if (!first)
                                append("\n")
                            val (component, cardinality) = cardinalityComponent(dataEntry)
                            append(component)
                            if (cardinalitySum < 0)
                                cardinalitySum = cardinality
                            else
                                cardinalitySum += cardinality
                            first = false
                        })
                    }
                }
            }
        if (data.isEmpty()) {
            if (metricNameFilter != null) {
                context.source.sendFailure(TranslationKeys.Commands.metricNameNotFound(metricNameFilter))
                return -1
            } else {
                context.source.sendSuccess(TranslationKeys.Commands::noMetrics, false)
                return 0
            }
        } else {
            context.source.sendSuccess(resultComponentFactory, false)
            return cardinalitySum.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
        }
    }
}
