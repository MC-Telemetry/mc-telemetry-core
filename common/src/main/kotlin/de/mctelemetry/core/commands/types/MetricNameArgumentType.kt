package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import de.mctelemetry.core.utils.Validators
import net.minecraft.commands.synchronization.SingletonArgumentInfo

object MetricNameArgumentType : SimpleArgumentTypeBase<String>() {

    val Info: SingletonArgumentInfo<MetricNameArgumentType> = SingletonArgumentInfo.contextFree { MetricNameArgumentType }

    val EXAMPLES = listOf<String>()

    override fun parse(reader: StringReader): String {
        val metricName = reader.readUnquotedString()
        Validators.requireValidMetricName(metricName)
        return metricName
    }

    override fun getExamples(): Collection<String> {
        return EXAMPLES
    }

    override fun getValue(context: CommandContext<*>, name: String): String {
        return context.getArgument(name, String::class.java)
    }
}
