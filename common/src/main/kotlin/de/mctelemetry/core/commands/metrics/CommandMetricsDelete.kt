package de.mctelemetry.core.commands.metrics

import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.commands.types.MetricNameArgumentType
import de.mctelemetry.core.commands.types.getValue
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.argument
import de.mctelemetry.core.utils.sendFailureAndThrow
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import kotlin.with

class CommandMetricsDelete internal constructor(
    private val buildContext: CommandBuildContext,
) {

    val command = buildCommand("delete") {
        requires { it.hasPermission(2) }
        argument("name", MetricNameArgumentType) {
            executes(::commandMetricsDelete)
        }
    }

    fun commandMetricsDelete(context: CommandContext<CommandSourceStack>): Int = with(context) {
        val instrumentManager = source.server.instrumentManager
        if (instrumentManager == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.worldInstrumentManagerMissing())
        }
        val name: String = context.getValue("name", MetricNameArgumentType)
        val instrument = instrumentManager.findLocalMutable(name)
        if (instrument == null) {
            source.sendFailureAndThrow(TranslationKeys.Commands.metricNameNotFound(name))
        } else {
            instrument.close()
            source.sendSuccess({ TranslationKeys.Commands.metricsDeleteSuccess(instrument) }, false)
            1
        }
    }
}
