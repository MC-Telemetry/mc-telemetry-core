package de.mctelemetry.core.api.observations.position.side

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.block.entity.BlockEntity

abstract class PositionSideObservationSourceBase<
        I : IPositionSideObservationSourceInstance<IAttributeValueStore.MapAttributeStore>
        > : PositionObservationSourceBase<I>(),
    IPositionSideObservationSource<I> {

    final override val observedSide: AttributeDataSource.Reference.ObservationSourceAttributeReference<Direction> =
        BuiltinAttributeKeyTypes.DirectionType.createObservationAttributeReference("dir")

    abstract class PositionSideInstanceBase<I : PositionSideInstanceBase<I>>(
        override val source: PositionSideObservationSourceBase<I>
    ) : PositionInstanceBase<PositionSideInstanceBase<I>>(source),
        IPositionSideObservationSourceInstance<IAttributeValueStore.MapAttributeStore>

    abstract class PositionSideSingletonBase<I : PositionSideSingletonBase<I>> :
        PositionSideObservationSourceBase<PositionSideSingletonBase<I>>(),
        IPositionSideObservationSource<PositionSideSingletonBase<I>>,
        IPositionSideObservationSourceInstance<IAttributeValueStore.MapAttributeStore>,
        IObservationSourceSingleton<BlockEntity, IAttributeValueStore.MapAttributeStore, PositionSideSingletonBase<I>> {

        override val source: PositionSideSingletonBase<I>
            get() = this

        override val streamCodec: StreamCodec<RegistryFriendlyByteBuf, PositionSideSingletonBase<I>> =
            StreamCodec.unit(this)

        override fun fromNbt(tag: Tag?): PositionSideSingletonBase<I> = this
        override fun toNbt(instance: PositionSideSingletonBase<I>): Tag? = null

        context(sourceContext: BlockEntity)
        override fun createAttributeStore(parent: IAttributeValueStore): IAttributeValueStore.MapAttributeStore {
            return IAttributeValueStore.MapAttributeStore(attributes.references, parent)
        }
    }
}
