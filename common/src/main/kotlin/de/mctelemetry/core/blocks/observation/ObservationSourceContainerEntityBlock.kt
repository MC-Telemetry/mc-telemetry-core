package de.mctelemetry.core.blocks.observation

import de.mctelemetry.core.observations.IObservationSource
import de.mctelemetry.core.observations.ObservationSourceState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class ObservationSourceContainerEntityBlock(
    blockEntityType: BlockEntityType<out ObservationSourceContainerEntityBlock>,
    blockPos: BlockPos,
    blockState: BlockState,
) : BlockEntity(blockEntityType, blockPos, blockState) {

    private val observationStates: MutableMap<IObservationSource<*, *>, ObservationSourceState> =
        mutableMapOf()

    override fun getType(): BlockEntityType<out ObservationSourceContainerEntityBlock> {
        @Suppress("UNCHECKED_CAST")
        return super.type as BlockEntityType<ObservationSourceContainerEntityBlock>
    }
}
