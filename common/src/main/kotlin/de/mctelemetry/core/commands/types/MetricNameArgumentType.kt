package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.utils.Validators
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.resources.ResourceLocation

object MetricNameArgumentType : SimpleArgumentTypeBase<String>() {


    val registration = ArgumentTypes.PreparedArgumentTypeRegistration(
        ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "metric_name"),
        this,
        SingletonArgumentInfo.contextFree { MetricNameArgumentType },
    )

    val examples = listOf(
        "jvm.memory.used",
        "jvm.gc.duration",
        "jvm.class.loaded"
    )

    override fun parse(reader: StringReader): String {
        return Validators.parseOTelName(reader, stopAtInvalid = true)
    }

    override fun getExamples(): Collection<String> {
        return examples
    }

    override fun getValue(context: CommandContext<*>, name: String): String {
        return context.getArgument(name, String::class.java)
    }
}
