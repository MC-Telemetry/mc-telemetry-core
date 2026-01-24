package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeValueStore

interface IObservationSourceSingleton<
        SC,
        AS : IAttributeValueStore.Mutable,
        I : IObservationSourceSingleton<SC, AS, I>
        > :
    IObservationSource<SC, I>,
    IObservationSourceInstance<SC, AS, I> {

    override val attributes: IAttributeDateSourceReferenceSet

    override val source: IObservationSourceSingleton<SC, AS, I>
        get() = this
}
