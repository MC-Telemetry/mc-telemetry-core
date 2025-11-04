@file:OptIn(ExperimentalContracts::class)

package de.mctelemetry.core.gametest.utils

import com.mojang.brigadier.context.ContextChain
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.CommandResultCallback
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.execution.ExecutionContext
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.optionals.getOrElse

val GameTestHelper.server: MinecraftServer get() = this.level.server

data class GameTestHelperCommandResult(
    val messages: List<Component>,
    val results: List<CommandResultCallbackResult>,
)

data class CommandResultCallbackResult(
    val success: Boolean,
    val code: Int,
)

@Suppress("LEAKED_IN_PLACE_LAMBDA")
fun GameTestHelper.runCommand(
    command: String,
    permissionLevel: Int = 2,
    requiredSuccess: Boolean? = null,
    messageCallback: (Component) -> Unit = {},
    resultCallback: CommandResultCallback,
) {
    contract {
        callsInPlace(messageCallback, InvocationKind.UNKNOWN)
    }
    val server = server
    var resultCount = 0
    val context: CommandSourceStack = CommandSourceStack(
        CallbackCommandSource(callback = messageCallback),
        this.absoluteVec(Vec3.ZERO),
        Vec2.ZERO,
        level,
        permissionLevel,
        "GameTest",
        Component.literal("GameTest"),
        server,
        null
    ).let { stack ->
        when (requiredSuccess) {
            null -> stack.withCallback(resultCallback)
            false -> stack.withCallback { success, result ->
                resultCount++
                assertValueEqual(resultCount, 1, "results.size")
                assertFalse(success, "Command should fail but succeeded instead: $result")
                resultCallback.onResult(success, result)
            }
            true -> stack.withCallback { success, result ->
                resultCount++
                assertValueEqual(resultCount, 1, "results.size")
                assertTrue(success, "Command should succeed but failed instead: $result")
                resultCallback.onResult(success, result)
            }
        }
    }
    val parseResult = server.commands.dispatcher.parse(command, context)
    Commands.validateParseResults(parseResult)
    val commandContext = parseResult.context.build(command)
    val commandContextChain = ContextChain.tryFlatten(commandContext).getOrElse {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
            .dispatcherUnknownCommand()
            .createWithContext(parseResult.reader)
    }
    Commands.executeCommandInContext(context) { execContext ->
        ExecutionContext.queueInitialCommandExecution(
            execContext,
            command,
            commandContextChain,
            context,
            CommandResultCallback.EMPTY,
        )
    }
    if (requiredSuccess != null) {
        assertValueEqual(resultCount, 1, "results.size")
    }
}

fun GameTestHelper.runCommand(
    command: String,
    permissionLevel: Int = 2,
    requiredSuccess: Boolean? = null,
): GameTestHelperCommandResult {
    val messages: MutableList<Component> = mutableListOf()
    val results: MutableList<CommandResultCallbackResult> = mutableListOf()
    runCommand(
        command,
        permissionLevel,
        requiredSuccess = requiredSuccess,
        messageCallback = messages::add
    ) { success, result ->
        results.add(CommandResultCallbackResult(success, result))
    }
    return GameTestHelperCommandResult(messages, results)
}

fun GameTestHelper.assertCommandCannotParse(
    command: String,
    permissionLevel: Int,
) {
    val server = server
    val context = CommandSourceStack(
        CommandSource.NULL,
        this.absoluteVec(Vec3.ZERO),
        Vec2.ZERO,
        level,
        permissionLevel,
        "GameTest",
        Component.literal("GameTest"),
        server,
        null
    )
    val parseResult = server.commands.dispatcher.parse(command, context)
    val parseException = Commands.getParseException(parseResult)
    if (parseException != null) {
        return
    }
    val commandContext = parseResult.context.build(command)
    if (ContextChain.tryFlatten(commandContext).isPresent) {
        return
    }
    failC("Command did not fail to parse")
}

inline fun GameTestHelper.assertThrows(block: () -> Unit): Exception {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return assertThrows<Exception>(block)
}

@JvmName("assertThrowsType")
inline fun <reified E : Exception> GameTestHelper.assertThrows(block: () -> Unit): E {
    @Suppress("WRONG_INVOCATION_KIND")
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    try {
        block()
    } catch (e: Exception) {
        if (e is E)
            return e
        else
            throw e
    }
    if (E::class.java != Exception::class.java) {
        failC("Expected an exception of type ${E::class.java.name} to be thrown but completed successfully")
    } else {
        failC("Expected an exception to be thrown but completed successfully")
    }
}
