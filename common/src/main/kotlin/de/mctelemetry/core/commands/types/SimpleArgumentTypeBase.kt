package de.mctelemetry.core.commands.types

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext

abstract class SimpleArgumentTypeBase<T> : ArgumentType<T> {

    abstract fun getValue(context: CommandContext<*>, name: String): T
    open fun get(context: CommandContext<*>, name: String): T? = try {
        getValue(context, name)
    } catch (ex: IllegalArgumentException) {
        if (ex.message?.startsWith("No such argument") == true)
            null
        else
            throw ex
    }

    @JvmName("getValue_context")
    context(context: CommandContext<*>)
    fun getValue(name: String): T = getValue(context, name)

    @JvmName("get_context")
    context(context: CommandContext<*>)
    operator fun get(name: String): T? = get(context, name)
}

fun <T> CommandContext<*>.getValue(name: String, type: SimpleArgumentTypeBase<T>): T = type.getValue(this, name)
operator fun <T> CommandContext<*>.get(name: String, type: SimpleArgumentTypeBase<T>): T? = type.get(this, name)
