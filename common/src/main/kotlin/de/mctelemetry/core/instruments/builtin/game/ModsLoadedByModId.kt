package de.mctelemetry.core.instruments.builtin.game

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationRecorder
import dev.architectury.platform.Platform

object ModsLoadedByModId : GameInstrumentBase.Simple(
    name = "game.mods.loaded.by_id",
    supportsFloating = false,
) {

    private val modIdSlot = NativeAttributeKeyTypes.StringType.createAttributeSlot("mod.id")

    override val description: String = "Which mods are currently loaded, by their mod-id."

    context(attributeStore: IMappedAttributeValueLookup.Mutable)
    override fun observeGameSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        for (mod in Platform.getMods()) {
            modIdSlot.set(mod.modId)
            recorder.observe(1)
        }
    }
}
