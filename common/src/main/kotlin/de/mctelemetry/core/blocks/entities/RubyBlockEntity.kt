package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.ui.RubyBlockMenu
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.jvm.optionals.getOrElse


class RubyBlockEntity(pos: BlockPos, state: BlockState) :
    BaseContainerBlockEntity(OTelCoreModBlockEntityTypes.RUBY_BLOCK_ENTITY.get(), pos, state) {

    private var storedBuckets = 0
    private var items: NonNullList<ItemStack> = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private var data: ContainerData = object : ContainerData {
        override fun get(index: Int): Int {
            if (index == 0) return storedBuckets
            else if (index == 1) return signalValue
            else return -1
        }

        override fun set(index: Int, value: Int) {
            if (index == 0) storedBuckets = value
            else if (index == 1) signalValue = value
        }

        override fun getCount(): Int {
            return 2
        }
    }

    var signalValue: Int = 0

    init {
        Ticker.register(this)
    }

    override fun setRemoved() {
        Ticker.unregister(this)
    }

    override fun getDefaultName(): Component {
        return Component.translatable("container.mcotelcore.ruby_block")
    }

    override fun getItems(): NonNullList<ItemStack> {
        return this.items
    }

    override fun setItems(items: NonNullList<ItemStack>) {
        this.items = items
    }

    override fun createMenu(containerId: Int, inventory: Inventory): AbstractContainerMenu {
        return RubyBlockMenu(containerId, inventory, this, this.data)
    }

    override fun getContainerSize(): Int {
        return INVENTORY_SIZE
    }

    class Ticker<T : BlockEntity> : BlockEntityTicker<T> {

        companion object {

            private val registrations = ConcurrentLinkedQueue<RubyBlockEntity>()
            fun register(blockEntity: RubyBlockEntity) {
                registrations.add(blockEntity)
            }

            fun unregister(blockEntity: RubyBlockEntity) {
                registrations.remove(blockEntity)
            }

            fun unregisterAll() {
                registrations.clear()
            }

            private val signalMetric =
                OTelCoreMod.meter.gaugeBuilder("minecraft.mod.${OTelCoreMod.MOD_ID}.ruby.signal").ofLongs()
                    .buildWithCallback { metric ->
                        var toRemove: MutableSet<RubyBlockEntity>? = null
                        registrations.forEach {
                            val level = it.level ?: return@forEach
                            if (!level.isLoaded(it.blockPos)) return@forEach
                            val levelEntity =
                                level.getBlockEntity(it.blockPos, OTelCoreModBlockEntityTypes.RUBY_BLOCK_ENTITY.get())
                                    .getOrElse { return@forEach }
                            if (levelEntity != it) {
                                toRemove = toRemove ?: mutableSetOf()
                                toRemove.add(it)
                                return@forEach
                            }
                            val attributes = Attributes.of(
                                positionAttributeKey,
                                "${
                                    level.dimension().location().path
                                }/(${it.blockPos.x},${it.blockPos.y},${it.blockPos.z})"
                            )
                            metric.record(it.signalValue.toLong(), attributes)
                        }
                        if (toRemove != null) {
                            registrations.removeAll(toRemove)
                        }
                    }
            private val positionAttributeKey: AttributeKey<String> = AttributeKey.stringKey("pos")
        }

        override fun tick(level: Level, blockPos: BlockPos, blockState: BlockState, blockEntity: T) {
            if (blockEntity is RubyBlockEntity) {
                val firstSlot: ItemStack = blockEntity.getItem(0)
                if (firstSlot.`is`(Items.WATER_BUCKET)) {
                    if (blockEntity.storedBuckets < 64) {
                        blockEntity.storedBuckets++
                        blockEntity.setItem(0, ItemStack(Items.BUCKET))
                    }
                }
                val secondSlot: ItemStack = blockEntity.getItem(1)
                if (secondSlot.`is`(Items.BUCKET)) {
                    if (blockEntity.storedBuckets > 0) {
                        blockEntity.storedBuckets--
                        blockEntity.setItem(1, ItemStack(Items.WATER_BUCKET))
                    }
                }

                val signal = level.getBestNeighborSignal(blockPos)
                blockEntity.signalValue = signal
            }
        }
    }

    companion object {
        const val INVENTORY_SIZE: Int = 2
    }
}