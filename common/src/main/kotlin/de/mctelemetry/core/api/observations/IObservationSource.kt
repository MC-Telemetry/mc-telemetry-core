package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.ObservationContext
import de.mctelemetry.core.api.attributes.IMappedAttributeKeySet
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import net.minecraft.resources.ResourceKey

interface IObservationSource<SC, OC : ObservationContext<*>> {

    val id: ResourceKey<IObservationSource<*, *>>

    val attributes: IMappedAttributeKeySet

    val sourceContextType: Class<SC>

    context(sourceContext: SC)
    fun createObservationContext(lookup: IMappedAttributeValueLookup): OC

    context(sourceContext: SC, observationContext: OC)
    fun observe(
        recorder: IObservationRecorder.Unresolved,
        unusedAttributes: Set<MappedAttributeKeyInfo<*, *>>,
    )

    interface Simple<SC> : IObservationSource<SC, ObservationContext<*>> {

        context(sourceContext: SC)
        override fun createObservationContext(
            lookup: IMappedAttributeValueLookup,
        ): ObservationContext<*> {
            val ownAttributes = this.attributes
            return ObservationContext(
                if (ownAttributes.attributeKeys.isEmpty())
                    lookup
                else
                    IMappedAttributeValueLookup.MapLookup(
                        data = ownAttributes.attributeKeys.associateWith { null },
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
                    data = this.attributes.attributeKeys.associateWith { null },
                    parent = lookup
                )
            )
        }
    }

    interface SingleAttribute<SC, T : Any> : IObservationSource<SC, ObservationContext<IMappedAttributeValueLookup.PairLookup<T>>> {

        val attributeKey: MappedAttributeKeyInfo<T, *>

        override val attributes: IMappedAttributeKeySet
            get() = IMappedAttributeKeySet(attributeKey)

        context(sourceContext: SC)
        override fun createObservationContext(
            lookup: IMappedAttributeValueLookup,
        ): ObservationContext<IMappedAttributeValueLookup.PairLookup<T>> {
            return ObservationContext(
                IMappedAttributeValueLookup.PairLookup(
                    attributeKey,
                    null,
                    parent = lookup
                )
            )
        }
    }
}
