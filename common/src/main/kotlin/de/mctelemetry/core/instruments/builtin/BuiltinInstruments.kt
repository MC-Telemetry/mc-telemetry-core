package de.mctelemetry.core.instruments.builtin

import de.mctelemetry.core.instruments.builtin.game.BuiltinGameInstruments
import de.mctelemetry.core.instruments.builtin.world.BuiltinWorldInstruments

object BuiltinInstruments {

    fun register() {
        BuiltinGameInstruments.register()
        BuiltinWorldInstruments.register()
    }
}
