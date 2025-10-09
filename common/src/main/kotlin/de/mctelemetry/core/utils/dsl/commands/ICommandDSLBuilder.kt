package de.mctelemetry.core.utils.dsl.commands

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
}

@CommandDSL
inline fun ICommandDSLBuilder<CommandSourceStack>.then(
    builder: ArgumentBuilder<CommandSourceStack, *>,
    thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
) {
    then(CommandDSLBuilder(builder).apply(thenBlock).build())
}

@CommandDSL
inline fun ICommandDSLBuilder<CommandSourceStack>.literal(
    value: String,
    thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
) = then(Commands.literal(value), thenBlock)


@CommandDSL
inline fun ICommandDSLBuilder<CommandSourceStack>.argument(
    name: String,
    type: ArgumentType<*>,
    thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
) = then(Commands.argument(name, type), thenBlock)

@CommandDSL
context (builder: ICommandDSLBuilder<CommandSourceStack>)
inline operator fun String.invoke(thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit) {
    builder.literal(this, thenBlock)
}

@CommandDSL
context (builder: ICommandDSLBuilder<S>)
operator fun <S> CommandNode<S>.unaryPlus() {
    builder.then(this)
}
