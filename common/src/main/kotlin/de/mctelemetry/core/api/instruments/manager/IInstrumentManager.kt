package de.mctelemetry.core.api.instruments.manager

import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.instruments.IInstrumentDefinition

interface IInstrumentManager : IInstrumentAvailabilityCallback<IMetricDefinition> {

    fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable
    fun addLocalCallback(callback: IInstrumentAvailabilityCallback<IInstrumentDefinition>): AutoCloseable

    fun findGlobal(pattern: Regex? = null): Sequence<IMetricDefinition>
    fun findGlobal(name: String): IMetricDefinition? {
        return findGlobal(Regex("^"+Regex.escape(name)+"$")).firstOrNull()
    }

    fun findLocal(pattern: Regex? = null): Sequence<IInstrumentDefinition>
    fun findLocal(name: String): IInstrumentDefinition? {
        return findLocal(Regex("^"+Regex.escape(name)+"$")).firstOrNull()
    }

    fun nameAvailable(name: String): Boolean {
        return findGlobal(name) == null
    }

    object ReadonlyEmpty : IInstrumentManager {

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

        override fun findLocal(pattern: Regex?): Sequence<IInstrumentRegistration> {
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
