package de.mctelemetry.core.commands

import com.mojang.brigadier.ImmutableStringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.DynamicNCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import de.mctelemetry.core.TranslationKeys

object CommandExceptions {

    val METRIC_NAME_EMPTY = SimpleCommandExceptionType(TranslationKeys.Errors.metricNameEmpty())
    val METRIC_NAME_INVALID_CHAR = Dynamic2CommandExceptionType { a, b ->
        TranslationKeys.Errors.metricNameInvalidChar(a as Char, b as Int)
    }
    val METRIC_NAME_BAD_START = SimpleCommandExceptionType(TranslationKeys.Errors.metricNameBadStart())
    val METRIC_NAME_BAD_END = SimpleCommandExceptionType(TranslationKeys.Errors.metricNameBadEnd())
    val METRIC_NAME_DOUBLE_DELIMITER = SimpleCommandExceptionType(TranslationKeys.Errors.metricNameDoubleDelimiter())
}

fun SimpleCommandExceptionType.createWithNullableContext(reader: ImmutableStringReader?): CommandSyntaxException {
    return if (reader == null) create()
    else createWithContext(reader)
}

fun DynamicCommandExceptionType.createWithNullableContext(
    reader: ImmutableStringReader?,
    arg: Any,
): CommandSyntaxException {
    return if (reader == null) create(arg)
    else createWithContext(reader, arg)
}

fun Dynamic2CommandExceptionType.createWithNullableContext(
    reader: ImmutableStringReader?,
    arg1: Any,
    arg2: Any,
): CommandSyntaxException {
    return if (reader == null) create(arg1, arg2)
    else createWithContext(reader, arg1, arg2)
}

fun Dynamic3CommandExceptionType.createWithNullableContext(
    reader: ImmutableStringReader?,
    arg1: Any,
    arg2: Any,
    arg3: Any,
): CommandSyntaxException {
    return if (reader == null) create(arg1, arg2, arg3)
    else createWithContext(reader, arg1, arg2, arg3)
}

fun Dynamic4CommandExceptionType.createWithNullableContext(
    reader: ImmutableStringReader?,
    arg1: Any,
    arg2: Any,
    arg3: Any,
    arg4: Any,
): CommandSyntaxException {
    return if (reader == null) create(arg1, arg2, arg3, arg4)
    else createWithContext(reader, arg1, arg2, arg3, arg4)
}

fun DynamicNCommandExceptionType.createWithNullableContext(
    reader: ImmutableStringReader?,
    a: Any,
    vararg args: Any,
): CommandSyntaxException {
    return if (reader == null) create(a, *args)
    else createWithContext(reader, a, *args)
}
