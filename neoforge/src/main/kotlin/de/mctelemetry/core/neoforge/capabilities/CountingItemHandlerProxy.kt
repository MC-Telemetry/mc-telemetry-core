package de.mctelemetry.core.neoforge.capabilities

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.observations.IORecorder
import de.mctelemetry.core.utils.runWithExceptionCleanup
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.IBlockCapabilityProvider
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.registries.RegisterEvent
import kotlin.concurrent.getOrSet

class CountingItemHandlerProxy(
    val wrapped: IItemHandler,
    val recorder: IORecorder<Item>?,
    val reversedRecorder: IORecorder<Item>?
) :
    IItemHandler by wrapped {
    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        val extractResult = wrapped.extractItem(slot, amount, simulate)
        if (!simulate && extractResult != null) {
            recorder?.addExtracted(extractResult.count, extractResult.item)
            reversedRecorder?.addPulled(extractResult.count, extractResult.item)
        }
        return extractResult
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        val originalCount = stack.count
        val insertResult = wrapped.insertItem(slot, stack, simulate)
        if (!simulate && insertResult != null) {
            val count = if(insertResult.item == stack.item) originalCount - insertResult.count else originalCount
            recorder?.addInserted(count, stack.item)
            reversedRecorder?.addPushed(count, stack.item)
        }
        return insertResult
    }

    companion object {

        private var blockSet: Set<Block> = emptySet()
        private val recorders: MutableMap<ResourceKey<Level>, Long2ObjectMap<IORecorder<Item>>> = mutableMapOf()

        private object CapabilityProvider : IBlockCapabilityProvider<IItemHandler, Direction?> {

            private val runningMapLocal: ThreadLocal<MutableMap<ResourceKey<Level>, LongSet>> = ThreadLocal()

            @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
            override fun getCapability(
                level: Level,
                pos: BlockPos,
                state: BlockState,
                blockEntity: BlockEntity?,
                context: Direction?
            ): IItemHandler? {
                val map = runningMapLocal.getOrSet { mutableMapOf() }
                val enteredBlockPositions = map.getOrPut(level.dimension()) { LongOpenHashSet() }
                val longPos = pos.asLong()
                if (!enteredBlockPositions.add(longPos)) return null
                try {
                    val levelRecorders = recorders.getOrElse(level.dimension()) { return null }
                    val primaryRecorder: IORecorder<Item>? = levelRecorders.get(longPos)
                    val secondaryRecorder: IORecorder<Item>?
                    if (context == null) {
                        if (primaryRecorder == null) return null
                        secondaryRecorder = null
                    } else {
                        val secondaryPos = pos.relative(context) //TODO: check if needs to be inverted?
                        secondaryRecorder = levelRecorders.get(secondaryPos.asLong())
                    }
                    val innerHandler: IItemHandler =
                        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                        level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, context)
                            ?: return null
                    return CountingItemHandlerProxy(innerHandler, primaryRecorder, secondaryRecorder)
                } finally {
                    enteredBlockPositions.remove(longPos)
                }
            }
        }

        fun getIORecorder(level: ServerLevel, blockPos: BlockPos): IORecorder.IORecorderAccess<Item> {
            val blockMap = recorders.getOrPut(level.dimension()) { Long2ObjectOpenHashMap() }
            val longPos = blockPos.asLong()
            val existingRecorder: IORecorder<Item>? = blockMap.get(longPos)
            if (existingRecorder != null) {
                return IORecorder.IORecorderAccess(existingRecorder)
            } else {
                val newRecorder = IORecorder<Item>()
                val storedRecorder: IORecorder<Item>? = blockMap.putIfAbsent(longPos, newRecorder)
                if (storedRecorder != null && storedRecorder !== newRecorder) {
                    return IORecorder.IORecorderAccess(storedRecorder)
                }
                val recorderAccess = IORecorder.IORecorderAccess(newRecorder, false)
                runWithExceptionCleanup({ recorderAccess.close() }) {
                    level.invalidateCapabilities(blockPos)
                    val mutablePos = blockPos.mutable()
                    for (d in Direction.entries) {
                        mutablePos.setWithOffset(blockPos, d)
                        level.invalidateCapabilities(mutablePos)
                    }
                }
                return recorderAccess
            }
        }

        fun provideBlockSet(blocks: Collection<Block>) {
            blockSet = blockSet.union(blocks)
        }

        fun onBlockRegister(registerEvent: RegisterEvent) {
            if(registerEvent.registryKey == Registries.BLOCK) {
                @Suppress("UNCHECKED_CAST")
                val registry = registerEvent.registry as Registry<Block>
                provideBlockSet(registry.toSet() - Blocks.AIR)
            }
        }

        fun registerCapabilities(event: RegisterCapabilitiesEvent) {
            val level =
                if (blockSet.isEmpty()) org.apache.logging.log4j.Level.ERROR
                else org.apache.logging.log4j.Level.DEBUG
            OTelCoreMod.logger.log(level, "Registering item io capability proxy for ${blockSet.size} blocks")
            event.registerBlock(Capabilities.ItemHandler.BLOCK, CapabilityProvider, *blockSet.toTypedArray())
        }
    }
}
