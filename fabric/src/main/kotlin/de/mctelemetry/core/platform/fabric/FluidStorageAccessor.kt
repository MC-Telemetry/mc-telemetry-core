package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.IFluidStorageAccessor
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.material.Fluid

object FluidStorageAccessor : IFluidStorageAccessor {
    override fun getFluidAmounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Fluid, Long> {
        val storage = FluidStorage.SIDED.find(level, position, facing) ?: return mapOf()

        val map = mutableMapOf<Fluid, Long>()
        for (view in storage) {
            if (view.isResourceBlank) {
                continue
            }

            map.merge(view.resource.fluid, view.amount, Long::plus)
        }

        return map
    }

    override fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double {
        val storage = FluidStorage.SIDED.find(level, position, facing) ?: return 0.0

        var count = 0
        var fillRatio = 0.0
        for (view in storage) {
            count += 1

            if (view.isResourceBlank) {
                continue
            }

            fillRatio += view.amount.toDouble() / view.capacity.toDouble()
        }

        return fillRatio / count.toDouble()
    }
}
