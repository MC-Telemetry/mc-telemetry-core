package de.mctelemetry.core.commands.types

import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.commands.CommandExceptions
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.StringRepresentable
import java.util.concurrent.CompletableFuture

class EnumArgumentType<E : Enum<E>>(
    private val clazz: Class<E>,
) : SimpleArgumentTypeBase<E>() {

    private val clazzPath = clazz.canonicalName
        ?: throw IllegalArgumentException("$clazz does not have a canonical name")

    init {
        require(clazzPath in allowedClassNames) { "Enum class not whitelisted: $clazzPath" }
    }

    private val entries = clazz.enumConstants ?: throw IllegalArgumentException("$clazz does not have a enum constants")

    private val nameAccessor: (E) -> String =
        if (StringRepresentable::class.java.isAssignableFrom(clazz))
            ::stringRepresentableAccessor
        else
            ::enumNameAccessor

    companion object {

        private fun <E : Enum<E>> enumNameAccessor(element: E): String = element.name
        private fun <E : Enum<E>> stringRepresentableAccessor(element: E): String =
            (element as StringRepresentable).serializedName

        private val allowedClassNames: MutableSet<String> = mutableSetOf()

        fun whitelistEnumClass(clazz: Class<Enum<*>>) {
            allowedClassNames.add(
                clazz.canonicalName ?: throw IllegalArgumentException("$clazz does not have a canonical name")
            )
        }

        val registration: ArgumentTypes.PreparedArgumentTypeRegistration<EnumArgumentType<*>, *, *> =
            ArgumentTypes.PreparedArgumentTypeRegistration(
                ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "enum"),
                Info
            )

        private object Info : ArgumentTypeInfo<EnumArgumentType<*>, Template> {

            override fun serializeToNetwork(
                template: Template,
                friendlyByteBuf: FriendlyByteBuf,
            ) {
                friendlyByteBuf.writeUtf(template.clazzPath)
            }

            override fun deserializeFromNetwork(friendlyByteBuf: FriendlyByteBuf): Template {
                val clazzPath = friendlyByteBuf.readUtf()
                require(clazzPath in allowedClassNames) {
                    "Received class is not whitelisted: $clazzPath"
                }
                val clazz = Class.forName(clazzPath)
                require(clazz.isEnum) {
                    "Received class is not an enum: $clazz"
                }
                @Suppress("UNCHECKED_CAST") // cast checked with Class.isEnum
                return Template(clazzPath, clazz as Class<Enum<*>>)
            }

            override fun serializeToJson(
                template: Template,
                jsonObject: JsonObject,
            ) {
                jsonObject.addProperty("class", template.clazzPath)
            }

            override fun unpack(argumentType: EnumArgumentType<*>): Template {
                @Suppress("UNCHECKED_CAST") // cast checked by requiring enumConstants to be exported during init
                return Template(argumentType.clazzPath, argumentType.clazz as Class<Enum<*>>)
            }
        }

        private class Template(
            val clazzPath: String,
            val clazz: Class<Enum<*>>,
        ) : ArgumentTypeInfo.Template<EnumArgumentType<*>> {

            override fun instantiate(commandBuildContext: CommandBuildContext): EnumArgumentType<*> {
                return EnumArgumentType(clazz as Class<out Enum<*>>)
            }

            override fun type(): ArgumentTypeInfo<EnumArgumentType<*>, *> {
                return Info
            }
        }

        fun <E : Enum<E>> CommandContext<*>.getValue(name: String, clazz: Class<E>): E = getArgument(name, clazz)
        operator fun <E : Enum<E>> CommandContext<*>.get(name: String, clazz: Class<E>): E? = tryGetValue(name, clazz)

        inline fun <reified E : Enum<E>> CommandContext<*>.getValue(name: String): E = getValue(name, E::class.java)
        inline operator fun <reified E : Enum<E>> CommandContext<*>.get(name: String): E? = get(name, E::class.java)
    }


    override fun parse(reader: StringReader): E {
        val name = reader.readUnquotedString()
        return entries.find { nameAccessor(it).equals(name, true) }
            ?: throw CommandExceptions.enumValueNotFound<InstrumentExportType>(name, reader)
    }

    override fun getValue(
        context: CommandContext<CommandSourceStack>,
        name: String,
    ): E {
        return context.getArgument(name, clazz)
    }

    override fun getExamples(): Collection<String> {
        return entries.take(5).map(nameAccessor)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        return SharedSuggestionProvider.suggest(entries.map(nameAccessor), builder)
    }
}
