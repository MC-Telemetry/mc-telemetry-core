package de.mctelemetry.core.platform

import net.minecraft.world.item.Item

interface IItemStorageAccessor {
    fun getItemCounts(): Map<Item, Long>
}