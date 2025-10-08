package de.mctelemetry.core.utils.commanddsl

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import java.util.function.Predicate

@CommandDSL
interface ICommandDSLBuilder<S> {

    fun requires(predicate: Predicate<S>)

    var command: Command<S>
    fun executes(command: Command<S>) {
        this.command = command
    }

    fun then(node: CommandNode<S>)
    fun then(builder: ArgumentBuilder<S, *>) = then(builder.build())
    fun then(builder: ArgumentBuilder<S, *>, thenBlock: ICommandDSLBuilder<S>.() -> Unit)
}

fun ICommandDSLBuilder<CommandSourceStack>.literal(
    value: String,
    thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
) = then(Commands.literal(value), thenBlock)

fun ICommandDSLBuilder<CommandSourceStack>.argument(
    name: String,
    type: ArgumentType<*>,
    thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
) = then(Commands.argument(name, type), thenBlock)

context (builder: ICommandDSLBuilder<CommandSourceStack>)
operator fun String.invoke(thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit) {
    builder.literal(this, thenBlock)
}
