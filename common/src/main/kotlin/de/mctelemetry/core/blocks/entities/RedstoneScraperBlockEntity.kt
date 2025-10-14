package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.RedstoneScraperBlock
import de.mctelemetry.core.ui.RedstoneScraperBlockMenu
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.jvm.optionals.getOrElse


class RedstoneScraperBlockEntity(pos: BlockPos, state: BlockState) :
    MenuProvider, BlockEntity(OTelCoreModBlockEntityTypes.REDSTONE_SCRAPER_BLOCK_ENTITY.get(), pos, state) {

    private var data: ContainerData = object : ContainerData {
        override fun get(index: Int): Int {
            if (index == 0) return signalValue
            else return -1
        }

        override fun set(index: Int, value: Int) {
            if (index == 0) signalValue = value
        }

        override fun getCount(): Int {
            return 1
        }
    }

    var signalValue: Int = 0

    init {
        Ticker.register(this)
    }

    override fun setRemoved() {
        Ticker.unregister(this)
    }

    override fun getDisplayName(): Component {
        return Component.translatable("container.mcotelcore.redstone_scraper_block")
    }

    override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu {
        return RedstoneScraperBlockMenu(containerId, inventory, this.data)
    }

    class Ticker<T : BlockEntity> : BlockEntityTicker<T> {

        companion object {

            private val registrations = ConcurrentLinkedQueue<RedstoneScraperBlockEntity>()
            fun register(blockEntity: RedstoneScraperBlockEntity) {
                registrations.add(blockEntity)
            }

            fun unregister(blockEntity: RedstoneScraperBlockEntity) {
                registrations.remove(blockEntity)
            }

            fun unregisterAll() {
                registrations.clear()
            }

            private val signalMetric =
                OTelCoreMod.meter.gaugeBuilder("minecraft.mod.${OTelCoreMod.MOD_ID}.redstone.signal").ofLongs()
                    .buildWithCallback { metric ->
                        var toRemove: MutableSet<RedstoneScraperBlockEntity>? = null
                        registrations.forEach {
                            val level = it.level ?: return@forEach
                            if (!level.isLoaded(it.blockPos)) return@forEach
                            val levelEntity =
                                level.getBlockEntity(
                                    it.blockPos,
                                    OTelCoreModBlockEntityTypes.REDSTONE_SCRAPER_BLOCK_ENTITY.get()
                                )
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
            if (blockEntity is RedstoneScraperBlockEntity) {
                val signal = level.getBestNeighborSignal(blockPos)
                blockEntity.signalValue = signal

                val currentValue = blockState.getValue(RedstoneScraperBlock.ERROR)
                val targetValue = signal < 9

                if (targetValue != currentValue) {
                    val newBlockState = blockState.setValue(RedstoneScraperBlock.ERROR, targetValue)
                    level.setBlock(blockPos, newBlockState, 2)
                }
            }
        }
    }
}