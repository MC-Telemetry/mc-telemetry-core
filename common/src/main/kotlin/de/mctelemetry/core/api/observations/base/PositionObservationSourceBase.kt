package de.mctelemetry.core.api.observations.base

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties

abstract class PositionObservationSourceBase : ObservationSourceBase<BlockEntity>() {
    val observedPosition = BuiltinAttributeKeyTypes.GlobalPosType.createObservationAttributeReference("pos")

    final override val sourceContextType: Class<BlockEntity> = BlockEntity::class.java

    open fun getFacingDirection(sourceContext: BlockEntity): Direction? {
        val state = sourceContext.blockState
        if(state.hasProperty(BlockStateProperties.FACING))
            return state.getValue(BlockStateProperties.FACING)
        else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING)
        return null
    }

    context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.MapLookup)
    final override fun observe(recorder: IObservationRecorder.Unresolved, unusedAttributes: Set<AttributeDataSource<*>>) {
        val level = sourceContext.level
        if (level == null || sourceContext.isRemoved) return
        val scraperPos = sourceContext.blockPos
        if (!(level.isLoaded(scraperPos) && level.shouldTickBlocksAt(scraperPos))) return
        val facing = getFacingDirection(sourceContext)
        val observationPos: BlockPos
        if(facing != null) {
            observationPos = scraperPos.relative(facing)
            if (!level.isLoaded(observationPos)) return
        }
        else {
            observationPos = scraperPos
        }
        observedPosition.set(GlobalPos(level.dimension(), observationPos))
        observePosition(recorder, level, observationPos, facing, unusedAttributes)
    }

    context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.MapLookup)
    abstract fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: Level,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    )

    abstract class PositionSideObservationSourceBase(val directions: Set<Direction> = directionsDefault) :
        PositionObservationSourceBase() {

        val observedSide = BuiltinAttributeKeyTypes.DirectionType.createObservationAttributeReference("dir")

        companion object {
            private val directionsDefault = Direction.entries.toSet()
        }

        context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.MapLookup)
        override fun observePosition(
            recorder: IObservationRecorder.Unresolved,
            level: Level,
            position: BlockPos,
            facing: Direction?,
            unusedAttributes: Set<AttributeDataSource<*>>
        ) {
            if (observedSide in unusedAttributes) {
                observedSide.unset()
                observeUnsided(recorder, level, position, facing, unusedAttributes)
            } else {
                for (direction in directions) {
                    observedSide.set(direction)
                    observeSide(recorder, level, position, direction, unusedAttributes)
                }
            }
        }

        context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.MapLookup)
        open fun observeUnsided(
            recorder: IObservationRecorder.Unresolved,
            level: Level,
            position: BlockPos,
            facing: Direction?,
            unusedAttributes: Set<AttributeDataSource<*>>
        ) {
            observeSide(recorder, level, position, (facing ?: Direction.UP).opposite, unusedAttributes)
        }

        context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.MapLookup)
        abstract fun observeSide(
            recorder: IObservationRecorder.Unresolved,
            level: Level,
            position: BlockPos,
            side: Direction,
            unusedAttributes: Set<AttributeDataSource<*>>
        )
    }
}
