package de.mctelemetry.core.utils

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IMetricDefinition
import de.mctelemetry.core.api.metrics.managar.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import org.apache.logging.log4j.Level

internal class InstrumentAvailabilityLogger(
    val context: Any,
    val local: Boolean,
    val level: Level = Level.TRACE,
) : IInstrumentAvailabilityCallback<IMetricDefinition> {

    override fun instrumentAdded(
        manager: IInstrumentManager,
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (local) {
            if (context === manager) {
                OTelCoreMod.logger.log(
                    level,
                    "Local instrumentAdded-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                OTelCoreMod.logger.log(
                    level,
                    "Local instrumentAdded-{} for {} of {} in {}",
                    phase,
                    instrument.name,
                    manager,
                    context,
                )
            }
        } else {
            if (context === manager) {
                OTelCoreMod.logger.log(
                    level,
                    "Global instrumentAdded-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                OTelCoreMod.logger.log(
                    level,
                    "Global instrumentAdded-{} for {} of {} in {}",
                    phase,
                    instrument.name,
                    manager,
                    context,
                )
            }
        }
    }

    override fun instrumentRemoved(
        manager: IInstrumentManager,
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (local) {
            if (context === manager) {
                OTelCoreMod.logger.log(
                    level,
                    "Local instrumentRemoved-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                OTelCoreMod.logger.log(
                    level,
                    "Local instrumentRemoved-{} for {} of {} in {}",
                    phase,
                    instrument.name,
                    manager,
                    context,
                )
            }
        } else {
            if (context === manager) {
                OTelCoreMod.logger.log(
                    level,
                    "Global instrumentRemoved-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                OTelCoreMod.logger.log(
                    level,
                    "Global instrumentRemoved-{} for {} of {} in {}",
                    phase,
                    instrument.name,
                    manager,
                    context,
                )
            }
        }
    }
}
