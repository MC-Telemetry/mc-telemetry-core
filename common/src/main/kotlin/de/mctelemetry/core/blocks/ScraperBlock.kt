package de.mctelemetry.core.blocks

import de.mctelemetry.core.blocks.entities.ScraperBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition


abstract class ScraperBlock(properties: Properties) : ObservationSourceContainerBlock(properties) {

    override fun newBlockEntity(blockPos: BlockPos, blockState: BlockState): ScraperBlockEntity {
        return ScraperBlockEntity(blockPos, blockState)
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection)
    }

    protected override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    }

    protected override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(FACING)
    }

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH))
    }
}
