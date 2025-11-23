package de.mctelemetry.core.api.instruments.manager

import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.builder.IGaugeInstrumentBuilder

interface IMutableInstrumentManager : IInstrumentManager {

    fun addLocalRegistrationCallback(callback: IInstrumentAvailabilityCallback<IInstrumentRegistration>): AutoCloseable {

        return addLocalCallback(object : IInstrumentAvailabilityCallback<IInstrumentDefinition> {
            override fun instrumentAdded(
                manager: IInstrumentManager,
                instrument: IInstrumentDefinition,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IInstrumentRegistration)
                    callback.instrumentAdded(manager, instrument, phase)
            }

            override fun instrumentRemoved(
                manager: IInstrumentManager,
                instrument: IInstrumentDefinition,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IInstrumentRegistration)
                    callback.instrumentRemoved(manager, instrument, phase)
            }
        })
    }

    fun addLocalMutableRegistrationCallback(callback: IInstrumentAvailabilityCallback<IInstrumentRegistration.Mutable<*>>): AutoCloseable {
        return addLocalCallback(object : IInstrumentAvailabilityCallback<IInstrumentDefinition> {
            override fun instrumentAdded(
                manager: IInstrumentManager,
                instrument: IInstrumentDefinition,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IInstrumentRegistration.Mutable<*>)
                    callback.instrumentAdded(manager, instrument, phase)
            }

            override fun instrumentRemoved(
                manager: IInstrumentManager,
                instrument: IInstrumentDefinition,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IInstrumentRegistration.Mutable<*>)
                    callback.instrumentRemoved(manager, instrument, phase)
            }
        })
    }



    override fun findLocal(pattern: Regex): Sequence<IInstrumentRegistration>
    override fun findLocal(name: String): IInstrumentRegistration? {
        return findLocal(Regex.fromLiteral(name)).firstOrNull()
    }

    fun findLocalMutable(pattern: Regex): Sequence<IInstrumentRegistration.Mutable<*>> {
        return findLocal(pattern).filterIsInstance<IInstrumentRegistration.Mutable<*>>()
    }

    fun findLocalMutable(name: String): IInstrumentRegistration.Mutable<*>? {
        return findLocalMutable(Regex.fromLiteral(name)).firstOrNull()
    }

    fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*>
}


inline fun IMutableInstrumentManager.gaugeInstrument(
    name: String,
    block: IGaugeInstrumentBuilder<*>.() -> Unit,
): IGaugeInstrumentBuilder<*> = gaugeInstrument(name).apply(block)
