package de.mctelemetry.core.commands.metrics

import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.commands.types.CompletingInstrumentArgumentType
import de.mctelemetry.core.commands.types.getValue
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.argument
import de.mctelemetry.core.utils.sendFailureAndThrow
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack

class CommandMetricsDelete internal constructor(
    private val buildContext: CommandBuildContext,
) {

    val command = buildCommand("delete") {
        requires { it.hasPermission(2) }
        argument("name", CompletingInstrumentArgumentType.forWorld(null, persistentFilter = true)) {
            executes(::commandMetricsDelete)
        }
    }

    fun commandMetricsDelete(context: CommandContext<CommandSourceStack>): Int = with(context) {
        val instrumentManager = source.server.instrumentManager
            ?: source.sendFailureAndThrow(TranslationKeys.Errors.worldInstrumentManagerMissing())
        val name: String = context.getValue("name", CompletingInstrumentArgumentType)
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
