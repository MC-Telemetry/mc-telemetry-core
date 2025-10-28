package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.metrics.IMappedAttributeValueLookup
import de.mctelemetry.core.api.metrics.IObservationObserver
import de.mctelemetry.core.api.metrics.IObservationSource

class ObservationMappingResolver(
    val resolvedObserver: IObservationObserver.Resolved,
    var mapping: ObservationAttributeMapping,
) : IObservationObserver.Unresolved {

    override fun observe(source: IObservationSource<*, *>, value: Double, attributes: IMappedAttributeValueLookup) {
        resolvedObserver.observe(value, mapping.resolveAttributes(attributes), source)
    }

    override fun observe(source: IObservationSource<*, *>, value: Long, attributes: IMappedAttributeValueLookup) {
        resolvedObserver.observe(value, mapping.resolveAttributes(attributes), source)
    }

    override fun observePreferred(
        source: IObservationSource<*, *>,
        double: Double,
        long: Long,
        attributes: IMappedAttributeValueLookup,
    ) {
        resolvedObserver.observePreferred(double, long, mapping.resolveAttributes(attributes), source)
    }
}
