package de.mctelemetry.core.metrics.builtin.game

import de.mctelemetry.core.api.metrics.managar.IGameInstrumentManager


object BuiltinGameInstruments{

    val gameInstruments: List<IGameInstrumentManager.Events.Ready> = listOf(
        ModsLoadedByModId,
        ModsLoadedByModVersion,
    )

    fun register() {
        for(instrument in gameInstruments){
            IGameInstrumentManager.Events.READY.register(instrument)
        }
    }
}
