package de.mctelemetry.core.observations.scrapers.fluid

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.stores.MapAttributeStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import de.mctelemetry.core.platform.ModPlatformProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity

object FluidScraperFillRatioObservationSource :
    PositionObservationSourceBase.PositionSingletonBase<FluidScraperFillRatioObservationSource>() {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "fluid_scraper.fill_ratio")
    )

    context(sourceContext: BlockEntity, attributeStore: MapAttributeStore)
    override fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        var ratio = 0.0
        level.server.executeBlocking {
            ratio = ModPlatformProvider.getPlatform().getFluidStorageAccessor().getFillRatio(level, position, facing)
        }

        recorder.observe(ratio, this)
    }
}
