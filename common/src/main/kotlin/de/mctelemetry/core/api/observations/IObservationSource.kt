package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
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

    fun fromNbt(tag: Tag?): I
    fun toNbt(instance: I): Tag?
}
