package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IMappedAttributeKeyType
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.utils.Validators
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.lang.AssertionError

class LabelDefinitionArgumentType(
    private val labelTypeLookup: HolderLookup.RegistryLookup<IMappedAttributeKeyType<*, *>>,
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
            "location${SEPARATOR}mcotelcore:string",
        )

        override fun getValue(
            context: CommandContext<CommandSourceStack>,
            name: String,
        ): MappedAttributeKeyInfo<*, *> {
            return context.getArgument(name, MappedAttributeKeyInfo::class.java)
        }
    }

    override fun getExamples(): Collection<String?>? {
        return Companion.examples
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
