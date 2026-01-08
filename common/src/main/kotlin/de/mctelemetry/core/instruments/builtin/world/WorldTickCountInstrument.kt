package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.utils.observe
import net.minecraft.server.MinecraftServer

object WorldTickCountInstrument : WorldInstrumentBase.Simple(
    name = "game.server.world.tick_count",
    supportsFloating = false,
) {

    override val description: String = "Tick count of the world."

    context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer)
    override fun observeWorldSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        recorder.observe(server.tickCount.toLong())
    }
}
