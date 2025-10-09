package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.OTelCoreMod
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.resources.ResourceLocation

object LabelValueArgumentType : SimpleArgumentTypeBase<String>() {

    val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
        ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "label_value"),
        this,
        SingletonArgumentInfo.contextFree { LabelValueArgumentType }
    )

    val examples = listOf(
        "",
        "\"\"",
        "minecraft:stick",
        "blocked",
        "false",
        "\" \""
    )

    override fun parse(reader: StringReader): String {
        if (!reader.canRead()) return ""
        val firstChar = reader.peek()
        when {
            firstChar == ',' -> return "".also { reader.skip() }
            StringReader.isQuotedStringStart(firstChar) -> {
                return reader.readQuotedString()
            }
        }
        val start = reader.cursor
        while (reader.canRead()) {
            val char = reader.read()
            if (char == ',' || Character.isWhitespace(char) || char.isISOControl()) {
                reader.cursor--
                break
            }
        }
        return reader.string.substring(start, reader.cursor)
    }

    override fun getExamples(): Collection<String> {
        return examples
    }

    override fun getValue(context: CommandContext<*>, name: String): String {
        return context.getArgument(name, String::class.java)
    }
}
