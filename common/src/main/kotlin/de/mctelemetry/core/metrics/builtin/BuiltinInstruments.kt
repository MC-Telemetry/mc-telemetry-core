package de.mctelemetry.core.metrics.builtin

import de.mctelemetry.core.metrics.builtin.game.BuiltinGameInstruments
import de.mctelemetry.core.metrics.builtin.world.BuiltinWorldInstruments

object BuiltinInstruments {

    fun register() {
        BuiltinGameInstruments.register()
        BuiltinWorldInstruments.register()
    }
}
