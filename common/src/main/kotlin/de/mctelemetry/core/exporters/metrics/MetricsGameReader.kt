package de.mctelemetry.core.exporters.metrics

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.CollectionRegistration
import io.opentelemetry.sdk.metrics.export.MetricReader

class MetricsGameReader() : MetricReader {

    init {
        println("Constructing MetricsGameReader")
    }

    companion object {
        private lateinit var _INSTANCE: MetricsGameReader
        var INSTANCE: MetricsGameReader? = null
            get() = if(::_INSTANCE.isInitialized) _INSTANCE else null
            private set(value) {
                if(field != null) {
                    throw IllegalStateException("MetricsGameReader-Instance has already been initialized")
                }
                field = value
            }

        @JvmStatic
        fun getInstance(): MetricsGameReader? {
            return INSTANCE
        }
    }

    fun makeGlobal() {
        INSTANCE = this
    }

    override fun forceFlush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    override fun getAggregationTemporality(instrumentType: InstrumentType): AggregationTemporality {
        return AggregationTemporality.CUMULATIVE
    }

    override fun shutdown(): CompletableResultCode {
        this.isActive = false
        return CompletableResultCode.ofSuccess()
    }

    private lateinit var registration: CollectionRegistration

    val isRegistered: Boolean
        get() = this::registration.isInitialized

    var isActive: Boolean = false
        private set

    fun collect(): Collection<MetricData> {
        if(!this::registration.isInitialized)
            throw IllegalStateException("Not registered")
        return registration.collectAllMetrics()
    }

    override fun register(registration: CollectionRegistration) {
        if (this::registration.isInitialized)
            throw IllegalStateException("Already registered")
        this.isActive = true
        this.registration = registration
    }
}
