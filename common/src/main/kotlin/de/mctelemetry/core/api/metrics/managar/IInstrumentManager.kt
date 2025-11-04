package de.mctelemetry.core.api.metrics.managar

import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.IMetricDefinition
import de.mctelemetry.core.api.metrics.builder.IGaugeInstrumentBuilder

interface IInstrumentManager : IInstrumentAvailabilityCallback<IMetricDefinition> {

    fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable
    fun addLocalCallback(callback: IInstrumentAvailabilityCallback<IInstrumentRegistration>): AutoCloseable
    fun addLocalMutableCallback(callback: IInstrumentAvailabilityCallback<IInstrumentRegistration.Mutable<*>>): AutoCloseable {
        return addLocalCallback(object : IInstrumentAvailabilityCallback<IInstrumentRegistration> {
            override fun instrumentAdded(
                manager: IInstrumentManager,
                instrument: IInstrumentRegistration,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IInstrumentRegistration.Mutable<*>)
                    callback.instrumentAdded(manager, instrument, phase)
            }

            override fun instrumentRemoved(
                manager: IInstrumentManager,
                instrument: IInstrumentRegistration,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IInstrumentRegistration.Mutable<*>)
                    callback.instrumentRemoved(manager, instrument, phase)
            }
        })
    }

    fun findGlobal(pattern: Regex): Sequence<IMetricDefinition>
    fun findGlobal(name: String): IMetricDefinition? {
        return findGlobal(Regex.fromLiteral(name)).firstOrNull()
    }

    fun findLocal(pattern: Regex): Sequence<IInstrumentRegistration>
    fun findLocal(name: String): IInstrumentRegistration? {
        return findLocal(Regex.fromLiteral(name)).firstOrNull()
    }

    fun findLocalMutable(pattern: Regex): Sequence<IInstrumentRegistration.Mutable<*>> {
        return findLocal(pattern).filterIsInstance<IInstrumentRegistration.Mutable<*>>()
    }

    fun findLocalMutable(name: String): IInstrumentRegistration.Mutable<*>? {
        return findLocalMutable(Regex.fromLiteral(name)).firstOrNull()
    }

    fun nameAvailable(name: String): Boolean {
        return findGlobal(name) == null
    }

    fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*>

    object ReadonlyEmpty : IInstrumentManager {

        override fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*> {
            throw UnsupportedOperationException()
        }

        override fun instrumentAdded(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase
        ) {
            throw UnsupportedOperationException()
        }

        override fun instrumentRemoved(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase
        ) {
            throw UnsupportedOperationException()
        }

        override fun findLocal(pattern: Regex): Sequence<IInstrumentRegistration> {
            return emptySequence()
        }

        override fun findGlobal(pattern: Regex): Sequence<IMetricDefinition> {
            return emptySequence()
        }

        override fun nameAvailable(name: String): Boolean {
            return true
        }

        override fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable {
            return AutoCloseable {}
        }

        override fun addLocalCallback(callback: IInstrumentAvailabilityCallback<IInstrumentRegistration>): AutoCloseable {
            return AutoCloseable {}
        }
    }
}

inline fun IInstrumentManager.gaugeInstrument(
    name: String,
    block: IGaugeInstrumentBuilder<*>.() -> Unit,
): IGaugeInstrumentBuilder<*> = gaugeInstrument(name).apply(block)
