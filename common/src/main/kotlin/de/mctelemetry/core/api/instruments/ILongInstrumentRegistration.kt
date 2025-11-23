package de.mctelemetry.core.api.instruments

interface ILongInstrumentRegistration : IInstrumentRegistration {
    interface Mutable<out T: Mutable<T>> : ILongInstrumentRegistration, IInstrumentRegistration.Mutable<T>
}
