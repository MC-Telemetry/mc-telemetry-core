package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.utils.observe
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object PlayerExperienceInstruments {

    object PlayerExperienceLevelInstrument : PlayerInstrumentBase.Simple(
        name = "game.title.minecraft.player.experience.level",
        supportsFloating = true,
    ) {

        override val description: String = "The experience level of the player."

        context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.experienceLevel + player.experienceProgress.toDouble())
        }
    }

    object PlayerTotalExperienceInstrument : PlayerInstrumentBase.Simple(
        name = "game.title.minecraft.player.experience.total",
        supportsFloating = false,
    ) {

        override val description: String = "The current total experience of the player."

        context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.totalExperience)
        }
    }

    object PlayerScoreInstrument : PlayerInstrumentBase.Simple(
        name = "game.player.score",
        supportsFloating = false,
    ) {

        override val description: String = "The score of the player."

        context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.score)
        }
    }
}
