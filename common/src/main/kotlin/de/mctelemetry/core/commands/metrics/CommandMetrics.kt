package de.mctelemetry.core.commands.metrics

import com.mojang.brigadier.tree.CommandNode
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.unaryPlus
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack

class CommandMetrics internal constructor(
    buildContext: CommandBuildContext,
) {

    val command: CommandNode<CommandSourceStack>
    val subCommandCreate = CommandMetricsCreate(buildContext)
    val subCommandList = CommandMetricsList(buildContext)
    val subCommandDelete = CommandMetricsDelete(buildContext)

    init {
        command = buildCommand("metrics") {
            requires {
                it.hasPermission(2)
            }
            +subCommandCreate.command
            +subCommandList.command
            +subCommandDelete.command
        }
    }
}
