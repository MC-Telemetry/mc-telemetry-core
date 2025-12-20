package de.mctelemetry.core.observations.scrapers.redstone

import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.AttributeDataSource.Companion.asReference
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.invoke
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity

object RedstoneScraperDirectPowerObservationSource : IObservationSource.MultiAttribute<BlockEntity> {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "redstone_scraper.direct_power")
    )

    override val sourceContextType: Class<BlockEntity> = BlockEntity::class.java

    private val POS_KEY = BuiltinAttributeKeyTypes.GlobalPosType("pos").asReference()
    private val DIR_KEY = BuiltinAttributeKeyTypes.DirectionType("dir").asReference()

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
            recorder.observe(
                level.getDirectSignal(observationPos, facing).toLong(),
                this
            )
        } else {
            for (dir in Direction.entries) {
                DIR_KEY.set(dir)
                recorder.observe(
                    level.getDirectSignal(observationPos, dir.opposite).toLong(),
                    this
                )
            }
        }
    }
}
