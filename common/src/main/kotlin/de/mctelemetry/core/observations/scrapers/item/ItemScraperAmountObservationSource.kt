package de.mctelemetry.core.observations.scrapers.item

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
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
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.BlockEntity

object ItemScraperAmountObservationSource :
    PositionObservationSourceBase.PositionSingletonBase<ItemScraperAmountObservationSource>() {

    val observedItem = BuiltinAttributeKeyTypes.ItemType.createObservationAttributeReference("item")

    override val id: ResourceKey<IObservationSource<*, *>> = ResourceKey.create(
        OTelCoreModAPI.ObservationSources,
        ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "item_scraper.amount")
    )

    context(sourceContext: BlockEntity, attributeStore: MapAttributeStore)
    override fun observePosition(
        recorder: IObservationRecorder.Unresolved,
        level: ServerLevel,
        position: BlockPos,
        facing: Direction?,
        unusedAttributes: Set<AttributeDataSource<*>>
    ) {
        var map: Map<Item, Long>? = null
        level.server.executeBlocking {
            map = ModPlatformProvider.getPlatform().getItemStorageAccessor().getItemAmounts(level, position, facing)
        }

        if (map == null) return
        for ((item, count) in map) {
            observedItem.set(item)
            recorder.observe(count, this)
        }
    }
}
