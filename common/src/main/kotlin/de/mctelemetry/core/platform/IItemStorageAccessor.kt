package de.mctelemetry.core.platform

import de.mctelemetry.core.observations.IORecorder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item

interface IItemStorageAccessor {
    fun getItemAmounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Item, Long>
    fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double
    fun getIORecorder(level: ServerLevel, position: BlockPos): IORecorder.IORecorderAccess<Item>
}
