package de.mctelemetry.core.commands.scrape

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.commands.types.LabelStringMapArgumentType
import de.mctelemetry.core.commands.types.MetricNameArgumentType
import de.mctelemetry.core.api.metrics.managar.IMetricsAccessor
import de.mctelemetry.core.metrics.exporters.agent.ObjectMetricReconverter
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder
import de.mctelemetry.core.utils.dsl.commands.argument
import de.mctelemetry.core.utils.dsl.commands.invoke
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.onClickSuggestCommand
import de.mctelemetry.core.utils.dsl.components.style
import de.mctelemetry.core.utils.sendFailureAndThrow
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import kotlin.collections.component1
import kotlin.collections.component2

class CommandScrapeValue(val metricsAccessor: IMetricsAccessor?) {

    val command = CommandDSLBuilder.Companion.buildCommand("value") {
        requires { it.hasPermission(2) }
        "raw" {
            argument("metric", MetricNameArgumentType) {
                requires { it.hasPermission(2) }
                executes(::commandScrapeValueRaw)
                argument("attributes", LabelStringMapArgumentType) {
                    requires { it.hasPermission(2) }
                    executes(::commandScrapeValueRaw)
                    argument("scale", DoubleArgumentType.doubleArg()) {
                        requires { it.hasPermission(2) }
                        executes(::commandScrapeValueRaw)
                    }
                }
            }
        }
        "exact" {
            argument("metric", MetricNameArgumentType) {
                requires { it.hasPermission(2) }
                executes(::commandScrapeValueExact)
                argument("attributes", LabelStringMapArgumentType) {
                    requires { it.hasPermission(2) }
                    executes(::commandScrapeValueExact)
                }
            }
        }
        "matching" {
            argument("metric", MetricNameArgumentType) {
                requires { it.hasPermission(2) }
                executes(::commandScrapeValueMatching)
                argument("attributes", LabelStringMapArgumentType) {
                    requires { it.hasPermission(2) }
                    executes(::commandScrapeValueMatching)
                }
            }
        }
    }

    fun commandScrapeValueRaw(context: CommandContext<CommandSourceStack>): Int = with(context) {
        if (metricsAccessor == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.metricsAccessorMissing())
        }
        val metricNameFilter = MetricNameArgumentType.getValue("metric")
        val labelMap = LabelStringMapArgumentType["attributes"] ?: emptyMap()
        val scale = try {
            DoubleArgumentType.getDouble(context, "scale")
        } catch (ex: IllegalArgumentException) {
            if (ex.message?.startsWith("No such argument") == true)
                1.0
            else
                throw ex
        }
        val scrapeResponse = metricsAccessor.collectDataPointValue(metricNameFilter, labelMap, exact = true)
        if (scrapeResponse == null) {
            source.sendFailureAndThrow(
                TranslationKeys.Commands.metricDatapointNotFound(metricNameFilter, labelMap),
                ::NoSuchElementException
            )
        }
        return when (scrapeResponse) {
            is ObjectMetricReconverter.MetricValueReadback.MetricDoubleValue -> {
                val value = (scrapeResponse.value * scale)
                source.sendSuccess({ Component.literal(value.toString()) }, false)
                value.toInt()
            }
            is ObjectMetricReconverter.MetricValueReadback.MetricLongValue -> {
                val value = (scrapeResponse.value * scale)
                source.sendSuccess({ Component.literal(value.toString()) }, false)
                value.toInt()
            }
            else -> {
                source.sendFailure(
                    TranslationKeys.Errors.metricResponseTypeUnexpected(
                        metricNameFilter,
                        scrapeResponse.type,
                        "(long_value|double_value)"
                    )
                )
                throw IllegalArgumentException("Unexpected metric response type for $metricNameFilter: Got ${scrapeResponse.type} but expected single value")
            }
        }
    }

    private fun scrapeValueDefaultImpl(context: CommandContext<CommandSourceStack>, exact: Boolean): Int =
        with(context) {
            if (metricsAccessor == null) {
                source.sendFailureAndThrow(TranslationKeys.Errors.metricsAccessorMissing())
            }
            val metricNameFilter = MetricNameArgumentType.getValue("metric")
            val labelMap = LabelStringMapArgumentType["attributes"] ?: emptyMap()
            val scale = try {
                DoubleArgumentType.getDouble(context, "scale")
            } catch (ex: IllegalArgumentException) {
                if (ex.message?.startsWith("No such argument") == true)
                    1.0
                else
                    throw ex
            }
            val scrapeResponse = metricsAccessor.collectDataPoint(metricNameFilter, labelMap, exact = exact)
            if (scrapeResponse == null) {
                source.sendFailureAndThrow(
                    TranslationKeys.Commands.metricNameNotFound(metricNameFilter),
                    ::NoSuchElementException
                )
            }
            if (scrapeResponse.data.isEmpty()) {
                source.sendFailureAndThrow(
                    TranslationKeys.Commands.metricDatapointNotFound(metricNameFilter, labelMap),
                    ::NoSuchElementException
                )
            }
            var totalCount = 0
            val totalSum = scrapeResponse.data.values.sumOf { valueReadback ->
                totalCount++
                when (valueReadback) {
                    is ObjectMetricReconverter.MetricValueReadback.MetricDoubleValue -> {
                        (valueReadback.value * scale)
                    }
                    is ObjectMetricReconverter.MetricValueReadback.MetricLongValue -> {
                        (valueReadback.value * scale)
                    }
                    else -> {
                        source.sendFailureAndThrow(
                            TranslationKeys.Errors.metricResponseTypeUnexpected(
                                metricNameFilter,
                                scrapeResponse.type,
                                "(long_value|double_value)"
                            ),
                            ::IllegalArgumentException
                        )
                    }
                }
            }
            source.sendSuccess({
                                   buildComponent {
                                       +TranslationKeys.Commands.scrapeValueSuccess(totalCount, totalSum)
                                       scrapeResponse.data.forEach { (labelMap, valueReadback) ->
                                           append("\n  - ") {
                                               append(labelMap.toString()) {
                                                   style {
                                                       onClickSuggestCommand(
                                                           "/mcotel scrape value raw ${scrapeResponse.name}" +
                                                                   if (labelMap.isEmpty()) ""
                                                                   else labelMap.entries.joinToString(
                                                                       separator = ",",
                                                                       prefix = " "
                                                                   ) {
                                                                       it.key + "=\"" + it.value
                                                                           .replace("\\", "\\\\")
                                                                           .replace("\"", "\\\"") +
                                                                               '\"'
                                                                   }
                                                       )
                                                   }
                                               }
                                               +": "
                                               append(
                                                   when (valueReadback) {
                                                       is ObjectMetricReconverter.MetricValueReadback.MetricDoubleValue -> {
                                                           (valueReadback.value * scale).toString()
                                                       }
                                                       is ObjectMetricReconverter.MetricValueReadback.MetricLongValue -> {
                                                           (valueReadback.value * scale).toString()
                                                       }
                                                       else -> throw IllegalArgumentException("Unexpected value readback, should already have been handled: $valueReadback")
                                                   }
                                               )
                                           }
                                       }
                                   }
                               }, false)
            return totalSum.toInt()
        }

    fun commandScrapeValueExact(context: CommandContext<CommandSourceStack>): Int {
        return scrapeValueDefaultImpl(context, true)
    }

    fun commandScrapeValueMatching(context: CommandContext<CommandSourceStack>): Int {
        return scrapeValueDefaultImpl(context, false)
    }
}
