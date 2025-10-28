package de.mctelemetry.core.observations.scrapers.redstone

import de.mctelemetry.core.api.metrics.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.metrics.IMappedAttributeValueLookup
import de.mctelemetry.core.api.metrics.IObservationObserver
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.api.metrics.invoke
import de.mctelemetry.core.blocks.RedstoneScraperBlock
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity

object RedstoneDirectValueScraperObservationSource : IObservationSource<BlockEntity, IMappedAttributeValueLookup.MapLookup> {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "redstone_scraper.direct")
    )

    override val contextType: Class<BlockEntity> = BlockEntity::class.java

    private val POS_KEY = BuiltinAttributeKeyTypes.GlobalPosType("pos")
    private val DIR_KEY = BuiltinAttributeKeyTypes.DirectionType("dir")

    override fun createAttributeLookup(
        context: BlockEntity,
        attributes: IMappedAttributeValueLookup,
    ): IMappedAttributeValueLookup.MapLookup {
        return IMappedAttributeValueLookup.MapLookup(
            mapOf(
                POS_KEY to null,
                DIR_KEY to null,
            ), attributes
        )
    }

    override fun observe(
        context: BlockEntity,
        observer: IObservationObserver.Unresolved,
        attributes: IMappedAttributeValueLookup.MapLookup,
        unusedAttributes: Set<MappedAttributeKeyInfo<*, *>>,
    ) {
        val level = context.level
        if (level == null || context.isRemoved) return
        val scraperPos = context.blockPos
        if (!(level.isLoaded(scraperPos) && level.shouldTickBlocksAt(scraperPos))) return
        val facing = context.blockState.getValue(RedstoneScraperBlock.FACING)
        val observationPos = scraperPos.relative(facing)
        attributes[POS_KEY] = GlobalPos(level.dimension(), observationPos)
        if (DIR_KEY in unusedAttributes) {
            attributes[DIR_KEY] = null
            observer.observe(
                this,
                level.getDirectSignalTo(observationPos).toLong(),
                attributes
            )
        } else {
            for (dir in Direction.entries) {
                attributes[DIR_KEY] = dir
                observer.observe(
                    this,
                    level.getDirectSignal(observationPos.relative(dir), dir).toLong(),
                    attributes
                )
            }
        }
    }
}
