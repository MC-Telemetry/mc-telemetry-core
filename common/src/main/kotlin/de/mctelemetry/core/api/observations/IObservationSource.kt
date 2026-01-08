package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import net.minecraft.resources.ResourceKey

interface IObservationSource<SC, AS : IMappedAttributeValueLookup> {

    val id: ResourceKey<IObservationSource<*, *>>

    val attributes: IAttributeDateSourceReferenceSet

    val sourceContextType: Class<SC>

    context(sourceContext: SC)
    fun createAttributeStore(parent: IMappedAttributeValueLookup): AS

    context(sourceContext: SC, attributeStore: AS)
    fun observe(
        recorder: IObservationRecorder.Unresolved,
        unusedAttributes: Set<AttributeDataSource<*>>,
    )

    interface Simple<SC> : IObservationSource<SC, IMappedAttributeValueLookup> {

        context(sourceContext: SC)
        override fun createAttributeStore(
            parent: IMappedAttributeValueLookup,
        ): IMappedAttributeValueLookup {
            val ownAttributes = this.attributes
            return if (ownAttributes.references.isEmpty())
                parent
            else
                IMappedAttributeValueLookup.MapLookup(
                    data = ownAttributes.references,
                    parent = parent
                )
        }
    }

    interface MultiAttribute<SC> : IObservationSource<SC, IMappedAttributeValueLookup.MapLookup> {

        context(sourceContext: SC)
        override fun createAttributeStore(
            parent: IMappedAttributeValueLookup,
        ): IMappedAttributeValueLookup.MapLookup {
            return IMappedAttributeValueLookup.MapLookup(
                data = this.attributes.references,
                parent = parent
            )
        }
    }

    interface SingleAttribute<SC, T : Any> : IObservationSource<SC, IMappedAttributeValueLookup.PairLookup<T>> {

        val reference: AttributeDataSource.Reference<T>

        override val attributes: IAttributeDateSourceReferenceSet
            get() = IAttributeDateSourceReferenceSet(listOf(reference))

        context(sourceContext: SC)
        override fun createAttributeStore(
            parent: IMappedAttributeValueLookup,
        ): IMappedAttributeValueLookup.PairLookup<T> {
            return IMappedAttributeValueLookup.PairLookup(
                reference,
                null,
                parent = parent
            )
        }
    }
}
