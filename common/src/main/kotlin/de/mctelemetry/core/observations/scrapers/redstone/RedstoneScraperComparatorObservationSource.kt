package de.mctelemetry.core.observations.scrapers.redstone

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.base.PositionObservationSourceBase
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.state.BlockState

object RedstoneScraperComparatorObservationSource : PositionObservationSourceBase() {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "redstone_scraper.comparator")
    )

    context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.MapLookup)
    override fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        val state = level.getBlockState(position)
        if (state.hasAnalogOutputSignal()) {
            var analogValue = 0
            var advancedAnalogValue = 0.0
            val server = level.server
            server.executeBlocking {
                analogValue = state.getAnalogOutputSignal(level, position)
                if (recorder.supportsFloating)
                    advancedAnalogValue = getAdvancedAnalogValue(level, state, position, analogValue)
            }

            if (recorder.supportsFloating) {
                recorder.observePreferred(
                    advancedAnalogValue,
                    analogValue.toLong(),
                    this,
                )
            } else {
                recorder.observe(
                    analogValue.toLong(),
                    this,
                )
            }
        }
    }

    fun getAdvancedAnalogValue(level: ServerLevel, blockState: BlockState, blockPos: BlockPos, fallback: Int): Double {
        if (!blockState.hasBlockEntity()) return fallback.toDouble()
        val blockEntity = level.getChunkAt(blockPos).getBlockEntity(blockPos) ?: return fallback.toDouble()
        val block = blockState.block
        return when {
            blockEntity is LecternBlockEntity -> getLecternAdvancedAnalogOutput(blockEntity, fallback)
            block is ChestBlock -> getContainerAdvancedAnalogOutput(
                ChestBlock.getContainer(
                    block,
                    blockState,
                    level,
                    blockPos,
                    false
                ), fallback
            )

            blockEntity is Container -> getContainerAdvancedAnalogOutput(blockEntity, fallback)
            else -> fallback.toDouble()
        }
    }

    fun getContainerAdvancedAnalogOutput(entity: Container?, fallback: Int): Double {
        if (entity == null) return fallback.toDouble()
        var accumulator = 0.0
        for (i in 0..<entity.containerSize) {
            val item = entity.getItem(i) ?: continue
            if (item.isEmpty) continue
            accumulator += item.count.toDouble() / item.maxStackSize
        }
        return 14 * (accumulator / entity.containerSize) + if (accumulator > 0.0) 1 else 0
    }

    fun getLecternAdvancedAnalogOutput(entity: LecternBlockEntity?, fallback: Int): Double {
        if (entity == null || !entity.hasBook()) return fallback.toDouble()
        val book = entity.book ?: return fallback.toDouble()
        if (book.isEmpty) return fallback.toDouble()
        val writtenBookContent = book[DataComponents.WRITTEN_BOOK_CONTENT]
        val pageCount: Int = if (writtenBookContent != null) {
            writtenBookContent.pages().count()
        } else {
            val writableBookContent = book[DataComponents.WRITABLE_BOOK_CONTENT]
            if (writableBookContent != null) {
                writableBookContent.pages().count()
            } else {
                return fallback.toDouble()
            }
        }
        if (pageCount <= 1) return 15.0
        val currentPage = entity.page
        return 1.0 + (14.0 * (currentPage.toDouble() / (pageCount - 1)))
    }
}
