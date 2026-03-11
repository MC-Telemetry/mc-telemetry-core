package de.mctelemetry.core.api.observations.position.side

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.observations.position.IPositionObservationSource
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity

interface IPositionSideObservationSource<
        SO: BlockEntity,
        I : IPositionSideObservationSourceInstance<SO, *, I>
        > : IPositionObservationSource<SO, I> {
    val observedSide: AttributeDataSource.Reference.ObservationSourceAttributeReference<Direction>
}
