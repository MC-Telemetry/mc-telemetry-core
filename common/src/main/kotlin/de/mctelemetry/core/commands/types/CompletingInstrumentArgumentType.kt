package de.mctelemetry.core.commands.types

import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.definition.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IWorldInstrumentManager
import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import de.mctelemetry.core.instruments.manager.server.ServerInstrumentMetaManager
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture
import kotlin.experimental.and

sealed class CompletingInstrumentArgumentType<T : IMetricDefinition> : ArgumentType<String> {

    private class Global(manager: IInstrumentManager?) : CompletingInstrumentArgumentType<IMetricDefinition>() {

        private val _manager: IInstrumentManager? = manager

        private val manager: IInstrumentManager
            get() = _manager ?: ServerInstrumentMetaManager.current()

        override fun findPartial(name: String): Sequence<IMetricDefinition> {
            return manager.findGlobal("^${Regex.escape(name)}.+$".toRegex(RegexOption.IGNORE_CASE))
        }

        companion object {

            val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
                ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "metric_name.existing.global"),
                SingletonArgumentInfo.contextAware {
                    // only runs in client context
                    Global(ClientInstrumentMetaManager.activeManager ?: IInstrumentManager.ReadonlyEmpty)
                }
            )
        }
    }

    private class Local(manager: IInstrumentManager?) : CompletingInstrumentArgumentType<IInstrumentDefinition>() {

        private val _manager: IInstrumentManager? = manager

        private val manager: IInstrumentManager
            get() = _manager ?: ServerInstrumentMetaManager.current()


        override fun findPartial(name: String): Sequence<IInstrumentDefinition> {
            return manager.findLocal("^${Regex.escape(name)}.+$".toRegex(RegexOption.IGNORE_CASE))
        }

        companion object {

            val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
                ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "metric_name.existing.local"),
                SingletonArgumentInfo.contextAware {
                    // only runs in client context
                    Local(ClientInstrumentMetaManager.activeManager ?: IInstrumentManager.ReadonlyEmpty)
                }
            )
        }
    }


    private class World(manager: IWorldInstrumentManager?, val persistentFilter: Boolean? = null) :
            CompletingInstrumentArgumentType<IWorldInstrumentDefinition>() {

        private val _manager: IWorldInstrumentManager? = manager

        private val manager: IWorldInstrumentManager
            get() = _manager ?: ServerInstrumentMetaManager.current()


        override fun findPartial(name: String): Sequence<IWorldInstrumentDefinition> {
            return manager.findLocal("^${Regex.escape(name)}.+$".toRegex(RegexOption.IGNORE_CASE))
                .let { sequence ->
                    if (persistentFilter == null)
                        sequence
                    else
                        sequence.filter {
                            it.persistent == persistentFilter
                        }
                }
        }

        companion object {

            val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
                ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "metric_name.existing.world"),
                Info,
            )

            private object Info : ArgumentTypeInfo<World, Template> {

                override fun serializeToNetwork(
                    template: Template,
                    friendlyByteBuf: FriendlyByteBuf,
                ) {
                    friendlyByteBuf.writeByte(
                        when (template.persistentFilter) {
                            null -> 0b00
                            false -> 0b10
                            true -> 0b11
                        }
                    )
                }

                override fun deserializeFromNetwork(friendlyByteBuf: FriendlyByteBuf): Template {
                    val flags = friendlyByteBuf.readByte()
                    val persistentFilter = when (val persistentFilterFlags = (flags and 0b11)) {
                        0b00.toByte() -> null
                        0b10.toByte() -> false
                        0b11.toByte() -> true
                        else -> throw IllegalArgumentException("Unexpected persistentFilterFlag ${persistentFilterFlags.toHexString()} (whole flags: ${flags.toHexString()})")
                    }
                    return Template(persistentFilter)
                }

                override fun serializeToJson(
                    template: Template,
                    jsonObject: JsonObject,
                ) {
                    jsonObject.addProperty("persistentFilter", template.persistentFilter)
                }

                override fun unpack(argumentType: World): Template {
                    return Template(argumentType.persistentFilter)
                }
            }

            class Template(val persistentFilter: Boolean? = null) : ArgumentTypeInfo.Template<World> {

                override fun type(): ArgumentTypeInfo<World, *> {
                    return Info
                }

                override fun instantiate(commandBuildContext: CommandBuildContext): World {
                    // only runs in client context
                    return World(
                        ClientInstrumentMetaManager.activeWorldManager ?: IWorldInstrumentManager.ReadonlyEmpty,
                        persistentFilter
                    )
                }
            }
        }
    }

    companion object : IArgumentResolver<CommandSourceStack, String> {

        val registrations: List<ArgumentTypes.PreparedArgumentTypeRegistration<*, *, *>> =
            listOf(
                Global.registration,
                Local.registration,
                World.registration,
            )

        fun forGlobal(manager: IInstrumentManager?): CompletingInstrumentArgumentType<IMetricDefinition> {
            return Global(manager)
        }

        fun forLocal(manager: IInstrumentManager?): CompletingInstrumentArgumentType<IInstrumentDefinition> {
            return Local(manager)
        }

        fun forWorld(
            manager: IWorldInstrumentManager?,
            persistentFilter: Boolean? = null,
        ): CompletingInstrumentArgumentType<IWorldInstrumentDefinition> {
            return World(manager, persistentFilter)
        }

        override fun getValue(context: CommandContext<CommandSourceStack>, name: String): String {
            return context.getArgument(name, String::class.java)
        }
    }

    protected abstract fun findPartial(name: String): Sequence<T>

    override fun parse(reader: StringReader): String {
        return MetricNameArgumentType.parse(reader)
    }

    private fun suggestionTooltip(definition: IMetricDefinition): Component? {
        return IComponentDSLBuilder.buildComponent {
            var modified = false
            if (definition.description.isNotEmpty()) {
                modified = true
                append(definition.description)
            }
            if (!modified) return null
        }
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val name = builder.remainingLowerCase
        for (definition in findPartial(name)) {
            val tooltip = suggestionTooltip(definition)
            if (tooltip != null) {
                builder.suggest(definition.name, tooltip)
            } else {
                builder.suggest(definition.name)
            }
        }
        return builder.buildFuture()
    }
}
