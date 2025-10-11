package de.mctelemetry.core.metrics.exporters

import it.unimi.dsi.fastutil.doubles.DoubleList

sealed class MetricValueReadback(val type: String) {
    data class MetricLongValue(val value: Long) : MetricValueReadback("value_long")
    data class MetricDoubleValue(val value: Double) : MetricValueReadback("value_double")
    data class MetricSummaryValue(val count: Long, val sum: Double, val quantiles: List<QuantileData>) :
            MetricValueReadback("summary") {

        data class QuantileData(
            val quantile: Double,
            val value: Double,
        )
    }

    data class MetricHistogramValue(
        val count: Long,
        val sum: Double,
        val buckets: DoubleList,
        val counts: DoubleList,
    ) : MetricValueReadback("histogram")

    data class MetricExponentialHistogramValue(
        val count: Long,
        val sum: Double,
    ) : MetricValueReadback("exponential_histogram")
}
