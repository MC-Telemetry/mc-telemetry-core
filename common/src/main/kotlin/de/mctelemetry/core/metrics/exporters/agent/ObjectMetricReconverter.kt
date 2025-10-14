package de.mctelemetry.core.metrics.exporters.agent

import de.mctelemetry.core.metrics.exporters.MetricDataReadback
import de.mctelemetry.core.metrics.exporters.MetricDefinitionReadback
import de.mctelemetry.core.metrics.exporters.MetricValueReadback
import it.unimi.dsi.fastutil.doubles.DoubleArrayList

object ObjectMetricReconverter {

    internal fun convertMetric(metricData: Array<Any>): MetricDataReadback {
        @Suppress("UNCHECKED_CAST")
        return MetricDataReadback(
            name = metricData[0] as String,
            description = metricData[1] as String,
            unit = metricData[2] as String,
            type = metricData[3] as String,
            data = (metricData[4] as Array<Array<Any>>).associate { dataPoint ->
                val value = dataPoint[0]
                buildMap((dataPoint.size - 1) / 2) {
                    for (i in 1..<dataPoint.lastIndex step 2) {
                        put(dataPoint[i] as String, dataPoint[i + 1] as String)
                    }
                } to convertMetricDataPointValue(value)
            }
        )
    }

    internal fun convertMetrics(metricData: Array<Array<Any>>): Map<String, MetricDataReadback> {
        return metricData.associate { data ->
            convertMetric(data).let { it.name to it }
        }
    }

    internal fun convertMetricDefinition(definitionData: Array<String>): MetricDefinitionReadback {
        return MetricDefinitionReadback(
            name = definitionData[0],
            description = definitionData[1],
            unit = definitionData[2],
            type = definitionData[3],
        )
    }

    internal fun convertMetricDefinitions(definitionData: Array<Array<String>>): Map<String, MetricDefinitionReadback> {
        return definitionData.associate { data ->
            convertMetricDefinition(data).let { it.name to it }
        }
    }

    internal fun convertMetricDataPointValue(value: Any): MetricValueReadback {
        return if (value is Array<*>) {
            when (value[0]) {
                ObjectMetricReader.METRIC_OBJECT_TYPE_SUMMARY ->
                    MetricValueReadback.MetricSummaryValue(
                        count = value[1] as Long,
                        sum = value[2] as Double,
                        quantiles = (value[3] as DoubleArray).let { array ->
                            buildList(array.size / 2) {
                                for (i in 0..<array.lastIndex step 2) {
                                    add(
                                        MetricValueReadback.MetricSummaryValue.QuantileData(
                                            quantile = array[i],
                                            value = array[i + 1],
                                        )
                                    )
                                }
                            }
                        })
                ObjectMetricReader.METRIC_OBJECT_TYPE_HISTOGRAM -> {
                    val countData: DoubleArray = value[3] as DoubleArray
                    val buckets = DoubleArrayList(countData.size / 2)
                    val counts = DoubleArrayList(countData.size / 2 + 1)
                    for (i in 0..<countData.lastIndex step 2) {
                        counts.add(countData[i])
                        buckets.add(countData[i + 1])
                    }
                    if (countData.size % 2 == 1)
                        counts.add(countData.last())
                    MetricValueReadback.MetricHistogramValue(
                        count = value[1] as Long,
                        sum = value[2] as Double,
                        buckets = buckets,
                        counts = counts,
                    )
                }
                ObjectMetricReader.METRIC_OBJECT_TYPE_EXP_HISTOGRAM -> MetricValueReadback.MetricExponentialHistogramValue(
                    count = value[1] as Long,
                    sum = value[2] as Double,
                )
                else -> throw IllegalArgumentException("Unknown type identifier for data array $value")
            }
        } else {
            when (value) {
                is Long -> MetricValueReadback.MetricLongValue(value)
                is Double -> MetricValueReadback.MetricDoubleValue(value)
                else -> throw IllegalArgumentException("Unknown data point $value")
            }
        }
    }
}
