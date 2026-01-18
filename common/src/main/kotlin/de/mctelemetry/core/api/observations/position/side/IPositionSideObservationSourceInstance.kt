package de.mctelemetry.core.api.observations.position.side

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.api.observations.position.IPositionObservationSource
import de.mctelemetry.core.api.observations.position.IPositionObservationSourceInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity

interface IPositionSideObservationSourceInstance<
        AS : IAttributeValueStore.Mutable
        > : IPositionObservationSourceInstance<AS> {

    override val source: IPositionSideObservationSource<out IPositionSideObservationSourceInstance<AS>>

    val directions: Iterable<Direction>
        get() = Direction.entries

    context(sourceContext: BlockEntity, attributeStore: AS)
    fun observeUnsided(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        observeSide(recorder, level, position, (facing ?: Direction.UP).opposite, unusedAttributes)
    }

    context(sourceContext: BlockEntity, attributeStore: AS)
    fun observeSide(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        side: Direction,
        unusedAttributes: Set<AttributeDataSource<*>>
    )

    context(sourceContext: BlockEntity, attributeStore: AS)
    override fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        if (source.observedSide in unusedAttributes) {
            source.observedSide.unset()
            observeUnsided(recorder, level, position, facing, unusedAttributes)
        } else {
            for (direction in directions) {
                source.observedSide.set(direction)
                observeSide(recorder, level, position, direction, unusedAttributes)
            }
        }
    }
}
