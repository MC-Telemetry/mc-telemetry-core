package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import net.minecraft.server.MinecraftServer

object WorldTickCountInstrument : WorldInstrumentBase.Simple(
    name = "game.title.minecraft.world.tick_count",
    supportsFloating = false,
) {

    override val description: String = "Tick count of the world."

    context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer)
    override fun observeWorldSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        recorder.observe(server.tickCount.toLong())
    }
}
