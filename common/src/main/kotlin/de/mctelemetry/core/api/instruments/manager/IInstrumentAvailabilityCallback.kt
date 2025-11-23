package de.mctelemetry.core.api.instruments.manager

import de.mctelemetry.core.api.IMetricDefinition

interface IInstrumentAvailabilityCallback<in T : IMetricDefinition> {

    enum class Phase{
        PRE,
        POST,
        ;
    }

    fun instrumentAdded(manager: IInstrumentManager, instrument: T, phase: Phase)
    fun instrumentRemoved(manager: IInstrumentManager, instrument: T, phase: Phase)
}
