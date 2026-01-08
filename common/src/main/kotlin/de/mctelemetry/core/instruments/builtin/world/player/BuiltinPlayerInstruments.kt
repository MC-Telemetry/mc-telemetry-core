package de.mctelemetry.core.instruments.builtin.world.player


object BuiltinPlayerInstruments {

    val playerInstruments: List<PlayerInstrumentBase<*>> = listOf(
        PlayerInfoInstrument,
        PlayerScoreboardInstrument,
    )
}
