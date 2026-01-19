package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceInstance

class ObservationMappingResolver(
    val resolvedRecorder: IObservationRecorder.Resolved,
    var mapping: ObservationAttributeMapping,
) : IObservationRecorder.Unresolved {

    override val supportsFloating: Boolean = resolvedRecorder.supportsFloating

    context(attributeStore: IAttributeValueStore)
    override fun observe(value: Double, sourceInstance: IObservationSourceInstance<*, *, *>) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(), sourceInstance)
    }

    context(attributeStore: IAttributeValueStore)
    override fun observe(value: Long, sourceInstance: IObservationSourceInstance<*, *, *>) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(), sourceInstance)
    }

    context(attributeStore: IAttributeValueStore)
    override fun observePreferred(
        double: Double,
        long: Long,
        sourceInstance: IObservationSourceInstance<*, *, *>,
    ) {
        resolvedRecorder.observePreferred(double, long, mapping.resolveAttributes(), sourceInstance)
    }
}
