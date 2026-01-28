package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

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
            StreamCodec.composite(
                ByteBufCodecs.registry(OTelCoreModAPI.ObservationSources),
                IObservationSourceInstance<*, *, *>::source,
                ByteBufCodecs.optional(ByteBufCodecs.TAG),
                { Optional.ofNullable(it.toNbt()) },
            ) { source, data ->
                source.fromNbt(data.getOrNull())
            }
    }
}

val <SC> IObservationSourceInstance<SC, *, *>.sourceContextType: Class<SC>
    get() = source.sourceContextType

@Suppress("UNCHECKED_CAST")
fun <I : IObservationSourceInstance<*, *, I>> I.toNbt(): Tag? = (source as IObservationSource<*, I>).toNbt(this)
