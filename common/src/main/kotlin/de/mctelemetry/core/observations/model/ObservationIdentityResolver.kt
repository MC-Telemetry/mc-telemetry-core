package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceInstance

class ObservationIdentityResolver(
    val resolvedRecorder: IObservationRecorder.Resolved,
) : IObservationRecorder.Unresolved.Sourceless {

    override val supportsFloating: Boolean = resolvedRecorder.supportsFloating

    context(attributeStore: IAttributeValueStore)
    override fun observe(value: Double) {
        resolvedRecorder.observe(value, ObservationAttributeMapping.resolveAttributesUnmapped(), sourceInstance = null)
    }

    context(attributeStore: IAttributeValueStore)
    override fun observe(value: Long) {
        resolvedRecorder.observe(value, ObservationAttributeMapping.resolveAttributesUnmapped(), sourceInstance = null)
    }

    context(attributeStore: IAttributeValueStore)
    override fun observePreferred(
        double: Double,
        long: Long,
    ) {
        resolvedRecorder.observePreferred(
            double,
            long,
            ObservationAttributeMapping.resolveAttributesUnmapped(),
            sourceInstance = null
        )
    }

    override fun onNewSource(source: IObservationSourceInstance<*, *>) {
        resolvedRecorder.onNewSource(source)
    }

}
