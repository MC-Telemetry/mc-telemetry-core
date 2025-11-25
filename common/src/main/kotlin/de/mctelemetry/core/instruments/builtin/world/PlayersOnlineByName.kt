package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object PlayersOnlineByName : IServerWorldInstrumentManager.Events.Loading {

    private val playerNameAttributeKey = AttributeKey.stringKey("name")

    override fun worldInstrumentManagerLoading(manager: IServerWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument("game.minecraft.server.players.online.by_name") {
            description = "How many players with which name are online."
            addAttribute(playerNameAttributeKey)
        }.registerWithCallbackOfLong { measurement ->
            for ((name, count) in server.playerList.players
                .filter(ServerPlayer::allowsListing)
                .groupingBy { it.name.string }
                .eachCount()
            ) {
                measurement.observe(count.toLong(), Attributes.of(playerNameAttributeKey, name))
            }
        }
    }
}
