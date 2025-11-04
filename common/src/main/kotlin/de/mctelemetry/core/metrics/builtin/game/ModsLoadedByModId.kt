package de.mctelemetry.core.metrics.builtin.game

import de.mctelemetry.core.api.metrics.managar.IGameInstrumentManager
import de.mctelemetry.core.api.metrics.managar.gaugeInstrument
import dev.architectury.platform.Platform
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

object ModsLoadedByModId : IGameInstrumentManager.Events.Ready {

    private val modId = AttributeKey.stringKey("mod.id")

    override fun gameMetricsManagerReady(manager: IGameInstrumentManager) {
        manager.gaugeInstrument("game.minecraft.mods.loaded.by_id") {
            description = "Which mods are currently loaded, by their mod-id."
            addAttribute(modId)
        }.registerWithCallbackOfLong { measurement ->
            for(mod in Platform.getMods()) {
                measurement.observe(1, Attributes.of(modId, mod.modId))
            }
        }
    }
}
