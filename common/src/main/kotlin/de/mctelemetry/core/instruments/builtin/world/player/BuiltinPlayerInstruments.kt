package de.mctelemetry.core.instruments.builtin.world.player


object BuiltinPlayerInstruments {

    val playerInstruments: List<PlayerInstrumentBase<*>> = listOf(
        PlayerInfoInstrument,
        PlayerScoreboardInstrument,
        PlayerGameModeInstrument,
        PlayerPositionInstrument.PlayerPositionXInstrument,
        PlayerPositionInstrument.PlayerPositionYInstrument,
        PlayerPositionInstrument.PlayerPositionZInstrument,
        PlayerFoodInstruments.PlayerFoodLevelInstrument,
        PlayerFoodInstruments.PlayerSaturationInstrument,
        PlayerFoodInstruments.PlayerExhaustionInstrument,
        PlayerHealthInstruments.PlayerHealthInstrument,
        PlayerHealthInstruments.PlayerMaxHealthInstrument,
        PlayerExperienceInstruments.PlayerExperienceLevelInstrument,
        PlayerExperienceInstruments.PlayerTotalExperienceInstrument,
        PlayerExperienceInstruments.PlayerScoreInstrument,
    )
}
