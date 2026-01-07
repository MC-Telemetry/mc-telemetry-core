package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.ScoreHolder

object PlayersOnlineScoreboard : IServerWorldInstrumentManager.Events.Loading {

    private val playerUUIDAttributeKey = AttributeKey.stringKey("uuid")
    private val objectiveNameAttributeKey = AttributeKey.stringKey("objective")

    override fun worldInstrumentManagerLoading(manager: IServerWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument("game.minecraft.scoreboard.objectives.by_player") {
            description = "The scoreboard values for each tracked objective for each player currently online."
        }.registerWithCallbackOfLong {
            for (player in server.playerList.players) {
                if (!player.allowsListing()) continue
                for (entry in server.scoreboard.listPlayerScores(player).object2IntEntrySet()) {
                    it.observe(
                        entry.intValue.toLong(),
                        Attributes.of(
                            playerUUIDAttributeKey,
                            player.stringUUID,
                            objectiveNameAttributeKey,
                            entry.key.name,
                        ),
                    )
                }
            }
        }
    }
}
