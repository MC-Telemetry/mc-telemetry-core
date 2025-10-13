package de.mctelemetry.core.metrics.exporters

import de.mctelemetry.core.api.metrics.IMetricDefinition

data class MetricDefinitionReadback(
    override val name: String,
    override val description: String,
    override val unit: String,
    val type: String,
): IMetricDefinition
