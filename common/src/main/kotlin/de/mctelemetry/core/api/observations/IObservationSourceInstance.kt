package de.mctelemetry.core.api.observations

import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

interface IObservationSourceInstance<
        SO,
        OC : AutoCloseable,
        out I : IObservationSourceInstance<SO, OC, I>
        > {
    val source: IObservationSource<SO, out I>

    val attributes: IAttributeDateSourceReferenceSet
        get() = source.attributes

    context(sourceOwner: SO, mapping: ObservationAttributeMapping)
    fun createObservationContext(): OC

    context(sourceOwner: SO, observationContext: OC)
    fun createAttributeStore(parent: IAttributeValueStore): IAttributeValueStore.Mutable {
        return IAttributeValueStore.MapAttributeStore(attributes.references, parent)
    }

    context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.Mutable)
    fun observe(
        recorder: IObservationRecorder.Unresolved,
        unusedAttributes: Set<AttributeDataSource<*>>,
    )

    companion object {

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IObservationSourceInstance<*, *, *>> =
            object : StreamCodec<RegistryFriendlyByteBuf, IObservationSourceInstance<*, *, *>> {
                override fun encode(`object`: RegistryFriendlyByteBuf, object2: IObservationSourceInstance<*, *, *>) {
                    IObservationSource.STREAM_CODEC.encode(`object`, object2.source)
                    object2.encode(`object`)
                }

                override fun decode(`object`: RegistryFriendlyByteBuf): IObservationSourceInstance<*, *, *> {
                    val source = IObservationSource.STREAM_CODEC.decode(`object`)
                    return source.streamCodec.decode(`object`)
                }
            }
    }
}

val <SO> IObservationSourceInstance<SO, *, *>.sourceContextType: Class<SO>
    get() = source.sourceOwnerType

@Suppress("UNCHECKED_CAST")
context(ops: DynamicOps<T>)
fun <T, I : IObservationSourceInstance<*, *, I>> I.encode(prefix: T = ops.empty()): DataResult<T> =
    (source as IObservationSource<*, I>).codec.encode(this, ops, prefix)

@Suppress("UNCHECKED_CAST")
fun <I : IObservationSourceInstance<*, *, I>> I.encode(`object`: RegistryFriendlyByteBuf) {
    (source as IObservationSource<*, I>).streamCodec.encode(`object`, this)
}
