package de.mctelemetry.core.instruments.manager.gauge

import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.instruments.manager.InstrumentManagerBase
import de.mctelemetry.core.utils.plus

internal open class ImmutableGaugeInstrumentRegistration :
    GaugeInstrumentRegistration {

    constructor(
        name: String,
        description: String,
        unit: String,
        attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
        supportsFloating: Boolean,
        callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
    ) : super(name, description, unit, attributes, supportsFloating) {
        this.callback = callback
    }

    constructor(
        builder: InstrumentManagerBase.GaugeInstrumentBuilder<*>,
        supportsFloating: Boolean,
        callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
    ) : super(builder, supportsFloating) {
        this.callback = callback
    }

    val callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>
    override fun observe(recorder: IObservationRecorder.Resolved) {
        callback.observe(this, recorder)
    }

    override fun close() {
        var accumulator: Exception? = null
        try {
            super.close()
        } catch (ex: Exception) {
            accumulator = ex
        }
        try {
            callback.onRemove(this)
        } catch (ex: Exception) {
            accumulator + ex
        }
        if (accumulator != null) throw accumulator
    }
}
