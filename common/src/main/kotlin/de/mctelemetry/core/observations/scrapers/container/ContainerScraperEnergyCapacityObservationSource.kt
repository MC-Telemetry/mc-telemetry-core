package de.mctelemetry.core.observations.scrapers.container

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.api.observations.position.PositionObservationSourceBase
import de.mctelemetry.core.platform.ModPlatformProvider
import de.mctelemetry.core.utils.EmptyAutoCloseable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity

object ContainerScraperEnergyCapacityObservationSource :
    PositionObservationSourceBase.PositionSingletonBase.Simple<ContainerScraperEnergyCapacityObservationSource>() {

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "container_scraper.energy.capacity")
    )

    context(sourceOwner: BlockEntity, observationContext: EmptyAutoCloseable, attributeStore: IAttributeValueStore.Mutable)
    override fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        var capacity = 0L
        level.server.executeBlocking {
            capacity =
                ModPlatformProvider.getPlatform().getEnergyStorageAccessor().getEnergyCapacity(level, position, facing)
        }

        recorder.observe(capacity, this)
    }
}
