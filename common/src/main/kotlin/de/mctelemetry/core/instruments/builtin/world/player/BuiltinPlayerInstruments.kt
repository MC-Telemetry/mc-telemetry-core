package de.mctelemetry.core.instruments.builtin.world.player


object BuiltinPlayerInstruments {

    val playerInstruments: List<PlayerInstrumentBase<*>> = listOf(
        PlayerInfoInstrument,
        PlayerScoreboardInstrument,
        PlayerPositionInstrument.PlayerPositionXInstrument,
        PlayerPositionInstrument.PlayerPositionYInstrument,
        PlayerPositionInstrument.PlayerPositionZInstrument,
        PlayerFoodInstruments.PlayerFoodLevelInstrument,
        PlayerFoodInstruments.PlayerSaturationInstrument,
        PlayerFoodInstruments.PlayerExhaustionInstrument,
    )
}
