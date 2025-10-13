package de.mctelemetry.core.ui

import de.mctelemetry.core.blocks.OTelCoreModBlocks
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.item.ItemStack


class RedstoneScraperBlockMenu(
    containerID: Int,
    playerInventory: Inventory,
    val data: ContainerData = SimpleContainerData(1)
) : AbstractContainerMenu(OTelCoreModMenuTypes.REDSTONE_SCRAPER_BLOCK.get(), containerID) {

    init {
        checkContainerDataCount(data, 1)
        addDataSlots(data)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        return  ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return stillValid(ContainerLevelAccess.NULL, player, OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get());
    }
}