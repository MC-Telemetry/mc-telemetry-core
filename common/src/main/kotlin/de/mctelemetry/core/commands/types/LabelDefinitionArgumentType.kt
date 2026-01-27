package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.utils.Validators
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

class LabelDefinitionArgumentType(
    private val labelTypeLookup: HolderLookup.RegistryLookup<IAttributeKeyTypeTemplate<*, *>>,
) : ArgumentType<MappedAttributeKeyInfo<*, *>> {

    constructor(context: CommandBuildContext) : this(context.lookupOrThrow(OTelCoreModAPI.AttributeTypeMappings))

    companion object : IArgumentResolver<CommandSourceStack, MappedAttributeKeyInfo<*, *>> {

        private const val SEPARATOR: Char = '#'

        val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "label_definition"),
            SingletonArgumentInfo.contextAware(::LabelDefinitionArgumentType),
        )

        private val examples = listOf(
            "name",
            "location${SEPARATOR}minecraft:global_position",
        )

        override fun getValue(
            context: CommandContext<CommandSourceStack>,
            name: String,
        ): MappedAttributeKeyInfo<*, *> {
            return context.getArgument(name, MappedAttributeKeyInfo::class.java)
        }
    }

    override fun getExamples(): Collection<String> {
        return Companion.examples
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remainingInput = builder.remaining
        val separatorIndex = remainingInput.indexOf(SEPARATOR)
        if (separatorIndex > 0) {
            val validationResult = Validators.validateOTelName(remainingInput.substring(0..<separatorIndex))
            if (validationResult == null) {
                // no errors during validation, substring before SEPARATOR is valid OTel name
                val subBuilder = builder.createOffset(builder.start + separatorIndex + 1)
                SharedSuggestionProvider.suggestResource(
                    labelTypeLookup.listElementIds().map(ResourceKey<*>::location),
                    subBuilder
                ) // returns CompletableFuture, but it is directly build from passed subBuilder, so we can just add the subBuilder instead
                builder.add(subBuilder)
            }
        }
        return builder.buildFuture()
    }

    override fun parse(reader: StringReader): MappedAttributeKeyInfo<*, *> {
        val cursor = reader.cursor
        val name = Validators.parseOTelName(reader, stopAtInvalid = true)
        if (!reader.canRead())
            return MappedAttributeKeyInfo.forString(name)
        reader.peek().let {
            if (it == ' ')
                return MappedAttributeKeyInfo.forString(name)
            if (it != SEPARATOR) {
                reader.cursor = cursor
                Validators.parseOTelName(reader, stopAtInvalid = false)
                throw AssertionError("Failed to throw from invalid OTelName")
            }
        }
        reader.skip() // skip SEPARATOR
        val location = ResourceLocation.read(reader)
        val type = labelTypeLookup.getOrThrow(
            ResourceKey.create(
                OTelCoreModAPI.AttributeTypeMappings,
                location,
            )
        ).value()
        if (!reader.canRead())
            return type.create(name, null)
        if (reader.peek() == ' ')
            return type.create(name, null)
        val tag = TagParser(reader).readStruct()
        return type.create(name, tag)
    }
}
