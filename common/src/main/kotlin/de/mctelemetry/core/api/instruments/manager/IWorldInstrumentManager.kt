package de.mctelemetry.core.api.instruments.manager

import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition

interface IWorldInstrumentManager : IInstrumentManager {

    override fun findLocal(name: String): IWorldInstrumentDefinition? {
        return findLocal(Regex("^"+Regex.escape(name)+"$")).firstOrNull()
    }

    override fun findLocal(pattern: Regex?): Sequence<IWorldInstrumentDefinition>

    object ReadonlyEmpty : IWorldInstrumentManager {
        override fun instrumentAdded(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase,
        ) {
        }

        override fun instrumentRemoved(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase,
        ) {
        }

        override fun findLocal(pattern: Regex?): Sequence<IWorldInstrumentDefinition> {
            return emptySequence()
        }

        override fun findGlobal(pattern: Regex?): Sequence<IMetricDefinition> {
            return emptySequence()
        }

        override fun nameAvailable(name: String): Boolean {
            return true
        }

        override fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable {
            return AutoCloseable {}
        }

        override fun addLocalCallback(callback: IInstrumentAvailabilityCallback<IInstrumentDefinition>): AutoCloseable {
            return AutoCloseable {}
        }
    }
}
