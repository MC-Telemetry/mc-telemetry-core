package de.mctelemetry.core.api.observations.position.side

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.position.IPositionObservationSourceInstance
import de.mctelemetry.core.utils.withValue
import de.mctelemetry.core.utils.withoutValue
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity

interface IPositionSideObservationSourceInstance<
        SO: BlockEntity,
        OC: AutoCloseable,
        out I : IPositionSideObservationSourceInstance<SO, OC, I>,
        > : IPositionObservationSourceInstance<SO, OC, I> {

    override val source: IPositionSideObservationSource<SO, out I>

    val directions: Iterable<Direction>
        get() = Direction.entries

    context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.MapAttributeStore)
    fun observeUnsided(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        observeSide(recorder, level, position, (facing ?: Direction.UP).opposite, unusedAttributes)
    }

    context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.MapAttributeStore)
    fun observeSide(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        side: Direction,
        unusedAttributes: Set<AttributeDataSource<*>>
    )

    context(sourceOwner: SO, observationContext: OC, attributeStore: IAttributeValueStore.MapAttributeStore)
    override fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        if (source.observedSide in unusedAttributes) {
            source.observedSide.withoutValue {
                observeUnsided(recorder, level, position, facing, unusedAttributes)
            }
        } else {
            for (direction in directions) {
                source.observedSide.withValue(direction) {
                    observeSide(recorder, level, position, direction, unusedAttributes)
                }
            }
        }
    }
}
