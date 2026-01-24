package de.mctelemetry.core.api.observations.position.side

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import de.mctelemetry.core.persistence.DirectUnitCodec
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.block.entity.BlockEntity

abstract class PositionSideObservationSourceBase<
        I : IPositionSideObservationSourceInstance<IAttributeValueStore.MapAttributeStore, I>
        > : PositionObservationSourceBase<I>(),
    IPositionSideObservationSource<I> {

    final override val observedSide: AttributeDataSource.Reference.ObservationSourceAttributeReference<Direction> =
        BuiltinAttributeKeyTypes.DirectionType.createObservationAttributeReference("dir")

    abstract class PositionSideInstanceBase<I : PositionSideInstanceBase<I>>(
        override val source: PositionSideObservationSourceBase<I>
    ) : PositionInstanceBase<I>(source),
        IPositionSideObservationSourceInstance<IAttributeValueStore.MapAttributeStore, I>

    abstract class PositionSideSingletonBase<I : PositionSideSingletonBase<I>> :
        PositionSideObservationSourceBase<I>(),
        IPositionSideObservationSource<I>,
        IPositionSideObservationSourceInstance<IAttributeValueStore.MapAttributeStore, I>,
        IObservationSourceSingleton<BlockEntity, IAttributeValueStore.MapAttributeStore, I> {

        override val source: PositionSideSingletonBase<I>
            get() = this

        @Suppress("UNCHECKED_CAST")
        private val typedThis: I
            get() = this as I

        override val streamCodec: StreamCodec<RegistryFriendlyByteBuf, I> = StreamCodec.unit(typedThis)
        override val codec: Codec<I> = DirectUnitCodec(typedThis)

        context(sourceContext: BlockEntity)
        override fun createAttributeStore(parent: IAttributeValueStore): IAttributeValueStore.MapAttributeStore {
            return IAttributeValueStore.MapAttributeStore(attributes.references, parent)
        }
    }
}
