package de.mctelemetry.core.metrics.exporters

data class MetricDefinitionReadback(
    val name: String,
    val description: String,
    val unit: String,
    val type: String,
)
