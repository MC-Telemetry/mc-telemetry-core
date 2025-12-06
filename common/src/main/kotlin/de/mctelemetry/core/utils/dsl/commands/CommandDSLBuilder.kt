@file:OptIn(ExperimentalContracts::class)

package de.mctelemetry.core.utils.dsl.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import java.util.function.Predicate
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
internal class CommandDSLBuilder(
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

    fun build(): CommandNode<CommandSourceStack> {
        return argumentBuilder.build()
    }

    companion object {

        @CommandDSL
        inline fun buildCommand(
            name: String,
            builderBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
        ): CommandNode<CommandSourceStack> {
            contract {
                callsInPlace(builderBlock, InvocationKind.EXACTLY_ONCE)
            }
            return CommandDSLBuilder(name).apply(builderBlock).build()
        }

        @CommandDSL
        inline fun buildCommandBuilder(
            name: String,
            builderBlock: ICommandDSLBuilder<CommandSourceStack>.() -> Unit,
        ): LiteralArgumentBuilder<CommandSourceStack> {
            contract {
                callsInPlace(builderBlock, InvocationKind.EXACTLY_ONCE)
            }
            return CommandDSLBuilder(name).apply(builderBlock).argumentBuilder as LiteralArgumentBuilder<CommandSourceStack>
        }
    }
}
