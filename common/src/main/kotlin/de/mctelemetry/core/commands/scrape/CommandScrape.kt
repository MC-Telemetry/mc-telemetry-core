package de.mctelemetry.core.commands.scrape

import com.mojang.brigadier.tree.CommandNode
import de.mctelemetry.core.exporters.metrics.MetricsAccessor
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.unaryPlus
import net.minecraft.commands.CommandSourceStack

class CommandScrape(
    val metricsAccessor: MetricsAccessor? = MetricsAccessor.INSTANCE,
) {

    companion object {
        const val SCRAPE_ERROR_RESULT_NO_ACCESSOR = -1
        const val SCRAPE_ERROR_RESULT_NO_METRIC = -2
        const val SCRAPE_ERROR_RESULT_NO_DATA = -3
        const val SCRAPE_ERROR_RESULT_BAD_DATA = -4
    }

    val command: CommandNode<CommandSourceStack>
    val subCommandInfo = CommandScrapeInfo(metricsAccessor)
    val subCommandCardinality = CommandScrapeCardinality(metricsAccessor)
    val subCommandValue = CommandScrapeValue(metricsAccessor)

    init {
        command = buildCommand("scrape") {
            requires {
                it.hasPermission(2)
            }
            +subCommandInfo.command
            +subCommandCardinality.command
            +subCommandValue.command
        }
    }
}
