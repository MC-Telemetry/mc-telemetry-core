package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.attributes.ObservationContext
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource

class ObservationMappingResolver(
    val resolvedRecorder: IObservationRecorder.Resolved,
    var mapping: ObservationAttributeMapping,
) : IObservationRecorder.Unresolved {

    override val supportsFloating: Boolean = resolvedRecorder.supportsFloating

    context(observationContext: ObservationContext<*>)
    override fun observe(value: Double, source: IObservationSource<*, *>) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(), source)
    }

    context(observationContext: ObservationContext<*>)
    override fun observe(value: Long, source: IObservationSource<*, *>) {
        resolvedRecorder.observe(value, mapping.resolveAttributes(), source)
    }

    context(observationContext: ObservationContext<*>)
    override fun observePreferred(
        double: Double,
        long: Long,
        source: IObservationSource<*, *>,
    ) {
        resolvedRecorder.observePreferred(double, long, mapping.resolveAttributes(), source)
    }
}
