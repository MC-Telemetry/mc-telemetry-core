package de.mctelemetry.core.api.observations

import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

interface IObservationSourceInstance<
        SC,
        AS : IAttributeValueStore.Mutable,
        out I : IObservationSourceInstance<SC, AS, I>
        > {
    val source: IObservationSource<SC, out I>

    val attributes: IAttributeDateSourceReferenceSet
        get() = source.attributes

    context(sourceContext: SC)
    fun createAttributeStore(parent: IAttributeValueStore): AS

    context(sourceContext: SC, attributeStore: AS)
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

val <SC> IObservationSourceInstance<SC, *, *>.sourceContextType: Class<SC>
    get() = source.sourceContextType

@Suppress("UNCHECKED_CAST")
context(ops: DynamicOps<T>)
fun <T, I : IObservationSourceInstance<*, *, I>> I.encode(prefix: T = ops.empty()): DataResult<T> =
    (source as IObservationSource<*, I>).codec.encode(this, ops, prefix)

@Suppress("UNCHECKED_CAST")
fun <I : IObservationSourceInstance<*, *, I>> I.encode(`object`: RegistryFriendlyByteBuf) {
    (source as IObservationSource<*, I>).streamCodec.encode(`object`, this)
}
