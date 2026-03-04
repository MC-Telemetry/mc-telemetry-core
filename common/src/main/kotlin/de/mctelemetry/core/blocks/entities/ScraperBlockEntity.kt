package de.mctelemetry.core.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

open class ScraperBlockEntity(
    blockPos: BlockPos,
    blockState: BlockState,
    blockEntityType: BlockEntityType<out ScraperBlockEntity>
) : ObservationSourceContainerBlockEntity(
    blockEntityType,
    blockPos,
    blockState,
) {

    override fun getType(): BlockEntityType<out ScraperBlockEntity> {
        @Suppress("UNCHECKED_CAST") // known value from constructor
        return blockEntityType as BlockEntityType<out ScraperBlockEntity>
    }

    companion object {
        operator fun invoke(blockPos: BlockPos, blockState: BlockState): ScraperBlockEntity =
            ScraperBlockEntity(blockPos, blockState, OTelCoreModBlockEntityTypes.SCRAPER_BLOCK_ENTITY.get())
    }
}
