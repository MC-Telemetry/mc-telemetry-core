package de.mctelemetry.core.utils.commanddsl

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import java.util.function.Predicate

class CommandDSLBuilder(
    val argumentBuilder: ArgumentBuilder<CommandSourceStack, *>,
) : ICommandDSLBuilder<CommandSourceStack> {

    constructor(name: String) : this(Commands.literal(name))

    override fun requires(predicate: Predicate<CommandSourceStack>) {
        argumentBuilder.requires(predicate)
    }

    override var command: Command<CommandSourceStack>
        get() = argumentBuilder.command
        set(value) {
            argumentBuilder.executes(value)
        }

    override fun then(node: CommandNode<CommandSourceStack>) {
        argumentBuilder.then(node)
    }

    override fun then(
        builder: ArgumentBuilder<CommandSourceStack, *>,
        thenBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
    ) {
        then(CommandDSLBuilder(builder).apply(thenBlock).build())
    }

    fun build(): CommandNode<CommandSourceStack> {
        return argumentBuilder.build()
    }

    companion object {

        fun buildCommand(
            name: String,
            builderBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
        ): CommandNode<CommandSourceStack> {
            return CommandDSLBuilder(name).apply(builderBlock).build()
        }

        fun buildCommandBuilder(
            name: String,
            builderBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
        ): LiteralArgumentBuilder<CommandSourceStack> {
            return CommandDSLBuilder(name).apply(builderBlock).argumentBuilder as LiteralArgumentBuilder<CommandSourceStack>
        }
    }
}
