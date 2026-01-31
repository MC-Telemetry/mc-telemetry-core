package de.mctelemetry.core.instruments.manager.gauge

import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.gauge.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.ILongInstrumentRegistration
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.instruments.manager.InstrumentManagerBase
import de.mctelemetry.core.instruments.manager.ResolvedObservationRecorder
import io.opentelemetry.api.metrics.ObservableMeasurement
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal abstract class GaugeInstrumentRegistration(
    override val name: String,
    override val description: String,
    override val unit: String,
    override val attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
    override val supportsFloating: Boolean,
) : IDoubleInstrumentRegistration, ILongInstrumentRegistration {

    constructor(builder: InstrumentManagerBase.GaugeInstrumentBuilder<*>, supportsFloating: Boolean) : this(
        builder.name,
        builder.description,
        builder.unit,
        builder.attributes.associateBy { it.baseKey.key.lowercase() },
        supportsFloating,
    ) {
        untrackCallback.set(builder.manager::untrackRegistration)
    }

    private val otelRegistration: AtomicReference<AutoCloseable?> = AtomicReference(null)
    private val untrackCallback: AtomicReference<((name: String, value: IInstrumentRegistration) -> Unit)?> =
        AtomicReference(null)

    protected val closed: AtomicBoolean = AtomicBoolean(false)

    open fun observe(instrument: ObservableMeasurement) {
        if (closed.get()) {
            otelRegistration.get()?.close()
            untrackCallback.get()?.invoke(name, this)
            return
        }
        observe(ResolvedObservationRecorder.Companion(instrument, supportsFloating = supportsFloating))
    }

    abstract override fun observe(recorder: IObservationRecorder.Resolved)

    fun provideOTelRegistration(otelRegistration: AutoCloseable) {
        val previous = this.otelRegistration.compareAndExchange(null, otelRegistration)
        if (previous != null) throw IllegalStateException("Unregister callback already provided: $previous (tried to set $otelRegistration)")
        if (closed.get())
            otelRegistration.close()
    }

    fun provideUntrackCallback(callback: (name: String, value: IInstrumentRegistration) -> Unit) {
        val previous = untrackCallback.compareAndExchange(null, callback)
        if (previous != null) throw IllegalStateException("Untrack callback already provided: $previous (tried to set $callback)")
        if (closed.get())
            callback.invoke(name, this)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        var ex: Exception? = null
        try {
            otelRegistration.get()!!.close()
        } catch (ex2: Exception) {
            ex = ex2
        }
        try {
            untrackCallback.get()!!.invoke(name, this)
        } catch (ex2: Exception) {
            if (ex == null)
                ex = ex2
            else
                ex.addSuppressed(ex2)
        }
        if (ex != null) throw ex
    }
}
