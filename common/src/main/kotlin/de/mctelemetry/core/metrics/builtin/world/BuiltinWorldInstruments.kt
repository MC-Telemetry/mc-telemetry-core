package de.mctelemetry.core.metrics.builtin.world

import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager

object BuiltinWorldInstruments {

    val worldInstruments: List<IWorldInstrumentManager.Events.Loading> = listOf(
        PlayersOnlineCount,
        PlayersOnlineCapacity,
        PlayersOnlineByName,
        PlayersOnlineByUUID
    )

    fun register() {
        for (instrument in worldInstruments) {
            IWorldInstrumentManager.Events.LOADING.register(instrument)
        }
    }
}
