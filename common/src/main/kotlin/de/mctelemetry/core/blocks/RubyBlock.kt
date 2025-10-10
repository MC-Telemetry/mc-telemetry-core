package de.mctelemetry.core.blocks

import com.mojang.serialization.MapCodec
import de.mctelemetry.core.blocks.entities.RubyBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class RubyBlock(properties: Properties) : BaseEntityBlock(properties) {
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

    companion object {
        val CODEC: MapCodec<RubyBlock> = simpleCodec(::RubyBlock)
    }
}

