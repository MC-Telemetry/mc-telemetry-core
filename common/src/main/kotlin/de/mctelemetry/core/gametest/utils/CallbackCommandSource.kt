package de.mctelemetry.core.gametest.utils

import net.minecraft.commands.CommandSource
import net.minecraft.network.chat.Component

class CallbackCommandSource(
    val acceptsFailure: Boolean=true,
    val acceptsSuccess: Boolean=true,
    val shouldInformAdmins: Boolean=false,
    val callback: (Component)->Unit,
) : CommandSource {

    override fun acceptsFailure(): Boolean = acceptsFailure
    override fun acceptsSuccess(): Boolean = acceptsSuccess
    override fun shouldInformAdmins(): Boolean = shouldInformAdmins
    override fun sendSystemMessage(component: Component) {
        callback(component)
    }
}
