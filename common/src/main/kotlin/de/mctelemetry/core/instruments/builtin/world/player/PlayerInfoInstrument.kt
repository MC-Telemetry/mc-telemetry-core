package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationRecorder
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object PlayerInfoInstrument : PlayerInstrumentBase.Simple(
    name = "game.server.players.online",
    supportsFloating = false,
) {

    override val description = "Which players are currently online."

    private val nameSlot = NativeAttributeKeyTypes.StringType.createAttributeSlot("player.name")

    context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, player: ServerPlayer)
    override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        nameSlot.set(player.name.string)
        recorder.observe(1)
    }
}
