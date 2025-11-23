package de.mctelemetry.core.api.instruments

import de.mctelemetry.core.api.IMetricDefinition

class DuplicateInstrumentException : IllegalArgumentException {

    val name: String
    val existing: IMetricDefinition?

    constructor(name: String, existing: IMetricDefinition?) : this(
        name,
        existing,
        message = defaultMessage(name, existing)
    )

    constructor(name: String, existing: IMetricDefinition?, message: String) : super(message) {
        this.name = name
        this.existing = existing
    }

    constructor(name: String, existing: IMetricDefinition?, cause: Throwable?) : this(
        name,
        existing,
        message = defaultMessage(name, existing),
        cause = cause,
    )

    constructor(name: String, existing: IMetricDefinition?, message: String, cause: Throwable?) : super(message, cause) {
        this.name = name
        this.existing = existing
    }

    companion object {

        private fun defaultMessage(name: String, existing: IMetricDefinition?): String =
            if(existing == null)
                "Metric with name $name already exists"
            else
                "Metric with name $name already exists: $existing"
    }
}
