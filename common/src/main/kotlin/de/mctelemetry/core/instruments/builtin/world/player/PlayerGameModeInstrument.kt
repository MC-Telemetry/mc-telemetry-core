package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object PlayerGameModeInstrument : PlayerInstrumentBase.Simple(
    name = "game.title.minecraft.player.gamemode",
    supportsFloating = false,
) {

    override val description: String = "The gamemode of the player."

    context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
    override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        recorder.observe(player.gameMode.gameModeForPlayer.id.toLong())
    }
}
