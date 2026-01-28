package de.mctelemetry.core.api.observations.position.side

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.attributes.stores.MapAttributeStore
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.block.entity.BlockEntity

abstract class PositionSideObservationSourceBase<
        I : IPositionSideObservationSourceInstance<MapAttributeStore, I>
        > : PositionObservationSourceBase<I>(),
    IPositionSideObservationSource<I> {

    final override val observedSide: AttributeDataSource.Reference.ObservationSourceAttributeReference<Direction> =
        BuiltinAttributeKeyTypes.DirectionType.createObservationAttributeReference("dir")

    abstract class PositionSideInstanceBase<I : PositionSideInstanceBase<I>>(
        override val source: PositionSideObservationSourceBase<I>
    ) : PositionInstanceBase<I>(source),
        IPositionSideObservationSourceInstance<MapAttributeStore, I>

    abstract class PositionSideSingletonBase<I : PositionSideSingletonBase<I>> :
        PositionSideObservationSourceBase<I>(),
        IPositionSideObservationSource<I>,
        IPositionSideObservationSourceInstance<MapAttributeStore, I>,
        IObservationSourceSingleton<BlockEntity, MapAttributeStore, I> {

        override val source: PositionSideSingletonBase<I>
            get() = this

        @Suppress("UNCHECKED_CAST")
        private val typedThis: I
            get() = this as I

        override val streamCodec: StreamCodec<RegistryFriendlyByteBuf, I> =
            StreamCodec.unit(typedThis)

        override fun fromNbt(tag: Tag?): I = typedThis
        override fun toNbt(instance: I): Tag? = null

        context(sourceContext: BlockEntity)
        override fun createAttributeStore(parent: IAttributeValueStore): MapAttributeStore {
            return MapAttributeStore(attributes.references, parent)
        }
    }
}
