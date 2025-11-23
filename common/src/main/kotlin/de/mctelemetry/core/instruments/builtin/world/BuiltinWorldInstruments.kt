package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager

object BuiltinWorldInstruments {

    val worldInstruments: List<IServerWorldInstrumentManager.Events.Loading> = listOf(
        PlayersOnlineCount,
        PlayersOnlineCapacity,
        PlayersOnlineByName,
        PlayersOnlineByUUID
    )

    fun register() {
        for (instrument in worldInstruments) {
            IServerWorldInstrumentManager.Events.LOADING.register(instrument)
        }
    }
}
