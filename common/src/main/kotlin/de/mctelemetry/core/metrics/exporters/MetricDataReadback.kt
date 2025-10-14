package de.mctelemetry.core.metrics.exporters

data class MetricDataReadback(
    val name: String,
    val description: String,
    val unit: String,
    val type: String,
    val data: Map<Map<String, String>, MetricValueReadback>,
)
