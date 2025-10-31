package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.common.Attributes

interface IInstrumentRegistration : IInstrumentDefinition, AutoCloseable {

    fun interface Callback<in T : IInstrumentRegistration> {

        val invocationSyncHint: InvocationSynchronizationHint
            get() = InvocationSynchronizationHint.DEFAULT
        val tickSynchronizationHint: TickSynchronizationHint
            get() = TickSynchronizationHint.DEFAULT

        fun observe(instrument: T, recorder: IObservationRecorder.Resolved)

        fun onRemove(instrument: T) {}

        fun interface Simple : Callback<IInstrumentRegistration> {

            override fun observe(instrument: IInstrumentRegistration, recorder: IObservationRecorder.Resolved) {
                observe(recorder)
            }

            fun observe(recorder: IObservationRecorder.Resolved)
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
