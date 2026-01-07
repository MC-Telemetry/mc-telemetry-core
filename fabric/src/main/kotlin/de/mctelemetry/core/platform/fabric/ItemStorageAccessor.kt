package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.IItemStorageAccessor
import io.github.pixix4.kobserve.plus
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item

object ItemStorageAccessor : IItemStorageAccessor {
    override fun getItemCounts(level: ServerLevel, position: BlockPos, facing: Direction?): Map<Item, Long> {
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
}
