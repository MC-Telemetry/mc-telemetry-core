package de.mctelemetry.core.api.instruments

interface IDoubleInstrumentRegistration : IInstrumentRegistration {
    interface Mutable<out T : Mutable<T>> : IDoubleInstrumentRegistration, IInstrumentRegistration.Mutable<T>
}
