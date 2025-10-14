package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.ObservableMeasurement

interface IInstrumentRegistration : IMetricDefinition, AutoCloseable {
    val attributes: Map<String, MappedAttributeKeyInfo<*,*>>

    fun interface Callback<in R : ObservableMeasurement> {

        val invocationSyncHint: InvocationSynchronizationHint
            get() = InvocationSynchronizationHint.DEFAULT
        val tickSynchronizationHint: TickSynchronizationHint
            get() = TickSynchronizationHint.DEFAULT

        fun observe(recorder: R)
    }

    interface Mutable<out R : ObservableMeasurement> : IInstrumentRegistration {

        fun addCallback(
            attributes: Attributes = Attributes.empty(),
            callback: Callback<R>,
        ): AutoCloseable
    }
}
