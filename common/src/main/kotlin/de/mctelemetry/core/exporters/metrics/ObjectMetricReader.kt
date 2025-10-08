package de.mctelemetry.core.exporters.metrics

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.data.MetricDataType
import io.opentelemetry.sdk.metrics.export.CollectionRegistration
import io.opentelemetry.sdk.metrics.export.MetricReader

class ObjectMetricReader : MetricReader {

    companion object {

        internal const val METRIC_OBJECT_TYPE_SUMMARY = 1
        internal const val METRIC_OBJECT_TYPE_HISTOGRAM = 2
        internal const val METRIC_OBJECT_TYPE_EXP_HISTOGRAM = 3

        private inline fun <T, reified R> Collection<T>.mapArray(crossinline block: (T) -> R): Array<R> {
            val size = size
            if (size == 0) return emptyArray()
            val result = arrayOfNulls<R?>(size)
            forEachIndexed { index, value ->
                result[index] = block(value)
            }
            @Suppress("UNCHECKED_CAST")
            return result as Array<R>
        }

        private fun serializeMetricData(
            metricData: MetricData,
            attributesFilter: Map<String, String>? = null,
        ): Array<Any> {
            return arrayOf(
                metricData.name,
                metricData.description,
                metricData.unit,
                metricData.type.name,
                serializeMetricValues(metricData, attributesFilter),
            )
        }

        private fun serializeMetricDefinition(metricData: MetricData): Array<String> {
            return arrayOf(
                metricData.name,
                metricData.description,
                metricData.unit,
                metricData.type.name,
            )
        }

        private fun Attributes.match(attributesFilter: Map<String, String>?): Boolean {
            if (attributesFilter == null) return true
            var remainingAttributes = attributesFilter.size
            forEach { key, value ->
                if (attributesFilter[key.key] == value.toString())
                    remainingAttributes--
                else
                    remainingAttributes = Int.MAX_VALUE
            }
            return remainingAttributes == 0
        }

        private fun wrapMetricValue(
            attributes: Attributes,
            value: Any,
        ): Array<Any> {
            val resultArray: Array<Any?> = arrayOfNulls(attributes.size() * 2 + 1)
            resultArray[0] = value
            var idx = 1
            attributes.forEach { key, value ->
                resultArray[idx++] = key.key
                resultArray[idx++] = value.toString()
            }
            @Suppress("UNCHECKED_CAST")
            return resultArray as Array<Any>
        }

        private fun serializeMetricValues(
            metricData: MetricData,
            attributesFilter: Map<String, String>? = null,
        ): Array<Array<Any>> {
            return when (metricData.type) {
                MetricDataType.LONG_GAUGE -> metricData.longGaugeData.points.filter {
                    it.attributes.match(
                        attributesFilter
                    )
                }.mapArray { wrapMetricValue(it.attributes, it.value) }
                MetricDataType.DOUBLE_GAUGE -> metricData.doubleGaugeData.points.filter {
                    it.attributes.match(
                        attributesFilter
                    )
                }.mapArray { wrapMetricValue(it.attributes, it.value) }
                MetricDataType.LONG_SUM -> metricData.longSumData.points.filter {
                    it.attributes.match(
                        attributesFilter
                    )
                }.mapArray { wrapMetricValue(it.attributes, it.value) }
                MetricDataType.DOUBLE_SUM -> metricData.doubleSumData.points.filter {
                    it.attributes.match(
                        attributesFilter
                    )
                }.mapArray { wrapMetricValue(it.attributes, it.value) }
                MetricDataType.SUMMARY -> metricData.summaryData.points.filter {
                    it.attributes.match(
                        attributesFilter
                    )
                }.mapArray {
                    val quantileData = DoubleArray(it.values.size * 2)
                    it.values.forEachIndexed { index, value ->
                        quantileData[index] = value.quantile
                        quantileData[index + 1] = value.value
                    }
                    wrapMetricValue(it.attributes, arrayOf(METRIC_OBJECT_TYPE_SUMMARY, it.count, it.sum, quantileData))
                }
                MetricDataType.HISTOGRAM -> metricData.histogramData.points.filter {
                    it.attributes.match(attributesFilter)
                }.mapArray {
                    val countData = DoubleArray(it.boundaries.size * 2 + 1)
                    // structure:
                    // count - boundary - count - boundary - ... - boundary - count
                    it.counts.forEachIndexed { index, value ->
                        countData[index * 2] = value.toDouble()
                    }
                    it.boundaries.forEachIndexed { index, value -> //max index of boundaries is one lower than of counts
                        countData[index * 2 + 1] = value
                    }
                    wrapMetricValue(it.attributes, arrayOf(METRIC_OBJECT_TYPE_HISTOGRAM, it.count, it.sum, countData))
                }
                MetricDataType.EXPONENTIAL_HISTOGRAM -> metricData.exponentialHistogramData.points.filter {
                    it.attributes.match(attributesFilter)
                }.mapArray {
                    wrapMetricValue(it.attributes, arrayOf<Any>(METRIC_OBJECT_TYPE_EXP_HISTOGRAM, it.count, it.sum))
                }
            }
        }
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

    fun ensureActive() {
        if (!isRegistered)
            throw IllegalStateException("Not registered")
        if (!isActive)
            throw IllegalStateException("Stopping")
    }

    fun collect(): Array<Array<Any>> {
        ensureActive()
        val data = registration.collectAllMetrics()
        return data.mapArray { serializeMetricData(it) }
    }

    fun collectDefinitions(): Array<Array<String>> {
        ensureActive()
        val data = registration.collectAllMetrics()
        return data.mapArray { serializeMetricDefinition(it) }
    }

    fun collectDefinition(name: String): Array<String>? {
        ensureActive()
        val data = registration.collectAllMetrics()
        return serializeMetricDefinition(data.firstOrNull { it.name == name } ?: return null)
    }

    fun collectNamed(name: String): Array<Any>? {
        ensureActive()
        val data = registration.collectAllMetrics()
        return serializeMetricData(data.firstOrNull { it.name == name } ?: return null)
    }

    fun collectDataPoint(name: String, attributes: Array<String>): Array<Any>? {
        ensureActive()
        val data = registration.collectAllMetrics()
        val mapFilter: Map<String, String> = if (attributes.isEmpty()) emptyMap() else {
            buildMap(attributes.size / 2) {
                for (i in 0..<attributes.lastIndex step 2) {
                    put(attributes[i], attributes[i + 1])
                }
            }
        }
        return serializeMetricData(data.firstOrNull { it.name == name } ?: return null, mapFilter)
    }

    fun collectDataPointValue(name: String, attributes: Array<String>): Any? {
        ensureActive()
        val data = registration.collectAllMetrics()
        val mapFilter: Map<String, String> = if (attributes.isEmpty()) emptyMap() else {
            buildMap(attributes.size / 2) {
                for (i in 0..<attributes.lastIndex step 2) {
                    put(attributes[i], attributes[i + 1])
                }
            }
        }
        return serializeMetricValues(data.firstOrNull { it.name == name } ?: return null, mapFilter)
            .firstOrNull()
            ?.get(0)
    }

    override fun register(registration: CollectionRegistration) {
        if (this::registration.isInitialized)
            throw IllegalStateException("Already registered")
        this.isActive = true
        this.registration = registration
    }
}
