package de.mctelemetry.core.api.observations.position.side

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.observations.position.IPositionObservationSource
import net.minecraft.core.Direction

interface IPositionSideObservationSource<
        I : IPositionSideObservationSourceInstance<*, I>
        > : IPositionObservationSource<I> {
    val observedSide: AttributeDataSource.Reference.ObservationSourceAttributeReference<Direction>
}
