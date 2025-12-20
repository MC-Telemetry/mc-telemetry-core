package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.ObservationContext
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import net.minecraft.resources.ResourceKey

interface IObservationSource<SC, OC : ObservationContext<*>> {

    val id: ResourceKey<IObservationSource<*, *>>

    val attributes: IAttributeDateSourceReferenceSet

    val sourceContextType: Class<SC>

    context(sourceContext: SC)
    fun createObservationContext(lookup: IMappedAttributeValueLookup): OC

    context(sourceContext: SC, observationContext: OC)
    fun observe(
        recorder: IObservationRecorder.Unresolved,
        unusedAttributes: Set<AttributeDataSource<*>>,
    )

    interface Simple<SC> : IObservationSource<SC, ObservationContext<*>> {

        context(sourceContext: SC)
        override fun createObservationContext(
            lookup: IMappedAttributeValueLookup,
        ): ObservationContext<*> {
            val ownAttributes = this.attributes
            return ObservationContext(
                if (ownAttributes.references.isEmpty())
                    lookup
                else
                    IMappedAttributeValueLookup.MapLookup(
                        data = ownAttributes.references.associateWith { null },
                        parent = lookup
                    )
            )
        }
    }

    interface MultiAttribute<SC> : IObservationSource<SC, ObservationContext<IMappedAttributeValueLookup.MapLookup>> {

        context(sourceContext: SC)
        override fun createObservationContext(
            lookup: IMappedAttributeValueLookup,
        ): ObservationContext<IMappedAttributeValueLookup.MapLookup> {
            return ObservationContext(
                IMappedAttributeValueLookup.MapLookup(
                    data = this.attributes.references.associateWith { null },
                    parent = lookup
                )
            )
        }
    }

    interface SingleAttribute<SC, T : Any> : IObservationSource<SC, ObservationContext<IMappedAttributeValueLookup.PairLookup<T>>> {

        val reference: AttributeDataSource.ObservationSourceAttributeReference<T>

        override val attributes: IAttributeDateSourceReferenceSet
            get() = IAttributeDateSourceReferenceSet(listOf(reference))

        context(sourceContext: SC)
        override fun createObservationContext(
            lookup: IMappedAttributeValueLookup,
        ): ObservationContext<IMappedAttributeValueLookup.PairLookup<T>> {
            return ObservationContext(
                IMappedAttributeValueLookup.PairLookup(
                    reference,
                    null,
                    parent = lookup
                )
            )
        }
    }
}
