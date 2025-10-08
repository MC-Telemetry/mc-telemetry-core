package de.mctelemetry.core.utils

import com.mojang.brigadier.ImmutableStringReader
import de.mctelemetry.core.TranslationKeys
import de.mctelemetry.core.commands.CommandExceptions
import de.mctelemetry.core.commands.createWithNullableContext
import net.minecraft.network.chat.MutableComponent

object Validators {

    fun validateMetricName(metricName: String): MutableComponent? {
        if (metricName.isEmpty()) return TranslationKeys.Errors.metricNameEmpty()
        var previousWasSeparator = false
        metricName.forEachIndexed { idx, char ->
            when (char) {
                in 'a'..'z', in '0'..'9' -> {
                    previousWasSeparator = false
                }
                '_', '.' -> {
                    if (previousWasSeparator) {
                        return TranslationKeys.Errors.metricNameDoubleDelimiter()
                    }
                    previousWasSeparator = true
                }
                else -> return TranslationKeys.Errors.metricNameInvalidChar(char, idx)
            }
        }
        if (!metricName.first().isLetter()) return TranslationKeys.Errors.metricNameBadStart()
        if (!metricName.last().isLetterOrDigit()) return TranslationKeys.Errors.metricNameBadEnd()
        return null
    }

    fun requireValidMetricName(metricName: String, context: ImmutableStringReader? = null) {
        if (metricName.isEmpty()) throw CommandExceptions.METRIC_NAME_EMPTY.createWithNullableContext(context)
        var previousWasSeparator = false
        metricName.forEachIndexed { idx, char ->
            when (char) {
                in 'a'..'z', in '0'..'9' -> {
                    previousWasSeparator = false
                }
                '_', '.' -> {
                    if (previousWasSeparator) {
                        throw CommandExceptions.METRIC_NAME_DOUBLE_DELIMITER.createWithNullableContext(context)
                    }
                    previousWasSeparator = true
                }
                else -> throw CommandExceptions.METRIC_NAME_INVALID_CHAR.createWithNullableContext(context, char, idx)
            }
        }
        if (!metricName.first().isLetter()) throw CommandExceptions.METRIC_NAME_BAD_START.createWithNullableContext(context)
        if (!metricName.last().isLetterOrDigit()) throw CommandExceptions.METRIC_NAME_BAD_END.createWithNullableContext(context)
    }
}
