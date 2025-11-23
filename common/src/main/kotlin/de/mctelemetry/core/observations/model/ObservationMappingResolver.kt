package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource

class ObservationMappingResolver(
    val resolvedRecorder: IObservationRecorder.Resolved,
    var mapping: ObservationAttributeMapping,
) : IObservationRecorder.Unresolved {

    override val supportsFloating: Boolean = resolvedRecorder.supportsFloating

    override fun observe(value: Double, attributes: IMappedAttributeValueLookup, source: IObservationSource<*, *>) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(attributes), source)
    }

    override fun observe(value: Long, attributes: IMappedAttributeValueLookup, source: IObservationSource<*, *>) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(attributes), source)
    }

    override fun observePreferred(
        double: Double,
        long: Long,
        attributes: IMappedAttributeValueLookup,
        source: IObservationSource<*, *>,
    ) {
        resolvedRecorder.observePreferred(double, long, mapping.resolveAttributes(attributes), source)
    }
}
