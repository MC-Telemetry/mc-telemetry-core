package de.mctelemetry.core.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class ScraperBlockEntity(blockPos: BlockPos, blockState: BlockState) : ObservationSourceContainerBlockEntity(
    OTelCoreModBlockEntityTypes.SCRAPER_BLOCK_ENTITY.get(),
    blockPos,
    blockState,
) {

    override fun getType(): BlockEntityType<out ScraperBlockEntity> {
        @Suppress("UNCHECKED_CAST") // known value from constructor
        return blockEntityType as BlockEntityType<ScraperBlockEntity>
    }
}
