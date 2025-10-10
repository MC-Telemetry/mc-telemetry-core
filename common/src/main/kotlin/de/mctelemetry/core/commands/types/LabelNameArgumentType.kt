package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.Validators
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.resources.ResourceLocation

object LabelNameArgumentType : SimpleArgumentTypeBase<String>() {

    val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
        ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "label_name"),
        this,
        SingletonArgumentInfo.contextFree { LabelNameArgumentType }
    )

    val examples = listOf(
        "job",
        "item",
        "jvm.memory.pool.name",
        "jvm.memory.type",
        "jvm.gc.cause"
    )

    override fun parse(reader: StringReader): String {
        return Validators.parseOTelName(reader, stopAtInvalid = true)
    }

    override fun getValue(context: CommandContext<*>, name: String): String {
        return context.getArgument(name, String::class.java)
    }

    override fun getExamples(): Collection<String> {
        return examples
    }
}
