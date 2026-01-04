package de.mctelemetry.core.observations.scrapers.redstone

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.*
import de.mctelemetry.core.api.attributes.AttributeDataSource.Companion.asObservationDataReference
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.entity.BlockEntity

object RedstoneScraperPowerObservationSource : IObservationSource.MultiAttribute<BlockEntity> {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "redstone_scraper.power")
    )

    override val sourceContextType: Class<BlockEntity> = BlockEntity::class.java

    private val POS_KEY = BuiltinAttributeKeyTypes.GlobalPosType("pos").asObservationDataReference(this)
    private val DIR_KEY = BuiltinAttributeKeyTypes.DirectionType("dir").asObservationDataReference(this)

    override val attributes: IAttributeDateSourceReferenceSet =
        IAttributeDateSourceReferenceSet(listOf(POS_KEY, DIR_KEY))

    context(sourceContext: BlockEntity, attributeStore: IMappedAttributeValueLookup.MapLookup)
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
        if (DIR_KEY in unusedAttributes) {
            DIR_KEY.unset()
            val signal = level.getSignal(observationPos, facing)
            recorder.observe(
                if (signal != 0) signal.toLong()
                else level.getBlockState(observationPos).let {
                    if (it.block === Blocks.REDSTONE_WIRE)
                        it.getValue(RedStoneWireBlock.POWER)
                    else 0
                }.toLong(),
                this
            )
        } else {
            for (dir in Direction.entries) {
                DIR_KEY.set(dir)
                recorder.observe(
                    level.getSignal(observationPos, dir.opposite).toLong(),
                    this
                )
            }
        }
    }
}
