package de.mctelemetry.core.commands.scrape

import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.commands.types.MetricNameArgumentType
import de.mctelemetry.core.api.metrics.managar.IMetricsAccessor
import de.mctelemetry.core.metrics.exporters.agent.ObjectMetricReconverter
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder
import de.mctelemetry.core.utils.dsl.commands.argument
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.onClickSuggestCommand
import de.mctelemetry.core.utils.dsl.components.style
import de.mctelemetry.core.utils.sendFailureAndThrow
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.MutableComponent

class CommandScrapeInfo(val metricsAccessor: IMetricsAccessor?) {

    val command = CommandDSLBuilder.Companion.buildCommand("info") {
        requires { it.hasPermission(2) }
        executes(::commandScrapeInfo)
        argument("metric", MetricNameArgumentType) {
            requires { it.hasPermission(2) }
            executes(::commandScrapeInfo)
        }
    }

    private fun infoComponent(definition: ObjectMetricReconverter.MetricDefinitionReadback): MutableComponent {
        return buildComponent {
            append(definition.name) {
                style {
                    onClickSuggestCommand("/mcotel scrape value matching ${definition.name}")
                }
            }
            val unit = definition.unit
            if (unit.isNotEmpty()) {
                +" ("
                append(unit)
                +"): "
            } else {
                +""
                append("")
                +": "
            }
            append(definition.type)
            if (definition.description.isNotBlank()) {
                +" # "
                append(definition.description)
            } else {
                +""
                append("")
            }
        }
    }

    fun commandScrapeInfo(context: CommandContext<CommandSourceStack>): Int = with(context) {
        if (metricsAccessor == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.metricsAccessorMissing())
        }
        val metricNameFilter: String? = MetricNameArgumentType["metric"]
        val definitions: Map<String, ObjectMetricReconverter.MetricDefinitionReadback> =
            if (metricNameFilter == null) {
                metricsAccessor.collectDefinitions()
            } else {
                val result = metricsAccessor.collectDefinition(metricNameFilter)
                if (result == null) emptyMap()
                else mapOf(result.name to result)
            }
        if (definitions.isEmpty()) {
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
            source.sendSuccess(
                {
                    buildComponent {
                        append {
                            append(TranslationKeys.Commands.scrapeInfoSuccess(definitions.size))
                            for (definition in definitions.values) {
                                append {
                                    +"\n  - "
                                    append(infoComponent(definition))
                                }
                            }
                        }
                    }
                }, false
            )
            return definitions.size
        }
    }
}
