package de.mctelemetry.core.observations.scrapers.redstone

import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.IMappedAttributeKeySet
import de.mctelemetry.core.api.attributes.invoke
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

    private val POS_KEY = BuiltinAttributeKeyTypes.GlobalPosType("pos")
    private val DIR_KEY = BuiltinAttributeKeyTypes.DirectionType("dir")

    override val attributes: IMappedAttributeKeySet = IMappedAttributeKeySet(POS_KEY, DIR_KEY)

    override fun observe(
        context: BlockEntity,
        recorder: IObservationRecorder.Unresolved,
        attributes: IMappedAttributeValueLookup.MapLookup,
        unusedAttributes: Set<MappedAttributeKeyInfo<*, *>>,
    ) {
        val level = context.level
        if (level == null || context.isRemoved) return
        val scraperPos = context.blockPos
        if (!(level.isLoaded(scraperPos) && level.shouldTickBlocksAt(scraperPos))) return
        val facing = context.blockState.getValue(ObservationSourceContainerBlock.FACING)
        val observationPos = scraperPos.relative(facing)
        attributes[POS_KEY] = GlobalPos(level.dimension(), observationPos)
        if (DIR_KEY in unusedAttributes) {
            attributes[DIR_KEY] = null
            val signal = level.getSignal(observationPos, facing)
            recorder.observe(
                if (signal != 0) signal.toLong()
                else level.getBlockState(observationPos).let {
                    if (it.block === Blocks.REDSTONE_WIRE)
                        it.getValue(RedStoneWireBlock.POWER)
                    else 0
                }.toLong(),
                attributes,
                this
            )
        } else {
            for (dir in Direction.entries) {
                attributes[DIR_KEY] = dir
                recorder.observe(
                    level.getSignal(observationPos, dir.opposite).toLong(),
                    attributes,
                    this
                )
            }
        }
    }
}
