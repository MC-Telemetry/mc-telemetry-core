package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import net.minecraft.nbt.Tag

interface IObservationSourceInstance<
        SC,
        AS : IAttributeValueStore.Mutable
        > {
    val source: IObservationSource<SC, out IObservationSourceInstance<SC,AS>>

    val attributes: IAttributeDateSourceReferenceSet
        get() = source.attributes

    context(sourceContext: SC)
    fun createAttributeStore(parent: IAttributeValueStore): AS

    context(sourceContext: SC, attributeStore: AS)
    fun observe(
        recorder: IObservationRecorder.Unresolved,
        unusedAttributes: Set<AttributeDataSource<*>>,
    )
}

val <SC> IObservationSourceInstance<SC, *>.sourceContextType: Class<SC>
    get() = source.sourceContextType

@Suppress("UNCHECKED_CAST")
fun <I : IObservationSourceInstance<*,*>> I.toNbt(): Tag? = (source as IObservationSource<*,I>).toNbt(this)
