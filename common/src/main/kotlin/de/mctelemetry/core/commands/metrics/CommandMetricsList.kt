package de.mctelemetry.core.commands.metrics

import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.instruments.manager.server.IWorldMutableInstrumentRegistration
import de.mctelemetry.core.utils.dsl.commands.CommandDSLBuilder.Companion.buildCommand
import de.mctelemetry.core.utils.dsl.commands.invoke
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import de.mctelemetry.core.utils.dsl.components.append
import de.mctelemetry.core.utils.dsl.components.onHoverShowText
import de.mctelemetry.core.utils.dsl.components.style
import de.mctelemetry.core.utils.sendFailureAndThrow
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.MutableComponent
import kotlin.with

class CommandMetricsList internal constructor(
    private val buildContext: CommandBuildContext,
) {

    val command = buildCommand("list") {
        requires { it.hasPermission(2) }
        executes(::commandMetricsListAll)
        "all" {
            executes(::commandMetricsListAll)
        }
        "game" {
            executes(::commandMetricsListGame)
        }
        "world" {
            executes(::commandMetricsListWorld)
        }
        "custom" {
            executes(::commandMetricsListCustom)
        }
    }

    private fun registrationComponent(definition: IMetricDefinition): MutableComponent {
        return buildComponent {
            +"  - "
            append(definition.name)
            if (definition !is IInstrumentRegistration)
                return@buildComponent
            +" ("
            if (definition is IInstrumentRegistration.Mutable<*>) {
                append("M") {
                    style {
                        onHoverShowText("Mutable")
                    }
                }
                if (definition is ILongInstrumentRegistration.Mutable<*>)
                    append("L") {
                        style {
                            onHoverShowText("Long")
                        }
                    }
                if (definition is IDoubleInstrumentRegistration.Mutable<*>)
                    append("D") {
                        style {
                            onHoverShowText("Double")
                        }
                    }
            } else {
                if (definition is ILongInstrumentRegistration)
                    append("L") {
                        style {
                            onHoverShowText("Long")
                        }
                    }
                if (definition is IDoubleInstrumentRegistration)
                    append("D") {
                        style {
                            onHoverShowText("Double")
                        }
                    }
            }
            if (definition is IWorldMutableInstrumentRegistration<*>) {
                if (definition.persistent) {
                    append("P") {
                        style {
                            onHoverShowText("Persistent")
                        }
                    }
                }
            }
            +")"
            if (definition.attributes.isNotEmpty()) {
                +": "
                var first = true
                for (info in definition.attributes.values) {
                    if (!first)
                        +", "
                    first = false
                    append(attributeInfoComponent(info))
                }
            }
        }
    }

    private fun attributeInfoComponent(info: MappedAttributeKeyInfo<*, *>): MutableComponent {
        return buildComponent(info.baseKey.key) {
            style {
                onHoverShowText(info.type.id.location().toString())
            }
        }
    }

    private fun sendList(
        context: CommandContext<CommandSourceStack>,
        registrations: Sequence<IMetricDefinition>,
        scope: String,
    ): Int =
        with(context) {
            var counter = 0
            registrations.forEach {
                source.sendSuccess({ registrationComponent(it) }, false)
                counter++
            }
            source.sendSuccess({ TranslationKeys.Commands.metricsListSuccess(counter, scope) }, false)
            counter
        }

    fun commandMetricsListAll(context: CommandContext<CommandSourceStack>): Int = with(context) {
        val instrumentManager = source.server.instrumentManager
        if (instrumentManager == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.worldInstrumentManagerMissing())
        }
        return sendList(
            context,
            instrumentManager.findGlobal(),
            "all"
        )
    }

    fun commandMetricsListGame(context: CommandContext<CommandSourceStack>): Int = with(context) {
        val instrumentManager = source.server.instrumentManager
        if (instrumentManager == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.worldInstrumentManagerMissing())
        }
        return sendList(
            context,
            instrumentManager.gameInstruments.findLocal(),
            "game"
        )
    }

    fun commandMetricsListWorld(context: CommandContext<CommandSourceStack>): Int = with(context) {
        val instrumentManager = source.server.instrumentManager
        if (instrumentManager == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.worldInstrumentManagerMissing())
        }
        return sendList(
            context,
            instrumentManager.findLocal().filterNot {
                (it is IWorldMutableInstrumentRegistration<*>) && it.persistent
            },
            "world"
        )
    }

    fun commandMetricsListCustom(context: CommandContext<CommandSourceStack>): Int = with(context) {
        val instrumentManager = source.server.instrumentManager
        if (instrumentManager == null) {
            source.sendFailureAndThrow(TranslationKeys.Errors.worldInstrumentManagerMissing())
        }
        return sendList(
            context,
            instrumentManager.findLocal().filter {
                (it is IWorldMutableInstrumentRegistration<*>) && it.persistent
            },
            "custom"
        )
    }
}
