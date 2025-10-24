package de.mctelemetry.core.api.metrics

interface IDoubleInstrumentRegistration : IInstrumentRegistration {
    interface Mutable<out T : Mutable<T>> : IDoubleInstrumentRegistration, IInstrumentRegistration.Mutable<T>
}
