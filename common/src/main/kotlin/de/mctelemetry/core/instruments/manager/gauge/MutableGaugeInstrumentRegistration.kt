package de.mctelemetry.core.instruments.manager.gauge

import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.gauge.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.IInstrumentSubRegistration
import de.mctelemetry.core.api.instruments.gauge.ILongInstrumentRegistration
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.instruments.manager.InstrumentManagerBase
import de.mctelemetry.core.utils.consumeAllRethrow
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.ConcurrentLinkedDeque

internal open class MutableGaugeInstrumentRegistration<T : MutableGaugeInstrumentRegistration<T>> :
        GaugeInstrumentRegistration,
        IDoubleInstrumentRegistration.Mutable<T>,
        ILongInstrumentRegistration.Mutable<T> {

    constructor(
        name: String,
        description: String,
        unit: String,
        attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
        supportsFloating: Boolean,
    ) : super(
        name,
        description,
        unit,
        attributes,
        supportsFloating,
    )

    constructor(
        builder: InstrumentManagerBase.GaugeInstrumentBuilder<*>,
        supportsFloating: Boolean,
    ) : super(builder, supportsFloating)

    val callbacks: ConcurrentLinkedDeque<IInstrumentRegistration.Callback<T>> =
        ConcurrentLinkedDeque()

    override fun observe(recorder: IObservationRecorder.Resolved) {
        callbacks.forEach {
            it.observe(
                @Suppress("UNCHECKED_CAST")
                (this@MutableGaugeInstrumentRegistration as T),
                recorder
            )
        }
    }

    override fun addCallback(
        attributes: Attributes,
        callback: IInstrumentRegistration.Callback<T>,
    ): IInstrumentSubRegistration<T> {
        val closeCallback: IInstrumentSubRegistration<T> = object : IInstrumentSubRegistration<T> {
            override val baseInstrument: T =
                @Suppress("UNCHECKED_CAST")
                (this@MutableGaugeInstrumentRegistration as T)

            override fun close() {
                callbacks.remove(callback)
            }
        }
        callbacks.add(callback)
        return closeCallback
    }

    override fun close() {
        var accumulator: Exception? = null
        try {
            super.close()
        } catch (ex: Exception) {
            accumulator = ex
        }
        callbacks.consumeAllRethrow(accumulator) {
            it.onRemove(
                @Suppress("UNCHECKED_CAST")
                (this@MutableGaugeInstrumentRegistration as T)
            )
        }
    }
}
