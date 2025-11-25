package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import io.opentelemetry.api.common.Attributes
import net.minecraft.server.MinecraftServer

object PlayersOnlineCapacity: IServerWorldInstrumentManager.Events.Loading {

    override fun worldInstrumentManagerLoading(manager: IServerWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument("game.minecraft.server.players.online.capacity") {
            description = "How many players the server is configured to allow at once."
        }.registerWithCallbackOfLong { measurement ->
            measurement.observe(server.maxPlayers.toLong(), Attributes.empty())
        }
    }
}
