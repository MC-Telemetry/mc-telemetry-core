package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.instruments.builtin.world.level.BuiltinLevelInstruments
import de.mctelemetry.core.instruments.builtin.world.player.BuiltinPlayerInstruments

object BuiltinWorldInstruments {

    val worldInstruments: List<WorldInstrumentBase<*>> = listOf<WorldInstrumentBase<*>>(
        PlayersOnlineCountInstrument,
        PlayersOnlineCapacityInstrument,
        WorldTickCountInstrument,
    ) + BuiltinLevelInstruments.levelInstruments + BuiltinPlayerInstruments.playerInstruments

    fun register() {
        for (instrument in worldInstruments) {
            IServerWorldInstrumentManager.Events.LOADING.register(instrument)
        }
    }
}
