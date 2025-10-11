package de.mctelemetry.core.blocks

import com.mojang.serialization.MapCodec
import de.mctelemetry.core.blocks.entities.RubyBlockEntity
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.InteractionEvent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class RubyBlock(properties: Properties) : InteractionEvent.RightClickBlock, BaseEntityBlock(properties) {
    override fun codec(): MapCodec<out BaseEntityBlock> {
        return CODEC
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RubyBlockEntity(pos, state)
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T> {
        return RubyBlockEntity.Ticker()
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
        if (blockEntity !is RubyBlockEntity) {
            return EventResult.pass()
        }

        if (player.isShiftKeyDown) {
            return EventResult.pass()
        }

        player.openMenu(blockEntity)

        return EventResult.interruptTrue()
    }

    init {
        InteractionEvent.RIGHT_CLICK_BLOCK.register(this);
    }

    companion object {
        val CODEC: MapCodec<RubyBlock> = simpleCodec(::RubyBlock)
    }
}
