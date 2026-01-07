package de.mctelemetry.core.observations

import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.observations.scrapers.item.ItemScraperCountObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperComparatorObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperDirectPowerObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperPowerObservationSource

object ObservationSources {
    val ALL: List<IObservationSource<*,*>> = listOf(
        RedstoneScraperComparatorObservationSource,
        RedstoneScraperDirectPowerObservationSource,
        RedstoneScraperPowerObservationSource,
        ItemScraperCountObservationSource,
    )
}
