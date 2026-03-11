package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeValueStore

interface IObservationSourceSingleton<
        SO,
        OC : AutoCloseable,
        I : IObservationSourceSingleton<SO, OC, I>
        > :
    IObservationSource<SO, I>,
    IObservationSourceInstance<SO, OC, I> {

    override val attributes: IAttributeDateSourceReferenceSet

    override val source: IObservationSourceSingleton<SO, OC, I>
        get() = this
}
