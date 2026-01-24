package de.mctelemetry.core.api.observations

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.persistence.RegistryIdFieldCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey

interface IObservationSource<
        SC,
        I : IObservationSourceInstance<SC, *, I>
        > {

    val id: ResourceKey<IObservationSource<*, *>>

    val attributes: IAttributeDateSourceReferenceSet

    val sourceContextType: Class<SC>

    val streamCodec: StreamCodec<RegistryFriendlyByteBuf, I>

    val codec: Codec<I>

    companion object {
        val CODEC: Codec<IObservationSource<*, *>> = RegistryIdFieldCodec(
            OTelCoreModAPI.ObservationSources,
            IObservationSource<*, *>::id
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IObservationSource<*, *>> = ByteBufCodecs.registry(
            OTelCoreModAPI.ObservationSources
        )
    }
}
