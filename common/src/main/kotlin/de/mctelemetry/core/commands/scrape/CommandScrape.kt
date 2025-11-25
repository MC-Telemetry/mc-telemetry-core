package de.mctelemetry.core.commands.scrape

import com.mojang.brigadier.tree.CommandNode
import de.mctelemetry.core.api.instruments.manager.IMetricsAccessor
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.unaryPlus
import net.minecraft.commands.CommandSourceStack

class CommandScrape(
    metricsAccessor: IMetricsAccessor? = null,
) {

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
