@file:OptIn(ExperimentalContracts::class)

package de.mctelemetry.core.utils

import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


inline fun CommandSourceStack.sendFailureAndThrow(
    component: Component,
    exceptionFactory: (String) -> Exception = ::RuntimeException,
): Nothing {
    contract {
        callsInPlace(exceptionFactory, InvocationKind.EXACTLY_ONCE)
    }
    val sendException = try {
        sendFailure(component)
        null
    } catch (ex2: Exception) {
        ex2
    }
    throw exceptionFactory(component.string).apply {
        if (sendException != null)
            addSuppressed(sendException)
    }
}
