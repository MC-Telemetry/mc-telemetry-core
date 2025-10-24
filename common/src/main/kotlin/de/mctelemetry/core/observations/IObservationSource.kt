package de.mctelemetry.core.observations

import de.mctelemetry.core.api.metrics.IObservationObserver
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import net.minecraft.resources.ResourceKey

interface IObservationSource<C, A : IMappedAttributeValueLookup> {

    val id: ResourceKey<IObservationSource<*, *>>

    val contextType: Class<C>

    fun createAttributeLookup(context: C, attributes: IMappedAttributeValueLookup): A

    fun observe(
        context: C,
        observer: IObservationObserver.Unresolved,
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
