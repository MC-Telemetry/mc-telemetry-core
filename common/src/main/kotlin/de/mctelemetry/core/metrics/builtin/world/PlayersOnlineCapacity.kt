package de.mctelemetry.core.metrics.builtin.world

import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import de.mctelemetry.core.api.metrics.managar.gaugeWorldInstrument
import net.minecraft.server.MinecraftServer

object PlayersOnlineCapacity: IWorldInstrumentManager.Events.Loading {

    override fun worldInstrumentManagerLoading(manager: IWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument("game.minecraft.server.players.online.capacity") {
            description = "How many players the server is configured to allow at once."
        }.registerWithCallbackOfLong { measurement ->
            measurement.record(server.maxPlayers.toLong())
        }
    }
}
