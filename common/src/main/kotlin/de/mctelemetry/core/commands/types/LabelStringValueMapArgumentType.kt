package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.Validators
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.resources.ResourceLocation

object LabelStringValueMapArgumentType : SimpleArgumentTypeBase<Map<String, String>>() {

    val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
        ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "label_map_value_string"),
        SingletonArgumentInfo.contextFree { LabelStringValueMapArgumentType }
    )

    val examples =
        listOf(
            "",
            "label=",
            "key=,value=❄",
            "item=minecraft:stick",
            "jvm.thread.state=blocked,jvm.thread.daemon=false",
            "char=\" \""
        )

    override fun parse(reader: StringReader): Map<String, String> {
        return buildMap {
            var anchor: Int
            do {
                val name = Validators.parseOTelName(reader, true)
                reader.expect('=')
                val value = LabelValueArgumentType.parse(reader)
                put(name, value)
                anchor = reader.cursor
            } while (reader.canRead() && reader.read() == ',')
            reader.cursor = anchor
        }
    }

    override fun getExamples(): Collection<String> {
        return examples
    }


    override fun getValue(context: CommandContext<CommandSourceStack>, name: String): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        return context.getArgument(name, Map::class.java) as Map<String, String>
    }
}
