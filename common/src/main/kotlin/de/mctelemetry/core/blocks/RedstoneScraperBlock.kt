package de.mctelemetry.core.blocks

import com.mojang.serialization.MapCodec
import de.mctelemetry.core.blocks.entities.RedstoneScraperBlockEntity
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.InteractionEvent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.Property


class RedstoneScraperBlock(properties: Properties) : InteractionEvent.RightClickBlock, BaseEntityBlock(properties.noOcclusion()) {
    override fun codec(): MapCodec<out BaseEntityBlock> {
        return CODEC
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RedstoneScraperBlockEntity(pos, state)
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> {
        return RedstoneScraperBlockEntity.Ticker()
    }

    override fun click(
        player: Player,
        interactionHand: InteractionHand,
        blockPos: BlockPos,
        direction: Direction
    ): EventResult {
        if (player.level().getBlockEntity(blockPos) == null) {
            return EventResult.pass()
        }

        val blockEntity = player.level().getBlockEntity(blockPos)
        if (blockEntity !is RedstoneScraperBlockEntity) {
            return EventResult.pass()
        }

        if (player.isShiftKeyDown) {
            return EventResult.pass()
        }

        player.openMenu(blockEntity)

        return EventResult.interruptTrue()
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

    protected override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING).add(ERROR)
    }

    init {
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
        registerDefaultState(getStateDefinition().any().setValue(ERROR, false));

        InteractionEvent.RIGHT_CLICK_BLOCK.register(this);
    }

    companion object {
        val CODEC: MapCodec<RedstoneScraperBlock> = simpleCodec(::RedstoneScraperBlock)
        val FACING: EnumProperty<Direction> = BlockStateProperties.FACING
        val ERROR: Property<Boolean> = BooleanProperty.create("error")
    }
}
