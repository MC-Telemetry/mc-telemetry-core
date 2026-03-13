package de.mctelemetry.core.api.observations.position

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.utils.withValue
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties

interface IPositionObservationSourceInstance<
        SO: BlockEntity,
        OC: AutoCloseable,
        out I : IPositionObservationSourceInstance<SO, OC, I>,
        > : IObservationSourceInstance<SO, OC, I> {


    override val source: IPositionObservationSource<SO, out I>

    context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.Mutable)
    fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    )

    context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.Mutable)
    override fun observe(recorder: IObservationRecorder.Unresolved, unusedAttributes: Set<AttributeDataSource<*>>) {
        observeDefaultImpl(recorder, unusedAttributes)
    }

    companion object {
        fun defaultFacingAccessor(entity: BlockEntity): Direction? {
            val state = entity.blockState
            if (state.hasProperty(BlockStateProperties.FACING))
                return state.getValue(BlockStateProperties.FACING)
            else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                return state.getValue(BlockStateProperties.HORIZONTAL_FACING)
            return null
        }

        context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.Mutable)
        protected inline fun <SO: BlockEntity, OC: AutoCloseable> IPositionObservationSourceInstance<SO, OC, *>.observeDefaultImpl(
            recorder: IObservationRecorder.Unresolved,
            unusedAttributes: Set<AttributeDataSource<*>>,
            facingAccessor: (BlockEntity) -> Direction? = ::defaultFacingAccessor,
        ) {
            val level = sourceOwner.level
            if (level == null || sourceOwner.isRemoved) return
            if (level !is ServerLevel) throw IllegalArgumentException("Observed entity is part of a non-server level: $level")
            val scraperPos = sourceOwner.blockPos
            if (!(level.isLoaded(scraperPos) && level.shouldTickBlocksAt(scraperPos))) return
            val facing = facingAccessor(sourceOwner)
            val observationPos: BlockPos
            if (facing != null) {
                observationPos = scraperPos.relative(facing)
                if (!level.isLoaded(observationPos)) return
            } else {
                observationPos = scraperPos
            }
            source.observedPosition.withValue(GlobalPos(level.dimension(), observationPos)) {
                observePosition(recorder, level, observationPos, facing, unusedAttributes)
            }
        }
    }
}
