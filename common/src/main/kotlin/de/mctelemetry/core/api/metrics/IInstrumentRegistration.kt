package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.common.Attributes

interface IInstrumentRegistration : IInstrumentDefinition, AutoCloseable {

    fun interface Callback<in T : IInstrumentRegistration> {

        val invocationSyncHint: InvocationSynchronizationHint
            get() = InvocationSynchronizationHint.DEFAULT
        val tickSynchronizationHint: TickSynchronizationHint
            get() = TickSynchronizationHint.DEFAULT

        fun observe(instrument: T, recorder: IObservationObserver.Resolved)

        fun onRemove(instrument: T) {}

        fun interface Simple : Callback<IInstrumentRegistration> {

            override fun observe(instrument: IInstrumentRegistration, recorder: IObservationObserver.Resolved) {
                observe(recorder)
            }

            fun observe(recorder: IObservationObserver.Resolved)
            override fun onRemove(instrument: IInstrumentRegistration) {
                onRemove()
            }

            fun onRemove() {}
        }
    }

    interface Mutable<out T : Mutable<T>> : IInstrumentRegistration {

        fun addCallback(
            attributes: Attributes = Attributes.empty(),
            callback: Callback<T>,
        ): IInstrumentSubRegistration<T>
    }
}
