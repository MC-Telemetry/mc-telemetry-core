package de.mctelemetry.core.api.instruments.manager

import de.mctelemetry.core.metrics.exporters.MetricDataReadback
import de.mctelemetry.core.metrics.exporters.MetricDefinitionReadback
import de.mctelemetry.core.metrics.exporters.MetricValueReadback
import io.opentelemetry.api.common.Attributes
import java.util.Objects
import java.util.concurrent.CompletableFuture

interface IMetricsAccessor {

    fun collect(): CompletableFuture<Map<String, MetricDataReadback>>

    fun collectDefinitions(): CompletableFuture<Map<String, MetricDefinitionReadback>>

    companion object {

        fun matchFilter(value: Attributes, filter: Map<String, String>?, exact: Boolean): Boolean {
            if (filter == null) return true
            if ((!exact) && filter.isEmpty()) return true
            var remainingAttributes = filter.size
            value.forEach { k, v ->
                if (filter.getOrElse(k.key) {
                        if (exact) null
                        remainingAttributes = Int.MAX_VALUE
                        return@forEach
                    } != v.toString())
                    remainingAttributes--
                else
                    remainingAttributes = Int.MAX_VALUE
            }
            return remainingAttributes == 0
        }

        fun matchFilter(value: Map<String, String>, filter: Map<String, String>?, exact: Boolean): Boolean {
            if (filter == null) return true
            return if (exact) {
                value == filter
            } else {
                filter.all { (k, v) ->
                    value[k] == v
                }
            }
        }
    }

    fun collectDefinition(name: String): CompletableFuture<MetricDefinitionReadback?> {
        return collectDefinitions().thenApply { it[name] }
    }

    fun collectNamed(name: String): CompletableFuture<MetricDataReadback?> {
        return collect().thenApply { it[name] }
    }

    fun collectDataPoint(
        name: String,
        attributes: Map<String, String>,
        exact: Boolean = true,
    ): CompletableFuture<MetricDataReadback?>

    fun collectDataPoint(
        name: String,
        attributes: Attributes,
        exact: Boolean = true,
    ): CompletableFuture<MetricDataReadback?> {
        return collectDataPoint(
            name = name,
            attributes = attributes.asMap().entries.associate {
                it.key.toString() to Objects.toString(it.value)
            },
            exact = exact,
        )
    }

    fun collectDataPointValue(
        name: String,
        attributes: Map<String, String>,
        exact: Boolean = true,
    ): CompletableFuture<MetricValueReadback?>

    fun collectDataPointValue(
        name: String,
        attributes: Attributes,
        exact: Boolean = true,
    ): CompletableFuture<MetricValueReadback?> {
        return collectDataPointValue(
            name = name,
            attributes = attributes.asMap().entries.associate {
                it.key.toString() to Objects.toString(it.value)
            },
            exact = exact,
        )
    }
}
