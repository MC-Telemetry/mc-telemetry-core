package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.metrics.ObservableDoubleMeasurement

interface IDoubleInstrumentRegistration : IInstrumentRegistration {
    interface Mutable : IDoubleInstrumentRegistration, IInstrumentRegistration.Mutable<ObservableDoubleMeasurement>
}
