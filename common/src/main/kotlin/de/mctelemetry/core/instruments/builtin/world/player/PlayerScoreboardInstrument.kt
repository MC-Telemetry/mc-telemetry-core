package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.utils.observe
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object PlayerScoreboardInstrument : PlayerInstrumentBase.Simple(
    name = "game.title.minecraft.scoreboard.objectives.by_player",
    supportsFloating = false,
) {
    private val objectiveNameSlot = NativeAttributeKeyTypes.StringType.createAttributeSlot("objective.name")

    override val description = "The scoreboard values for each tracked objective for each player currently online."

    context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, player: ServerPlayer)
    override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        for (entry in server.scoreboard.listPlayerScores(player).object2IntEntrySet()) {
            objectiveNameSlot.set(entry.key.name)
            recorder.observe(entry.intValue)
        }
    }
}
