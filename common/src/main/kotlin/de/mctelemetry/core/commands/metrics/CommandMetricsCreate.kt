package de.mctelemetry.core.commands.metrics

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import de.mctelemetry.core.commands.types.EnumArgumentType
import de.mctelemetry.core.commands.types.EnumArgumentType.Companion.getValue
import de.mctelemetry.core.commands.types.InstrumentExportType
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

class CommandMetricsCreate internal constructor(
    private val buildContext: CommandBuildContext,
) {

    val command = buildCommand("data/create") {
        requires { it.hasPermission(2) }
        "gauge" {
            argument("type", EnumArgumentType(InstrumentExportType::class.java)) {
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
        val exportType: InstrumentExportType = context.getValue("type")
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
            when(exportType){
                InstrumentExportType.LONG -> it.registerMutableOfLong()
                InstrumentExportType.DOUBLE -> it.registerMutableOfDouble()
            }
        }
        source.sendSuccess({ TranslationKeys.Commands.metricsCreateSuccess(instrument) }, false)
        1
    }
}
