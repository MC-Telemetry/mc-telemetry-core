package de.mctelemetry.core.instruments.builtin.game

import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationRecorder
import dev.architectury.platform.Platform

object ModsLoadedByModVersion : GameInstrumentBase.Simple(
    name = "game.mods.loaded.by_version",
    supportsFloating = false,
) {

    private val modIdVersionSlot = NativeAttributeKeyTypes.StringArrayType.createAttributeSlot("mod.id_version")

    override val description: String = "Which mods are currently loaded, by their mod-id combined with their version."

    context(attributeStore: IAttributeValueStore.Mutable)
    override fun observeGameSimple(recorder: IObservationRecorder.Unresolved.Sourceless) {
        for (mod in Platform.getMods()) {
            modIdVersionSlot.set(listOf(mod.modId, mod.version))
            recorder.observe(1)
        }
    }
}
