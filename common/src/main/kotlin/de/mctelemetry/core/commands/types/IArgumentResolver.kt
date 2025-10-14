package de.mctelemetry.core.commands.types

import com.mojang.brigadier.context.CommandContext

interface IArgumentResolver<C, out T> {

    fun getValue(context: CommandContext<C>, name: String): T
    fun get(context: CommandContext<C>, name: String): T? = try {
        getValue(context, name)
    } catch (ex: IllegalArgumentException) {
        if (ex.message?.startsWith("No such argument") == true)
            null
        else
            throw ex
    }
}

inline fun <reified T : Any> CommandContext<*>.tryGetValue(name: String): T? = tryGetValue(name, T::class.java)
fun <T : Any> CommandContext<*>.tryGetValue(name: String, clazz: Class<T>): T? {
    return try {
        this.getArgument(name, clazz)
    } catch (ex: IllegalArgumentException) {
        if (ex.message?.startsWith("No such argument") == true)
            null
        else
            throw ex
    }
}

context(context: CommandContext<C>)
fun <C, T> IArgumentResolver<C, T>.getValue(name: String): T = getValue(context, name)

context(context: CommandContext<C>)
operator fun <C, T> IArgumentResolver<C, T>.get(name: String): T? = get(context, name)
fun <T, C> CommandContext<C>.getValue(name: String, type: IArgumentResolver<C, T>): T = type.getValue(this, name)
operator fun <T, C> CommandContext<C>.get(name: String, type: IArgumentResolver<C, T>): T? = type.get(this, name)
