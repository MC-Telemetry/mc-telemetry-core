package de.mctelemetry.core.api.observations.position.side

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import de.mctelemetry.core.persistence.DirectUnitCodec
import de.mctelemetry.core.utils.EmptyAutoCloseable
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.block.entity.BlockEntity

abstract class PositionSideObservationSourceBase<
        SO : BlockEntity,
        I : IPositionSideObservationSourceInstance<SO, *, I>
        > : PositionObservationSourceBase<SO, I>(),
    IPositionSideObservationSource<SO, I> {

    final override val observedSide: AttributeDataSource.Reference.ObservationSourceAttributeReference<Direction> =
        BuiltinAttributeKeyTypes.DirectionType.createObservationAttributeReference("dir")

    abstract class PositionSideInstanceBase<SO : BlockEntity, OC : AutoCloseable, out I : PositionSideInstanceBase<SO, OC, I>>(
        override val source: PositionSideObservationSourceBase<SO, out I>
    ) : PositionInstanceBase<SO, OC, I>(source),
        IPositionSideObservationSourceInstance<SO, OC, I> {

        abstract class Simple<out I : Simple<I>>(source: PositionSideObservationSourceBase<BlockEntity, out I>) :
            PositionSideInstanceBase<BlockEntity, EmptyAutoCloseable, I>(source) {
            context(sourceOwner: BlockEntity, mapping: ObservationAttributeMapping)
            final override fun createObservationContext(): EmptyAutoCloseable = EmptyAutoCloseable
        }
    }

    abstract class PositionSideSingletonBase<SO: BlockEntity, OC: AutoCloseable, I : PositionSideSingletonBase<SO, OC, I>> :
        PositionSideObservationSourceBase<SO, I>(),
        IPositionSideObservationSource<SO, I>,
        IPositionSideObservationSourceInstance<SO, OC, I>,
        IObservationSourceSingleton<SO, OC, I> {

        override val source: PositionSideSingletonBase<SO, OC, I>
            get() = this

        @Suppress("UNCHECKED_CAST")
        private val typedThis: I
            get() = this as I

        override val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, I> = StreamCodec.unit(typedThis)
        override val codec: Codec<I> = DirectUnitCodec(typedThis)

        abstract class Simple<I : Simple<I>> :
            PositionSideSingletonBase<BlockEntity, EmptyAutoCloseable, I>() {
            context(sourceOwner: BlockEntity, mapping: ObservationAttributeMapping)
            final override fun createObservationContext(): EmptyAutoCloseable = EmptyAutoCloseable

            override val sourceOwnerType: Class<BlockEntity>
                get() = BlockEntity::class.java
        }
    }
}
