package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.utils.observe
import net.minecraft.server.MinecraftServer

object PlayersOnlineCapacityInstrument: WorldInstrumentBase.Simple(
    name = "game.server.players.capacity",
    supportsFloating = false,
) {

    override val description: String = "How many players the server is configured to allow at once."

    context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer)
    override fun observeWorldSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        recorder.observe(server.maxPlayers)
    }
}
