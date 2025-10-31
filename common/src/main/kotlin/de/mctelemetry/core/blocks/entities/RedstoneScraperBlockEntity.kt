package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.IObservationRecorder
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.blocks.RedstoneScraperBlock
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.ui.RedstoneScraperBlockMenu
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
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


class RedstoneScraperBlockEntity(pos: BlockPos, state: BlockState) :
        MenuProvider, BlockEntity(OTelCoreModBlockEntityTypes.REDSTONE_SCRAPER_BLOCK_ENTITY.get(), pos, state) {

    private var registration: AutoCloseable? = null
        set(value) {
            var accumulator: Exception? = null
            try {
                val currentValue = blockState.getValue(RedstoneScraperBlock.ERROR)
                val targetValue = if (value == null)
                    ObservationSourceState.ErrorState.Type.Errors
                else
                    ObservationSourceState.ErrorState.Type.Ok

                if (targetValue != currentValue) {
                    val newBlockState = blockState.setValue(RedstoneScraperBlock.ERROR, targetValue)
                    level?.setBlock(blockPos, newBlockState, 2)
                }
            } catch (ex: Exception) {
                accumulator = ex
            }
            field = value
            if (accumulator != null) {
                throw accumulator
            }
        }

    fun tryRegister() {
        if (registration != null) {
            return
        }
        val instrumentManager = level?.server?.instrumentManager
        if (instrumentManager == null) {
            return
        }
        val mutableInstrument =
            instrumentManager.findLocalMutable("minecraft.mod.${OTelCoreMod.MOD_ID}.redstone.signal")
        val facing = blockState.getValue(RedstoneScraperBlock.FACING)
        val observePos = GlobalPos(level!!.dimension(), blockPos.relative(facing))
        val attributes = Attributes.of(
            positionAttributeKey, "${
                observePos.dimension.location().path
            }/(${observePos.pos.x},${observePos.pos.y},${observePos.pos.z})"
        )
        registration = mutableInstrument?.addCallback(
            Attributes.empty(),
            object : IInstrumentRegistration.Callback<IInstrumentRegistration> {

                override fun observe(instrument: IInstrumentRegistration, recorder: IObservationRecorder.Resolved) {
                    val level = level ?: return
                    if (!(level.isLoaded(observePos.pos) && level.shouldTickBlocksAt(observePos.pos))) return
                    val value = level.getBestNeighborSignal(observePos.pos)
                    recorder.observe(value.toLong(), attributes)
                }

                override fun onRemove(instrument: IInstrumentRegistration) {
                    this@RedstoneScraperBlockEntity.registration = null
                    super.onRemove(instrument)
                }
            })
    }

    init {
        tryRegister()
    }

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

    override fun setRemoved() {
        registration?.close()
    }

    override fun getDisplayName(): Component {
        return Component.translatable("container.mcotelcore.redstone_scraper_block")
    }

    override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu {
        return RedstoneScraperBlockMenu(containerId, inventory, this.data)
    }

    companion object {

        private val positionAttributeKey: AttributeKey<String> = AttributeKey.stringKey("pos")
    }

    class Ticker<T : BlockEntity> : BlockEntityTicker<T> {

        override fun tick(level: Level, blockPos: BlockPos, blockState: BlockState, blockEntity: T) {
            if (blockEntity is RedstoneScraperBlockEntity) {
                if (blockEntity.registration == null) {
                    blockEntity.tryRegister()
                }
                val facing = blockState.getValue(RedstoneScraperBlock.FACING)
                val observePos = blockPos.relative(facing)
                val signal = level.getBestNeighborSignal(observePos)
                blockEntity.signalValue = signal
            }
        }
    }
}
