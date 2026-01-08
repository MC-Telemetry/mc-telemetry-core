package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder

class ObservationIdentityResolver(
    val resolvedRecorder: IObservationRecorder.Resolved,
) : IObservationRecorder.Unresolved.Sourceless {

    override val supportsFloating: Boolean = resolvedRecorder.supportsFloating

    context(attributeStore: IMappedAttributeValueLookup)
    override fun observe(value: Double) {
        resolvedRecorder.observe(value, ObservationAttributeMapping.resolveAttributesUnmapped(), source = null)
    }

    context(attributeStore: IMappedAttributeValueLookup)
    override fun observe(value: Long) {
        resolvedRecorder.observe(value, ObservationAttributeMapping.resolveAttributesUnmapped(), source = null)
    }

    context(attributeStore: IMappedAttributeValueLookup)
    override fun observePreferred(
        double: Double,
        long: Long,
    ) {
        resolvedRecorder.observePreferred(
            double,
            long,
            ObservationAttributeMapping.resolveAttributesUnmapped(),
            source = null
        )
    }

}
