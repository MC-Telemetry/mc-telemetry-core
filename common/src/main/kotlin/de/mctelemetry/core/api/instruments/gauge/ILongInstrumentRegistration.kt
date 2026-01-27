package de.mctelemetry.core.api.instruments.gauge

interface ILongInstrumentRegistration : IInstrumentRegistration {

    override val supportsFloating: Boolean
        get() = false

    interface Mutable<out T : Mutable<T>> : ILongInstrumentRegistration, IInstrumentRegistration.Mutable<T>
}
