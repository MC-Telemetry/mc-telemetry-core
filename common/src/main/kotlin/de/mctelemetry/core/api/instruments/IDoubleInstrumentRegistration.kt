package de.mctelemetry.core.api.instruments

interface IDoubleInstrumentRegistration : IInstrumentRegistration {

    override val supportsFloating: Boolean
        get() = true

    interface Mutable<out T : Mutable<T>> : IDoubleInstrumentRegistration, IInstrumentRegistration.Mutable<T>
}
