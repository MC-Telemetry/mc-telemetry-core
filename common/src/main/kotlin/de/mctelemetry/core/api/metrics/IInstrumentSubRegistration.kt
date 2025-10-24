package de.mctelemetry.core.api.metrics

interface IInstrumentSubRegistration<out T : IInstrumentRegistration.Mutable<T>>: AutoCloseable {
    val baseInstrument: T
}
