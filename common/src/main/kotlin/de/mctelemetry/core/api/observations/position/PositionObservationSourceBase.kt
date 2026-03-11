package de.mctelemetry.core.api.observations.position

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.ObservationSourceBase
import de.mctelemetry.core.api.observations.position.IPositionObservationSourceInstance.Companion.defaultFacingAccessor
import de.mctelemetry.core.api.observations.position.IPositionObservationSourceInstance.Companion.observeDefaultImpl
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import de.mctelemetry.core.persistence.DirectUnitCodec
import de.mctelemetry.core.utils.EmptyAutoCloseable
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.block.entity.BlockEntity

abstract class PositionObservationSourceBase<
        SO : BlockEntity,
        I : IPositionObservationSourceInstance<SO, *, I>
        > : ObservationSourceBase<SO, I>(),
    IPositionObservationSource<SO, I> {

    final override val observedPosition =
        BuiltinAttributeKeyTypes.GlobalPosType.createObservationAttributeReference("pos")

    open fun getFacingDirection(sourceOwner: BlockEntity): Direction? {
        return defaultFacingAccessor(sourceOwner)
    }

    abstract class PositionInstanceBase<SO: BlockEntity, OC: AutoCloseable, out I : PositionInstanceBase<SO, OC, I>>(
        override val source: PositionObservationSourceBase<SO, out I>
    ) : InstanceBase<SO, OC, I>(source),
        IPositionObservationSourceInstance<SO, OC, I>
    {

        context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.MapAttributeStore)
        final override fun observe(
            recorder: IObservationRecorder.Unresolved,
            unusedAttributes: Set<AttributeDataSource<*>>
        ) {
            observeDefaultImpl(recorder, unusedAttributes, source::getFacingDirection)
        }

        abstract class Simple<out I : Simple<I>>(source: PositionObservationSourceBase<BlockEntity, out I>) :
            PositionInstanceBase<BlockEntity, EmptyAutoCloseable, I>(source) {
            context(sourceOwner: BlockEntity, mapping: ObservationAttributeMapping)
            final override fun createObservationContext(): EmptyAutoCloseable = EmptyAutoCloseable
        }
    }

    abstract class PositionSingletonBase<SO: BlockEntity, OC: AutoCloseable, I : PositionSingletonBase<SO, OC, I>> :
        PositionObservationSourceBase<SO, I>(),
        IPositionObservationSource<SO, I>,
        IPositionObservationSourceInstance<SO, OC, I>,
        IObservationSourceSingleton<SO, OC, I> {

        override val source: PositionSingletonBase<SO, OC, I>
            get() = this

        @Suppress("UNCHECKED_CAST")
        private val typedThis: I
            get() = this as I

        override val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, I> = StreamCodec.unit(typedThis)
        override val codec: Codec<I> = DirectUnitCodec(typedThis)

        abstract class Simple<I : Simple<I>> :
            PositionSingletonBase<BlockEntity, EmptyAutoCloseable, I>() {
            context(sourceOwner: BlockEntity, mapping: ObservationAttributeMapping)
            final override fun createObservationContext(): EmptyAutoCloseable = EmptyAutoCloseable

            override val sourceOwnerType: Class<BlockEntity>
                get() = BlockEntity::class.java
        }
    }
}
