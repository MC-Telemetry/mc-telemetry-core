package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.IItemStorageAccessor
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item

object ItemStorageAccessor : IItemStorageAccessor {
    override fun getItemAmounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Item, Long> {
        val storage = ItemStorage.SIDED.find(level, position, facing) ?: return mapOf()

        val map = mutableMapOf<Item, Long>()
        for (view in storage) {
            if (view.isResourceBlank) {
                continue
            }

            map.merge(view.resource.item, view.amount, Long::plus)
        }

        return map
    }

    override fun getFillRatio(level: ServerLevel, position: BlockPos, facing: Direction?): Double {
        val storage = ItemStorage.SIDED.find(level, position, facing) ?: return 0.0

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
