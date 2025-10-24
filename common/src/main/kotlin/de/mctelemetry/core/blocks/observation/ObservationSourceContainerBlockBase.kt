package de.mctelemetry.core.blocks.observation

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.state.BlockState

abstract class ObservationSourceContainerBlockBase(properties: Properties) : BaseEntityBlock(properties) {

    abstract override fun codec(): MapCodec<out ObservationSourceContainerBlockBase>

    abstract override fun newBlockEntity(
        blockPos: BlockPos,
        blockState: BlockState,
    ): ObservationSourceContainerEntityBlock?

    override fun tick(
        blockState: BlockState,
        serverLevel: ServerLevel,
        blockPos: BlockPos,
        randomSource: RandomSource
    ) {
        super.tick(blockState, serverLevel, blockPos, randomSource)
    }
}
