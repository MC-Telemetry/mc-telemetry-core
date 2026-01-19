package de.mctelemetry.core.api.observations.position

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties

interface IPositionObservationSourceInstance<
        AS : IAttributeValueStore.Mutable,
        out I : IPositionObservationSourceInstance<AS, I>,
        > : IObservationSourceInstance<BlockEntity, AS, I> {


    override val source: IPositionObservationSource<out I>

    context(sourceContext: BlockEntity, attributeStore: AS)
    fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    )

    context(sourceContext: BlockEntity, attributeStore: AS)
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

        context(sourceContext: BlockEntity, attributeStore: AS)
        protected inline fun <AS : IAttributeValueStore.Mutable> IPositionObservationSourceInstance<AS, *>.observeDefaultImpl(
            recorder: IObservationRecorder.Unresolved,
            unusedAttributes: Set<AttributeDataSource<*>>,
            facingAccessor: (BlockEntity) -> Direction? = ::defaultFacingAccessor,
        ) {
            val level = sourceContext.level
            if (level == null || sourceContext.isRemoved) return
            if (level !is ServerLevel) throw IllegalArgumentException("Observed entity is part of a non-server level: $level")
            val scraperPos = sourceContext.blockPos
            if (!(level.isLoaded(scraperPos) && level.shouldTickBlocksAt(scraperPos))) return
            val facing = facingAccessor(sourceContext)
            val observationPos: BlockPos
            if (facing != null) {
                observationPos = scraperPos.relative(facing)
                if (!level.isLoaded(observationPos)) return
            } else {
                observationPos = scraperPos
            }
            source.observedPosition.set(GlobalPos(level.dimension(), observationPos))
            observePosition(recorder, level, observationPos, facing, unusedAttributes)
        }
    }
}
