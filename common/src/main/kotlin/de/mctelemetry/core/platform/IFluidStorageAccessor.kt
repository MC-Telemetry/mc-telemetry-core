package de.mctelemetry.core.platform

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.material.Fluid

interface IFluidStorageAccessor {
    fun getFluidAmounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Fluid, Long>
    fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double
}