package de.mctelemetry.core.observations.scrapers.fluid

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IAttributeValueStore
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
import net.minecraft.world.level.material.Fluid

object FluidScraperAmountObservationSource :
    PositionObservationSourceBase.PositionSingletonBase<FluidScraperAmountObservationSource>() {

    val observedItem = BuiltinAttributeKeyTypes.FluidType.createObservationAttributeReference("fluid")

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "fluid_scraper.amount")
    )

    context(sourceContext: BlockEntity, attributeStore: IAttributeValueStore.MapAttributeStore)
    override fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        var map: Map<Fluid, Long>? = null
        level.server.executeBlocking {
            map = ModPlatformProvider.getPlatform().getFluidStorageAccessor().getFluidAmounts(level, position, facing)
        }

        if (map == null) return
        for ((fluid, count) in map) {
            observedItem.set(fluid)
            recorder.observe(count, this)
        }
    }
}
