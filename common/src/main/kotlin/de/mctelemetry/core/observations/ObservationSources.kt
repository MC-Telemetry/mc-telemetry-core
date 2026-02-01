package de.mctelemetry.core.observations

import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperEnergyAmountObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperEnergyCapacityObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperEnergyFillRatioObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperFluidAmountObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperFluidFillRatioObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperItemAmountObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperItemFillRatioObservationSource
import de.mctelemetry.core.observations.scrapers.container.ContainerScraperItemIOObservationSource
import de.mctelemetry.core.observations.scrapers.nbt.NbtScraperSignLineObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperComparatorObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperDirectPowerObservationSource
import de.mctelemetry.core.observations.scrapers.redstone.RedstoneScraperPowerObservationSource

object ObservationSources {
    val ALL: List<IObservationSource<*, *>> = listOf(
        RedstoneScraperComparatorObservationSource,
        RedstoneScraperDirectPowerObservationSource,
        RedstoneScraperPowerObservationSource,
        ContainerScraperItemAmountObservationSource,
        ContainerScraperItemFillRatioObservationSource,
        ContainerScraperItemIOObservationSource,
        ContainerScraperFluidAmountObservationSource,
        ContainerScraperFluidFillRatioObservationSource,
        ContainerScraperEnergyAmountObservationSource,
        ContainerScraperEnergyCapacityObservationSource,
        ContainerScraperEnergyFillRatioObservationSource,
        NbtScraperSignLineObservationSource,
    )
}
