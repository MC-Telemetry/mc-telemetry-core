package de.mctelemetry.core.api.metrics

interface IMetricDefinition {
    val name: String
    val description: String
    val unit: String
}
