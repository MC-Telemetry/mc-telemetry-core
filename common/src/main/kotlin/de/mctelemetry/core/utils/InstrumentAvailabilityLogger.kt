package de.mctelemetry.core.utils

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

internal class InstrumentAvailabilityLogger(
    val context: Any,
    val local: Boolean,
    val level: Level = Level.TRACE,
) : IInstrumentAvailabilityCallback<IMetricDefinition> {

    companion object {

        private val subLogger: Logger =
            LogManager.getLogger("${OTelCoreMod.MOD_ID}.${InstrumentAvailabilityLogger::class.java.simpleName}")
    }

    override fun instrumentAdded(
        manager: IInstrumentManager,
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (local) {
            if (context === manager) {
                subLogger.log(
                    level,
                    "Local instrumentAdded-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                subLogger.log(
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
                subLogger.log(
                    level,
                    "Global instrumentAdded-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                subLogger.log(
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
                subLogger.log(
                    level,
                    "Local instrumentRemoved-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                subLogger.log(
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
                subLogger.log(
                    level,
                    "Global instrumentRemoved-{} for {} of {}",
                    phase,
                    instrument.name,
                    manager,
                )
            } else {
                subLogger.log(
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
