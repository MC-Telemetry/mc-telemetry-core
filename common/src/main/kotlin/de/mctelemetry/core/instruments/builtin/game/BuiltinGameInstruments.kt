package de.mctelemetry.core.instruments.builtin.game

import de.mctelemetry.core.api.instruments.manager.IGameInstrumentManager


object BuiltinGameInstruments{

    val gameInstruments: List<GameInstrumentBase<*>> = listOf(
        ModsLoadedByModId,
        ModsLoadedByModVersion,
    )

    fun register() {
        for(instrument in gameInstruments){
            IGameInstrumentManager.Events.READY.register(instrument)
        }
    }
}
