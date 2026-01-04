package de.mctelemetry.core.observations.scrapers.redstone

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.AttributeDataSource.Companion.asObservationDataReference
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.attributes.invoke
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.state.BlockState

object RedstoneScraperComparatorObservationSource : IObservationSource.SingleAttribute<BlockEntity, GlobalPos> {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "redstone_scraper.comparator")
    )

    override val sourceContextType: Class<BlockEntity> = BlockEntity::class.java

    private val POS_KEY = BuiltinAttributeKeyTypes.GlobalPosType("pos").asObservationDataReference(this)

    override val reference: AttributeDataSource.Reference.ObservationSourceAttributeReference<GlobalPos> = POS_KEY

    context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.PairLookup<GlobalPos>)
    override fun observe(
        recorder: IObservationRecorder.Unresolved,
        unusedAttributes: Set<AttributeDataSource<*>>,
    ) {
        val level = sourceContext.level
        if (level == null || sourceContext.isRemoved) return
        val scraperPos = sourceContext.blockPos
        if (!(level.isLoaded(scraperPos) && level.shouldTickBlocksAt(scraperPos))) return
        val facing = sourceContext.blockState.getValue(ObservationSourceContainerBlock.FACING)
        val observationPos = scraperPos.relative(facing)
        POS_KEY.set(GlobalPos(level.dimension(), observationPos))
        val state = level.getBlockState(observationPos)
        if (state.hasAnalogOutputSignal()) {
            var analogValue = 0
            var advancedAnalogValue = 0.0
            val server = level.server
            if (server != null) {
                server.executeBlocking {
                    analogValue = state.getAnalogOutputSignal(level, observationPos)
                    if (recorder.supportsFloating)
                        advancedAnalogValue = getAdvancedAnalogValue(level, state, observationPos, analogValue)
                }
            } else {
                analogValue = state.getAnalogOutputSignal(level, observationPos)
                if (recorder.supportsFloating)
                    advancedAnalogValue = getAdvancedAnalogValue(level, state, observationPos, analogValue)
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

    fun getAdvancedAnalogValue(level: Level, blockState: BlockState, blockPos: BlockPos, fallback: Int): Double {
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
