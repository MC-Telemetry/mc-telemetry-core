package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IMappedAttributeKeySet
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import net.minecraft.resources.ResourceKey

interface IObservationSource<C, A : IMappedAttributeValueLookup> {

    val id: ResourceKey<IObservationSource<*, *>>

    val attributes: IMappedAttributeKeySet

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
        ): IMappedAttributeValueLookup {
            val ownAttributes = this.attributes
            return if (ownAttributes.attributeKeys.isEmpty())
                attributes
            else
                IMappedAttributeValueLookup.MapLookup(
                    data = ownAttributes.attributeKeys.associateWith { null },
                    parent = attributes
                )
        }
    }

    interface MultiAttribute<C> : IObservationSource<C, IMappedAttributeValueLookup.MapLookup> {

        override fun createAttributeLookup(
            context: C,
            attributes: IMappedAttributeValueLookup,
        ): IMappedAttributeValueLookup.MapLookup {
            return IMappedAttributeValueLookup.MapLookup(
                data = this.attributes.attributeKeys.associateWith { null },
                parent = attributes
            )
        }
    }

    interface SingleAttribute<C, T : Any> : IObservationSource<C, IMappedAttributeValueLookup.PairLookup<T>> {

        val attributeKey: MappedAttributeKeyInfo<T, *>

        override val attributes: IMappedAttributeKeySet
            get() = IMappedAttributeKeySet(attributeKey)

        override fun createAttributeLookup(
            context: C,
            attributes: IMappedAttributeValueLookup,
        ): IMappedAttributeValueLookup.PairLookup<T> {
            return IMappedAttributeValueLookup.PairLookup(
                attributeKey,
                null,
                parent = attributes
            )
        }
    }
}
