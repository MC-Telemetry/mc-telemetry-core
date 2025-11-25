package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import io.opentelemetry.api.common.Attributes
import net.minecraft.server.MinecraftServer

object PlayersOnlineCount: IServerWorldInstrumentManager.Events.Loading {

    override fun worldInstrumentManagerLoading(manager: IServerWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument("game.minecraft.server.players.online.count") {
            description = "How many players are currently online."
        }.registerWithCallbackOfLong { measurement ->
            measurement.observe(server.playerCount.toLong(), Attributes.empty())
        }
    }
}
