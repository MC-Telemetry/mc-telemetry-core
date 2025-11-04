package de.mctelemetry.core.api.metrics

import net.minecraft.resources.ResourceKey

interface IObservationSource<C, A : IMappedAttributeValueLookup> {

    val id: ResourceKey<IObservationSource<*, *>>

    val contextType: Class<C>

    fun createAttributeLookup(context: C, attributes: IMappedAttributeValueLookup): A

    fun observe(
        context: C,
        recorder: IObservationRecorder.Unresolved,
        attributes: A,
        unusedAttributes: Set<MappedAttributeKeyInfo<*, *>>,
    )

    interface Simple<C> : IObservationSource<C, IMappedAttributeValueLookup> {

        override fun createAttributeLookup(
            context: C,
            attributes: IMappedAttributeValueLookup,
        ): IMappedAttributeValueLookup = attributes
    }
}
