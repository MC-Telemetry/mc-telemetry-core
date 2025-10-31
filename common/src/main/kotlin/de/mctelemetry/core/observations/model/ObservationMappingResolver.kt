package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.metrics.IMappedAttributeValueLookup
import de.mctelemetry.core.api.metrics.IObservationRecorder
import de.mctelemetry.core.api.metrics.IObservationSource

class ObservationMappingResolver(
    val resolvedRecorder: IObservationRecorder.Resolved,
    var mapping: ObservationAttributeMapping,
) : IObservationRecorder.Unresolved {

    override fun observe(source: IObservationSource<*, *>, value: Double, attributes: IMappedAttributeValueLookup) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(attributes), source)
    }

    override fun observe(source: IObservationSource<*, *>, value: Long, attributes: IMappedAttributeValueLookup) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(attributes), source)
    }

    override fun observePreferred(
        source: IObservationSource<*, *>,
        double: Double,
        long: Long,
        attributes: IMappedAttributeValueLookup,
    ) {
        resolvedRecorder.observePreferred(double, long, mapping.resolveAttributes(attributes), source)
    }
}
