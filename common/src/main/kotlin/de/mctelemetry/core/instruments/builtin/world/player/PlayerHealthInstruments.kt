package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.utils.observe
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object PlayerHealthInstruments {

    object PlayerHealthInstrument : PlayerInstrumentBase.Simple(
        name = "game.player.health.current",
        supportsFloating = true,
    ) {

        override val description: String = "The current health of the player."

        context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.health.toDouble())
        }
    }

    object PlayerMaxHealthInstrument : PlayerInstrumentBase.Simple(
        name = "game.player.health.max",
        supportsFloating = true,
    ) {

        override val description: String = "The max health of the player."

        context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.maxHealth.toDouble())
        }
    }
}
