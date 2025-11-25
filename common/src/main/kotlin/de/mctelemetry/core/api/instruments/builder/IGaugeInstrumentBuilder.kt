package de.mctelemetry.core.api.instruments.builder

import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration

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
