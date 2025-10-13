package de.mctelemetry.core.ui

import de.mctelemetry.core.OTelCoreMod
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack


class RedstoneScraperBlockMenu(
    containerID: Int,
    playerInventory: Inventory,
    val container: Container = SimpleContainer(2),
    val data: ContainerData = SimpleContainerData(2)
) : AbstractContainerMenu(OTelCoreModMenuTypes.REDSTONE_SCRAPER_BLOCK.get(), containerID) {

    init {
        checkContainerSize(container, 2)
        checkContainerDataCount(data, 2)

        // Player inventory
        for (i in 0..2) {
            for (j in 0..8) {
                this.addSlot(Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18))
            }
        }
        // Player Hotbar
        for (i in 0..8) {
            this.addSlot(Slot(playerInventory, i, 8 + i * 18, 142))
        }

        // Own slots
        this.addSlot(Slot(container, 0, 62, 35))
        this.addSlot(Slot(container, 1, 98, 35))

        this.addDataSlots(data)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var newStack = ItemStack.EMPTY
        val slot = this.slots.get(index)
        if (slot.hasItem()) {
            val originalStack = slot.getItem()
            newStack = originalStack.copy()

            if (index < 36) {
                if (!moveItemStackTo(originalStack, 36, 36 + 2, false)) {
                    return ItemStack.EMPTY
                }
            } else if (index < 36 + 2) {
                if (!moveItemStackTo(originalStack, 0, 36, false)) {
                    return ItemStack.EMPTY
                }
            } else {
                OTelCoreMod.logger.error("Invalid slot index: {}", index)
                return ItemStack.EMPTY
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }

            slot.onTake(player, originalStack)
        }
        return newStack
    }

    override fun stillValid(player: Player): Boolean {
        return container.stillValid(player)
    }
}