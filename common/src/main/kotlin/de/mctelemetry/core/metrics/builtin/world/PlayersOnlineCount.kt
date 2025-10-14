package de.mctelemetry.core.metrics.builtin.world

import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import de.mctelemetry.core.api.metrics.managar.gaugeWorldInstrument
import net.minecraft.server.MinecraftServer

object PlayersOnlineCount: IWorldInstrumentManager.Events.Loading {

    override fun worldInstrumentManagerLoading(manager: IWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument("game.minecraft.server.players.online.count") {
            description = "How many players are currently online."
        }.registerWithCallbackOfLong { measurement ->
            measurement.record(server.playerCount.toLong())
        }
    }
}
