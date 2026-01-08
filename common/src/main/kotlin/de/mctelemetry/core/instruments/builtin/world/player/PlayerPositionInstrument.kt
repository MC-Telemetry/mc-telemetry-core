package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

abstract class PlayerPositionInstrument protected constructor(dimensionName: String) : PlayerInstrumentBase.Simple(
    name = "game.player.position.${dimensionName.lowercase()}",
    supportsFloating = true,
) {

    override val description: String = "The $dimensionName position of the player."
    override val unit: String = "Blocks"

    context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
    override fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        recorder.observe(getValue(player.position()))
    }

    protected abstract fun getValue(position: Vec3): Double

    object PlayerPositionXInstrument : PlayerPositionInstrument("x") {
        override fun getValue(position: Vec3): Double {
            return position.x
        }
    }

    object PlayerPositionYInstrument : PlayerPositionInstrument("y") {
        override fun getValue(position: Vec3): Double {
            return position.y
        }
    }

    object PlayerPositionZInstrument : PlayerPositionInstrument("z") {
        override fun getValue(position: Vec3): Double {
            return position.z
        }
    }
}
