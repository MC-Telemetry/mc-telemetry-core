package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.IItemStorageAccessor
import io.github.pixix4.kobserve.plus
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item

class ItemStorageAccessor(private val level: ServerLevel, private val position: BlockPos, private val facing: Direction?) : IItemStorageAccessor {
    override fun getItemCounts(): Map<Item, Long> {
        val storage = ItemStorage.SIDED.find(level, position, facing) ?: return mapOf()

        val map = mutableMapOf<Item, Long>()

        for (slot in storage) {
            if (slot.isResourceBlank) {
                continue
            }

            map.merge(slot.resource.item, slot.amount, Long::plus)
        }

        return map
    }
}
