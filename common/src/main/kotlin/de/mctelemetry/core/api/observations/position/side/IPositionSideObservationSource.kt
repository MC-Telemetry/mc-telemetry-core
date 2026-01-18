package de.mctelemetry.core.api.observations.position.side

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.position.IPositionObservationSource
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.block.entity.BlockEntity

interface IPositionSideObservationSource<
        I : IPositionSideObservationSourceInstance<*>
        > : IPositionObservationSource<I> {
    val observedSide: AttributeDataSource.Reference.ObservationSourceAttributeReference<Direction>
}
