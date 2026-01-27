package de.mctelemetry.core.api.instruments.gauge.builder

import de.mctelemetry.core.api.instruments.gauge.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.ILongInstrumentRegistration
import de.mctelemetry.core.api.instruments.definition.builder.IInstrumentDefinitionBuilder

interface IGaugeInstrumentBuilder<out B : IGaugeInstrumentBuilder<B>> : IInstrumentDefinitionBuilder<B> {

    fun registerWithCallbackOfLong(callback: IInstrumentRegistration.Callback<ILongInstrumentRegistration>): ILongInstrumentRegistration
    fun registerWithCallbackOfLong(callback: IInstrumentRegistration.Callback.Simple): ILongInstrumentRegistration {
        return registerWithCallbackOfLong(callback as IInstrumentRegistration.Callback<ILongInstrumentRegistration>)
    }
    fun registerWithCallbackOfDouble(callback: IInstrumentRegistration.Callback<IDoubleInstrumentRegistration>): IDoubleInstrumentRegistration
    fun registerWithCallbackOfDouble(callback: IInstrumentRegistration.Callback.Simple): IDoubleInstrumentRegistration {
        return registerWithCallbackOfDouble(callback as IInstrumentRegistration.Callback<IDoubleInstrumentRegistration>)
    }
    fun registerMutableOfLong(): ILongInstrumentRegistration.Mutable<ILongInstrumentRegistration.Mutable<*>>
    fun registerMutableOfDouble(): IDoubleInstrumentRegistration.Mutable<IDoubleInstrumentRegistration.Mutable<*>>
}
