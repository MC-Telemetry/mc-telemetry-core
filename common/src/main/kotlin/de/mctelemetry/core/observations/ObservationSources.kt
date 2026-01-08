package de.mctelemetry.core.observations

import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.observations.scrapers.energy.EnergyScraperAmountObservationSource
import de.mctelemetry.core.observations.scrapers.energy.EnergyScraperCapacityObservationSource
import de.mctelemetry.core.observations.scrapers.energy.EnergyScraperFillRatioObservationSource
import de.mctelemetry.core.observations.scrapers.fluid.FluidScraperAmountObservationSource
import de.mctelemetry.core.observations.scrapers.fluid.FluidScraperFillRatioObservationSource
import de.mctelemetry.core.observations.scrapers.item.ItemScraperAmountObservationSource
import de.mctelemetry.core.observations.scrapers.item.ItemScraperFillRatioObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperComparatorObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperDirectPowerObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperPowerObservationSource

object ObservationSources {
    val ALL: List<IObservationSource<*,*>> = listOf(
        RedstoneScraperComparatorObservationSource,
        RedstoneScraperDirectPowerObservationSource,
        RedstoneScraperPowerObservationSource,
        ItemScraperAmountObservationSource,
        ItemScraperFillRatioObservationSource,
        FluidScraperAmountObservationSource,
        FluidScraperFillRatioObservationSource,
        EnergyScraperAmountObservationSource,
        EnergyScraperCapacityObservationSource,
        EnergyScraperFillRatioObservationSource,
    )
}
