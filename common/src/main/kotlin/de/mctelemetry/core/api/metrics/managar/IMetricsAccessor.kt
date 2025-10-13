package de.mctelemetry.core.api.metrics.managar

import de.mctelemetry.core.metrics.exporters.MetricDataReadback
import de.mctelemetry.core.metrics.exporters.MetricDefinitionReadback
import de.mctelemetry.core.metrics.exporters.MetricValueReadback
import de.mctelemetry.core.metrics.exporters.agent.ObjectMetricsAccessor
import io.opentelemetry.api.common.Attributes
import java.util.Objects

interface IMetricsAccessor {

    fun collect(): Map<String, MetricDataReadback>

    fun collectDefinitions(): Map<String, MetricDefinitionReadback>

    companion object {

        val GLOBAL: IMetricsAccessor? by lazy {
            ObjectMetricsAccessor.Companion.INSTANCE
        }

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

    fun collectDefinition(name: String): MetricDefinitionReadback? {
        return collectDefinitions()[name]
    }

    fun collectNamed(name: String): MetricDataReadback? {
        return collect()[name]
    }

    fun collectDataPoint(
        name: String,
        attributes: Map<String, String>,
        exact: Boolean = true,
    ): MetricDataReadback?

    fun collectDataPoint(
        name: String,
        attributes: Attributes,
        exact: Boolean = true,
    ): MetricDataReadback? {
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
    ): MetricValueReadback?

    fun collectDataPointValue(
        name: String,
        attributes: Attributes,
        exact: Boolean = true,
    ): MetricValueReadback? {
        return collectDataPointValue(
            name = name,
            attributes = attributes.asMap().entries.associate {
                it.key.toString() to Objects.toString(it.value)
            },
            exact = exact,
        )
    }
}
