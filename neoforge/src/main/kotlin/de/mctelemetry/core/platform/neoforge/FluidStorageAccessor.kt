package de.mctelemetry.core.platform.neoforge

import de.mctelemetry.core.platform.IFluidStorageAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.capabilities.Capabilities

object FluidStorageAccessor : IFluidStorageAccessor {
    override fun getFluidAmounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Fluid, Long> {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val cap = level.getCapability(Capabilities.FluidHandler.BLOCK, position, facing) ?: return mapOf()

        val map = mutableMapOf<Fluid, Long>()
        for (i in 0..<cap.tanks) {
            val stack = cap.getFluidInTank(i)
            if (stack.isEmpty) {
                continue
            }

            map.merge(stack.fluid, stack.amount.toLong(), Long::plus)
        }

        return map
    }

    override fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val cap = level.getCapability(Capabilities.FluidHandler.BLOCK, position, facing) ?: return 0.0

        var count = 0
        var fillRatio = 0.0
        for (i in 0..<cap.tanks) {
            val stack = cap.getFluidInTank(i)
            count += 1

            if (stack.isEmpty) {
                continue
            }

            fillRatio += stack.amount.toDouble() / cap.getTankCapacity(i).toDouble()
        }

        return fillRatio / count.toDouble()
    }
}
