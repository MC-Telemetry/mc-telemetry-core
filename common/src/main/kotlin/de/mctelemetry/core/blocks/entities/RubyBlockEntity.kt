package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.OTelCoreMod
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.jvm.optionals.getOrElse

class RubyBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(OTelCoreModBlockEntityTypes.RUBY_BLOCK_ENTITY.get(), pos, state) {

    var signalValue: Int = 0

    init {
        Ticker.register(this)
    }

    override fun setRemoved() {
        Ticker.unregister(this)
    }


    class Ticker<T : BlockEntity> : BlockEntityTicker<T> {

        companion object {

            private val registrations = ConcurrentLinkedQueue<RubyBlockEntity>()
            fun register(blockEntity: RubyBlockEntity){
                registrations.add(blockEntity)
            }

            fun unregister(blockEntity: RubyBlockEntity){
                registrations.remove(blockEntity)
            }

            fun unregisterAll() {
                registrations.clear()
            }

            private val signalMetric = OTelCoreMod.meter.gaugeBuilder("minecraft.mod.${OTelCoreMod.MOD_ID}.ruby.signal").ofLongs().buildWithCallback { metric ->
                var toRemove: MutableSet<RubyBlockEntity>? = null
                registrations.forEach {
                    val level = it.level ?: return@forEach
                    if(!level.isLoaded(it.blockPos)) return@forEach
                    val levelEntity = level.getBlockEntity(it.blockPos, OTelCoreModBlockEntityTypes.RUBY_BLOCK_ENTITY.get()).getOrElse { return@forEach }
                    if(levelEntity != it) {
                        toRemove = toRemove ?: mutableSetOf()
                        toRemove.add(it)
                        return@forEach
                    }
                    val attributes = Attributes.of(positionAttributeKey, "${level.dimension().location().path}/(${it.blockPos.x},${it.blockPos.y},${it.blockPos.z})")
                    metric.record(it.signalValue.toLong(), attributes)
                }
                if(toRemove != null){
                    registrations.removeAll(toRemove)
                }
            }
            private val positionAttributeKey: AttributeKey<String> = AttributeKey.stringKey("pos")
        }

        override fun tick(level: Level, blockPos: BlockPos, blockState: BlockState, blockEntity: T) {
            val signal = level.getBestNeighborSignal(blockPos)
            (blockEntity as RubyBlockEntity).signalValue = signal
        }
    }
}