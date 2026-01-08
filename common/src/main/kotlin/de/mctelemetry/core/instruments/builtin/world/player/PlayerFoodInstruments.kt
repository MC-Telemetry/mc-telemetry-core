package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.utils.observe
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object PlayerFoodInstruments {

    object PlayerFoodLevelInstrument : PlayerInstrumentBase.Simple(
        name = "game.title.minecraft.player.food.food",
        supportsFloating = false,
    ) {

        override val description: String = "The food level of the player."

        context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.foodData.foodLevel)
        }
    }

    object PlayerSaturationInstrument : PlayerInstrumentBase.Simple(
        name = "game.title.minecraft.player.food.saturation",
        supportsFloating = true,
    ) {

        override val description: String = "The saturation level of the player."

        context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.foodData.saturationLevel.toDouble())
        }
    }

    object PlayerExhaustionInstrument : PlayerInstrumentBase.Simple(
        name = "game.title.minecraft.player.food.exhaustion",
        supportsFloating = true,
    ) {

        override val description: String = "The exhaustion level of the player."

        context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
        override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
            recorder.observe(player.foodData.exhaustionLevel.toDouble())
        }
    }
}
