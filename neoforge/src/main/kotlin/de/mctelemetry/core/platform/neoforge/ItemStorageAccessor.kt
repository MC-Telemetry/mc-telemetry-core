package de.mctelemetry.core.platform.neoforge

import de.mctelemetry.core.platform.IItemStorageAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.neoforged.neoforge.capabilities.Capabilities

object ItemStorageAccessor : IItemStorageAccessor {
    override fun getItemAmounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Item, Long> {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val cap = level.getCapability(Capabilities.ItemHandler.BLOCK, position, facing) ?: return mapOf()

        val map = mutableMapOf<Item, Long>()
        for (i in 0..<cap.slots) {
            val stack = cap.getStackInSlot(i)
            if (stack.isEmpty) {
                continue
            }

            map.merge(stack.item, stack.count.toLong(), Long::plus)
        }

        return map
    }

    override fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val cap = level.getCapability(Capabilities.ItemHandler.BLOCK, position, facing) ?: return 0.0

        var count = 0
        var fillRatio = 0.0
        for (i in 0..<cap.slots) {
            val stack = cap.getStackInSlot(i)
            count += 1

            if (stack.isEmpty) {
                continue
            }

            val limit = cap.getSlotLimit(i)
            val stackSize =
                if (limit == 99) stack.maxStackSize.toDouble() else ((limit.toDouble() / 64.0) * stack.maxStackSize.toDouble())
            fillRatio += stack.count.toDouble() / stackSize
        }

        return fillRatio / count.toDouble()
    }
}
