package de.mctelemetry.core.observations.scrapers.redstone

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.position.side.PositionSideObservationSourceBase
import de.mctelemetry.core.utils.observe
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity

object RedstoneScraperDirectPowerObservationSource :
    PositionSideObservationSourceBase.PositionSideSingletonBase<RedstoneScraperDirectPowerObservationSource>() {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "redstone_scraper.direct_power")
    )

    context(sourceContext: BlockEntity, attributeStore: IAttributeValueStore.MapAttributeStore)
    override fun observeSide(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        side: Direction,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        recorder.observe(level.getDirectSignal(position, side.opposite))
    }
}
