package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import net.minecraft.nbt.Tag

interface IObservationSourceSingleton<
        SC,
        AS : IAttributeValueStore.Mutable,
        I : IObservationSourceSingleton<SC, AS, I>
        > :
    IObservationSource<SC, I>,
    IObservationSourceInstance<SC, AS> {

    override val attributes: IAttributeDateSourceReferenceSet

    override fun fromNbt(tag: Tag?): I

    override fun toNbt(instance: I): Tag?

    override val source: IObservationSourceSingleton<SC, AS, I>
        get() = this
}
