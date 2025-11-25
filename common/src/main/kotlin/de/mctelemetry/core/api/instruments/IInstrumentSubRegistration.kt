package de.mctelemetry.core.api.instruments

interface IInstrumentSubRegistration<out T : IInstrumentRegistration.Mutable<T>>: AutoCloseable {
    val baseInstrument: T
}
