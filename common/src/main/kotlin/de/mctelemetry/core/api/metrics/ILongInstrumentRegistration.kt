package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.metrics.ObservableLongMeasurement

interface ILongInstrumentRegistration : IInstrumentRegistration {
    interface Mutable : ILongInstrumentRegistration, IInstrumentRegistration.Mutable<ObservableLongMeasurement>
}
