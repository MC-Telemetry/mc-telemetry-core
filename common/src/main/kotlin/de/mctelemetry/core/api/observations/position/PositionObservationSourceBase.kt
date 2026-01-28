package de.mctelemetry.core.api.observations.position

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.attributes.stores.MapAttributeStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceSingleton
import de.mctelemetry.core.api.observations.ObservationSourceBase
import de.mctelemetry.core.api.observations.position.IPositionObservationSourceInstance.Companion.observeDefaultImpl
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.block.entity.BlockEntity

abstract class PositionObservationSourceBase<
        I : IPositionObservationSourceInstance<MapAttributeStore, I>
        > : ObservationSourceBase<BlockEntity, I>(),
    IPositionObservationSource<I> {

    final override val observedPosition =
        BuiltinAttributeKeyTypes.GlobalPosType.createObservationAttributeReference("pos")

    final override val sourceContextType: Class<BlockEntity> = BlockEntity::class.java

    open fun getFacingDirection(sourceContext: BlockEntity): Direction? {
        return IPositionObservationSourceInstance.Companion.defaultFacingAccessor(sourceContext)
    }

    abstract class PositionInstanceBase<out I : PositionInstanceBase<I>>(
        override val source: PositionObservationSourceBase<out I>
    ) : InstanceBase<BlockEntity, I>(source),
        IPositionObservationSourceInstance<MapAttributeStore, I>
    {

        context(sourceContext: BlockEntity, attributeStore: MapAttributeStore)
        final override fun observe(
            recorder: IObservationRecorder.Unresolved,
            unusedAttributes: Set<AttributeDataSource<*>>
        ) {
            observeDefaultImpl(recorder, unusedAttributes, source::getFacingDirection)
        }

        context(sourceContext: BlockEntity)
        override fun createAttributeStore(parent: IAttributeValueStore): MapAttributeStore {
            return MapAttributeStore(attributes.references, parent)
        }
    }

    abstract class PositionSingletonBase<I : PositionSingletonBase<I>> :
        PositionObservationSourceBase<I>(),
        IPositionObservationSource<I>,
        IPositionObservationSourceInstance<MapAttributeStore, I>,
        IObservationSourceSingleton<BlockEntity, MapAttributeStore, I> {

        override val source: PositionSingletonBase<I>
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
