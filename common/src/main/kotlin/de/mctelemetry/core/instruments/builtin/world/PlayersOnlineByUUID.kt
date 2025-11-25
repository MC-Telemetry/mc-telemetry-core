package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.server.MinecraftServer

object PlayersOnlineByUUID: IServerWorldInstrumentManager.Events.Loading {

    private val playerUUIDAttributeKey = AttributeKey.stringKey("uuid")

    override fun worldInstrumentManagerLoading(manager: IServerWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument("game.minecraft.server.players.online.by_uuid") {
            description = "Which players are currently online, by their uuid."
            addAttribute(playerUUIDAttributeKey)
        }.registerWithCallbackOfLong { measurement ->
            for(player in server.playerList.players){
                if(!player.allowsListing()) continue
                measurement.observe(1, Attributes.of(playerUUIDAttributeKey, player.uuid.toString()))
            }
        }
    }
}
