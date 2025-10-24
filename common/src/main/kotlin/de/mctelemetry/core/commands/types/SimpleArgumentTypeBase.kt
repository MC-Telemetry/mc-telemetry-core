package de.mctelemetry.core.commands.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.CommandSourceStack

abstract class SimpleArgumentTypeBase<T> : ArgumentType<T>, IArgumentResolver<CommandSourceStack, T> {
}

fun <T> ArgumentType<T>.parse(text: String): T = parse(StringReader(text))
fun <T : Any> ArgumentType<T>.parseOrNull(text: String): T? = try {
    parse(StringReader(text))
} catch (_: CommandSyntaxException) {
    null
}
