package de.mctelemetry.core.api.instruments.gauge

interface IInstrumentSubRegistration<out T : IInstrumentRegistration.Mutable<T>>: AutoCloseable {
    val baseInstrument: T
}
