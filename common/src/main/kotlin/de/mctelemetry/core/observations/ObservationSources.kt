package de.mctelemetry.core.observations

import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneDirectValueScraperObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneIndirectValueScraperObservationSource

object ObservationSources {
    val ALL: List<IObservationSource<*,*>> = listOf(
        RedstoneDirectValueScraperObservationSource,
        RedstoneIndirectValueScraperObservationSource,
    )
}
