package de.mctelemetry.core.commands.metrics

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.metrics.managar.gaugeWorldInstrument
import de.mctelemetry.core.commands.types.LabelDefinitionArgumentType
import de.mctelemetry.core.commands.types.MetricNameArgumentType
import de.mctelemetry.core.commands.types.getValue
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.argument
import de.mctelemetry.core.utils.dsl.commands.invoke
import de.mctelemetry.core.utils.sendFailureAndThrow
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import kotlin.with

class CommandMetricsCreate internal constructor(
    private val buildContext: CommandBuildContext,
) {

    val command = buildCommand("create") {
        requires { it.hasPermission(2) }
        "gauge" {
            argument("float", BoolArgumentType.bool()) {
                argument("name", MetricNameArgumentType) {
                    executes(::commandMetricsCreateGauge)
                    var labelNode: CommandNode<CommandSourceStack>? = null
                    for (i in 6 downTo 1) {
                        labelNode = Commands.argument("label$i", LabelDefinitionArgumentType(buildContext))!!.apply {
                            executes(::commandMetricsCreateGauge)
                            if (labelNode != null) {
                                then(labelNode)
                            }
                        }.build()
                    }
                    then(labelNode!!)
                }
            }
        }
    }

    fun commandMetricsCreateGauge(context: CommandContext<CommandSourceStack>): Int = with(context) {
        val instrumentManager = source.server.instrumentManager
        if (instrumentManager == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.worldInstrumentManagerMissing())
        }
        val isFloat: Boolean = context.getArgument("float", Boolean::class.java)
        val name: String = context.getValue("name", MetricNameArgumentType)
        val labels: List<MappedAttributeKeyInfo<*, *>> = generateSequence(1) { it + 1 }.map {
            LabelDefinitionArgumentType.get(context, "label$it")
        }.takeWhile { it != null }.requireNoNulls().toList()
        val instrument = instrumentManager.gaugeWorldInstrument(name) {
            persistent = true
            for (label in labels) {
                addAttribute(label)
            }
        }.let {
            if (isFloat) {
                it.registerMutableOfDouble()
            } else {
                it.registerMutableOfLong()
            }
        }
        source.sendSuccess({ TranslationKeys.Commands.metricsCreateSuccess(instrument) }, false)
        1
    }
}
