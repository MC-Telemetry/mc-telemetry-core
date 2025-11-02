package de.mctelemetry.core.api.metrics.managar

import de.mctelemetry.core.api.metrics.IMetricDefinition

interface IInstrumentAvailabilityCallback<in T : IMetricDefinition> {

    enum class Phase{
        PRE,
        POST,
        ;
    }

    fun instrumentAdded(manager: IInstrumentManager, instrument: T, phase: Phase)
    fun instrumentRemoved(manager: IInstrumentManager, instrument: T, phase: Phase)
}
