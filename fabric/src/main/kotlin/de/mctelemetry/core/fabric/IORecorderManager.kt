package de.mctelemetry.core.fabric

import de.mctelemetry.core.observations.IORecorder
import de.mctelemetry.core.utils.runWithExceptionCleanup
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.impl.lookup.block.ServerWorldCache
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.Spliterator
import java.util.function.Consumer

object IORecorderManager {

    private val levelMap: MutableMap<ResourceKey<Level>, Long2ObjectMap<IORecorder<Item>>> = mutableMapOf()

    fun getIORecorder(level: ServerLevel, blockPos: BlockPos): IORecorder.IORecorderAccess<Item> {
        val blockMap = levelMap.getOrPut(level.dimension()) { Long2ObjectOpenHashMap() }
        level as ServerWorldCache
        val longPos = blockPos.asLong()
        val existingRecorder = blockMap.get(longPos)
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
                level.fabric_invalidateCache(blockPos)
                val mutablePos = blockPos.mutable()
                for (d in Direction.entries) {
                    mutablePos.setWithOffset(blockPos, d)
                    level.fabric_invalidateCache(mutablePos)
                }
            }
            return recorderAccess
        }
    }

    fun transform(
        original: Storage<ItemVariant>,
        level: Level,
        pos: BlockPos,
        state: BlockState,
        blockEntity: BlockEntity?,
        context: Direction?
    ): Storage<ItemVariant> {
        val recorders = levelMap.getOrElse(level.dimension()) { return original }
        val longPos = pos.asLong()
        val recorder: IORecorder<Item>? = recorders.get(longPos)
        val reversedRecorder: IORecorder<Item>?
        if (context != null) {
            val reversedLongPos = pos.relative(context).asLong()
            reversedRecorder = recorders.get(reversedLongPos)
        } else {
            reversedRecorder = null
        }
        if (recorder == null && reversedRecorder == null) {
            return original
        }
        return IORecorderStorageWrapper(recorder, reversedRecorder, original)
    }

    private class RecordingTransactionCallback(
        val recorder: IORecorder<Item>?,
        val reversedRecorder: IORecorder<Item>?,
        val recorderIsReceiving: Boolean,
        val resource: ItemVariant,
        var count: Long = 0,
    ) : TransactionContext.CloseCallback, TransactionContext.OuterCloseCallback {
        var aborted: Boolean = false
            private set

        override fun onClose(transaction: TransactionContext, result: TransactionContext.Result) {
            aborted = result.wasAborted()
        }

        override fun afterOuterClose(result: TransactionContext.Result) {
            if (!aborted && result.wasCommitted()) {
                if (recorderIsReceiving) {
                    recorder?.addInserted(count, resource.item)
                    reversedRecorder?.addPushed(count, resource.item)
                } else {
                    recorder?.addExtracted(count, resource.item)
                    reversedRecorder?.addPulled(count, resource.item)
                }
            }
        }
    }

    class IORecorderStorageWrapper(
        private val recorder: IORecorder<Item>?,
        private val reversedRecorder: IORecorder<Item>?,
        val baseStorage: Storage<ItemVariant>
    ) : Storage<ItemVariant> by baseStorage {
        override fun supportsInsertion(): Boolean {
            return baseStorage.supportsInsertion()
        }

        override fun supportsExtraction(): Boolean {
            return baseStorage.supportsExtraction()
        }

        override fun insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
            val count = baseStorage.insert(resource, maxAmount, transaction)
            val callback = RecordingTransactionCallback(recorder, reversedRecorder, true, resource, count)
            transaction.addCloseCallback(callback)
            transaction.addOuterCloseCallback(callback)
            return count
        }

        override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long {
            val count = baseStorage.insert(resource, maxAmount, transaction)
            val callback = RecordingTransactionCallback(recorder, reversedRecorder, false, resource, count)
            transaction.addCloseCallback(callback)
            transaction.addOuterCloseCallback(callback)
            return count
        }

        override fun getVersion(): Long {
            return baseStorage.version
        }

        private inner class RecorderStorageViewWrapper(val baseView: StorageView<ItemVariant>) :
            StorageView<ItemVariant> by baseView {
            override fun getUnderlyingView(): StorageView<ItemVariant> {
                return baseView
            }

            override fun extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext?): Long {
                val count = baseView.extract(resource, maxAmount, transaction)

                if (transaction == null) {
                    recorder?.addExtracted(count, resource.item)
                    reversedRecorder?.addPulled(count, resource.item)
                } else {
                    val callback = RecordingTransactionCallback(recorder, reversedRecorder, false, resource, count)
                    transaction.addCloseCallback(callback)
                    transaction.addOuterCloseCallback(callback)
                }
                return count
            }
        }

        private inner class IteratorDecorator(
            val baseIterator: Iterator<StorageView<ItemVariant>>
        ) : Iterator<StorageView<ItemVariant>> by baseIterator {
            override fun forEachRemaining(action: Consumer<in StorageView<ItemVariant>>) {
                baseIterator.forEachRemaining { action.accept(RecorderStorageViewWrapper(it)) }
            }

            override fun next(): StorageView<ItemVariant> = RecorderStorageViewWrapper(baseIterator.next())
        }

        private inner class MutableIteratorDecorator(
            val baseIterator: MutableIterator<StorageView<ItemVariant>>
        ) : MutableIterator<StorageView<ItemVariant>> by baseIterator {
            override fun forEachRemaining(action: Consumer<in StorageView<ItemVariant>>) {
                baseIterator.forEachRemaining { action.accept(RecorderStorageViewWrapper(it)) }
            }

            override fun next(): StorageView<ItemVariant> = RecorderStorageViewWrapper(baseIterator.next())
        }

        private inner class SpliteratorDecorator(val baseSpliterator: Spliterator<StorageView<ItemVariant>>) :
            Spliterator<StorageView<ItemVariant>> by baseSpliterator {
            override fun forEachRemaining(action: Consumer<in StorageView<ItemVariant>>) {
                baseSpliterator.forEachRemaining { action.accept(RecorderStorageViewWrapper(it)) }
            }

            override fun tryAdvance(p0: Consumer<in StorageView<ItemVariant>>): Boolean {
                return baseSpliterator.tryAdvance { p0.accept(RecorderStorageViewWrapper(it)) }
            }

            override fun trySplit(): Spliterator<StorageView<ItemVariant>>? {
                val baseSplit = baseSpliterator.trySplit() ?: return null
                return SpliteratorDecorator(baseSplit)
            }

            override fun getExactSizeIfKnown(): Long {
                return baseSpliterator.getExactSizeIfKnown()
            }

            override fun hasCharacteristics(characteristics: Int): Boolean {
                return baseSpliterator.hasCharacteristics(characteristics)
            }
        }

        override fun iterator(): MutableIterator<StorageView<ItemVariant>> {
            return MutableIteratorDecorator(baseStorage.iterator())
        }

        override fun spliterator(): Spliterator<StorageView<ItemVariant>> {
            return SpliteratorDecorator(baseStorage.spliterator())
        }

        override fun nonEmptyIterator(): Iterator<StorageView<ItemVariant>> {
            return IteratorDecorator(baseStorage.nonEmptyIterator())
        }

        override fun nonEmptyViews(): Iterable<StorageView<ItemVariant>> {
            val baseViews = baseStorage.nonEmptyViews()
            return object : Iterable<StorageView<ItemVariant>> by baseViews {
                override fun forEach(action: Consumer<in StorageView<ItemVariant>>) {
                    baseViews.forEach { action.accept(RecorderStorageViewWrapper(it)) }
                }

                override fun spliterator(): Spliterator<StorageView<ItemVariant>> {
                    return SpliteratorDecorator(baseViews.spliterator())
                }

                override fun iterator(): Iterator<StorageView<ItemVariant>> {
                    return IteratorDecorator(baseViews.iterator())
                }
            }
        }

        override fun forEach(action: Consumer<in StorageView<ItemVariant>>) {
            baseStorage.forEach { action.accept(RecorderStorageViewWrapper(it)) }
        }

    }
}
