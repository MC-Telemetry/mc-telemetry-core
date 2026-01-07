package de.mctelemetry.core.platform

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item

interface IItemStorageAccessor {
    fun getItemCounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Item, Long>
}