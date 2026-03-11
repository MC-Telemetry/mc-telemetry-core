package de.mctelemetry.core.api.observations.position

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.observations.IObservationSource
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.block.entity.BlockEntity

interface IPositionObservationSource<
        SO: BlockEntity,
        I : IPositionObservationSourceInstance<SO, *, I>
        > : IObservationSource<SO, I> {
    val observedPosition: AttributeDataSource.Reference.ObservationSourceAttributeReference<GlobalPos>
}
