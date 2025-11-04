package de.mctelemetry.core.api.metrics

interface ILongInstrumentRegistration : IInstrumentRegistration {
    interface Mutable<out T: Mutable<T>> : ILongInstrumentRegistration, IInstrumentRegistration.Mutable<T>
}
