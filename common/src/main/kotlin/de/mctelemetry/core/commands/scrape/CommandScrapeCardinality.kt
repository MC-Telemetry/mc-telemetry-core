package de.mctelemetry.core.commands.scrape

import com.google.common.math.LongMath
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.commands.types.MetricNameArgumentType
import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import de.mctelemetry.core.exporters.metrics.ObjectMetricReconverter
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder
import de.mctelemetry.core.utils.dsl.commands.argument
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.onClickSuggestCommand
import de.mctelemetry.core.utils.dsl.components.onHoverShowText
import de.mctelemetry.core.utils.dsl.components.style
import de.mctelemetry.core.utils.sendFailureAndThrow
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.MutableComponent
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.min

class CommandScrapeCardinality(val metricsAccessor: MetricsAccessor?) {

    val command = CommandDSLBuilder.Companion.buildCommand("cardinality") {
        requires { it.hasPermission(2) }
        executes(::commandScrapeCardinality)
        argument("metric", MetricNameArgumentType) {
            requires { it.hasPermission(2) }
            executes(::commandScrapeCardinality)
        }
    }

    companion object {

        fun analyzeCardinality(labelData: Iterable<Map<String, String>>): Map<String, Int>? {
            val values: MutableMap<String, MutableSet<String>> = mutableMapOf()
            var hasData = false
            for (labels in labelData) {
                (values.keys - labels.keys).forEach { missingKey ->
                    values.getValue(missingKey).add("")
                }
                if (hasData) {
                    (labels.keys - values.keys).forEach { newKey ->
                        values.getOrPut(newKey, ::mutableSetOf).add("")
                    }
                }
                labels.forEach { (labelKey, labelValue) ->
                    values.getOrPut(labelKey, ::mutableSetOf).add(labelValue)
                }
                hasData = true
            }

            return if (!hasData) null
            else values.mapValues { it.value.size }
        }
    }

    private fun cardinalityComponent(
        name: String,
        cardinality: Map<String, Int>,
        totalSize: Long,
    ): MutableComponent {
        if (totalSize == 0L) {
            return buildComponent(
                name
            ) {
                +": "
                +"0"
            }
        }
        return buildComponent {
            append(name) {
                style {
                    onClickSuggestCommand("/mcotel scrape value matching $name")
                }
            }
            +"["
            var isFirst = true
            cardinality.forEach { (k, v) ->
                assert(v > 0)
                if (!isFirst) {
                    append("⨉")
                }
                append(v.toString()) {
                    style {
                        onHoverShowText(k)
                    }
                }
                isFirst = false
            }
            +"]: "
            append(totalSize.toString())
        }
    }

    fun commandScrapeCardinality(context: CommandContext<CommandSourceStack>): Int = with(context) {
        if (metricsAccessor == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.metricsAccessorMissing())
        }
        val metricNameFilter = MetricNameArgumentType["metric"]
        val data: Map<String, ObjectMetricReconverter.MetricDataReadback> =
            if (metricNameFilter == null) {
                metricsAccessor.collect()
            } else {
                val result = metricsAccessor.collectNamed(metricNameFilter)
                if (result == null) emptyMap()
                else mapOf(result.name to result)
            }
        val cardinalities: Map<String, Pair<Map<String, Int>, Long>> =
            data.mapValues { (_, value: ObjectMetricReconverter.MetricDataReadback) ->
                val cardinality = analyzeCardinality(value.data.keys)
                if (cardinality == null) return@mapValues emptyMap<String, Int>() to 0L
                return@mapValues cardinality to (cardinality.values.fold(1L) { acc, i ->
                    LongMath.saturatedMultiply(acc, i.toLong())
                })
            }
        val totalSize: Long = cardinalities.values.fold(0L) { acc, value ->
            assert(value.second > 0)
            val sum = acc + value.second
            if (sum < acc)
                Long.MAX_VALUE
            else
                sum
        }
        if (data.isEmpty()) {
            if (metricNameFilter != null) {
                source.sendFailureAndThrow(
                    TranslationKeys.Commands.metricNameNotFound(metricNameFilter),
                    ::NoSuchElementException
                )
            } else {
                source.sendSuccess(TranslationKeys.Commands::noMetrics, false)
                return 0
            }
        } else {
            source.sendSuccess({
                                   buildComponent {
                                       append(
                                           TranslationKeys.Commands.scrapeCardinalitySuccess(
                                               cardinalities.size, totalSize
                                           )
                                       )
                                       for ((name, cardinalityInfo) in cardinalities) {
                                           append {
                                               +"\n  - "
                                               append(
                                                   cardinalityComponent(
                                                       name,
                                                       cardinality = cardinalityInfo.first,
                                                       totalSize = cardinalityInfo.second
                                                   )
                                               )
                                           }
                                       }
                                   }
                               }, false)
            return min(Int.MAX_VALUE.toLong(), totalSize).toInt()
        }
    }
}
