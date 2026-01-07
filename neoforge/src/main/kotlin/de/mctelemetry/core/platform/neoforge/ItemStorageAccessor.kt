package de.mctelemetry.core.platform.neoforge

import de.mctelemetry.core.platform.IItemStorageAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.neoforged.neoforge.capabilities.BlockCapabilityCache
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler

object ItemStorageAccessor : IItemStorageAccessor {
    override fun getItemCounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Item, Long> {
        val cap = level.getCapability(Capabilities.ItemHandler.BLOCK, position, facing) ?: return mapOf()

        val map = mutableMapOf<Item, Long>()
        for (i in 0..<cap.slots) {
            val itemStack = cap.getStackInSlot(i)
            if (itemStack.isEmpty) {
                continue
            }

            map.merge(itemStack.item, itemStack.count.toLong(), Long::plus)
        }

        return map
    }
}
