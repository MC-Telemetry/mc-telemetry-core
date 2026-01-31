package de.mctelemetry.core.api.instruments.manager

import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.histogram.builder.IHistogramInstrumentBuilder
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

    fun findLocalMutable(pattern: Regex?=null): Sequence<IInstrumentRegistration.Mutable<*>> {
        return findLocal(pattern).filterIsInstance<IInstrumentRegistration.Mutable<*>>()
    }

    fun findLocalMutable(name: String): IInstrumentRegistration.Mutable<*>? {
        return findLocalMutable(Regex("^"+Regex.escape(name)+"$")).firstOrNull()
    }

    fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*>
    fun histogramInstrument(name: String): IHistogramInstrumentBuilder<*>
}


inline fun IMutableInstrumentManager.gaugeInstrument(
    name: String,
    block: IGaugeInstrumentBuilder<*>.() -> Unit,
): IGaugeInstrumentBuilder<*>{
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return gaugeInstrument(name).apply(block)
}

inline fun IMutableInstrumentManager.histogramInstrument(
    name: String,
    block: IHistogramInstrumentBuilder<*>.() -> Unit,
): IHistogramInstrumentBuilder<*> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return histogramInstrument(name).apply(block)
}
