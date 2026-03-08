package de.mctelemetry.core.geo.renderer.blocks.entities

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.blocks.entities.ScraperBlockEntity
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperCoreRenderLayer
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperStatusRenderLayer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

class ScraperBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
    GeoBlockRenderer<ScraperBlockEntity>(
        DefaultedBlockGeoModel(ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "scraper"))
    ) {
    init {
        addRenderLayer(ScraperCoreRenderLayer(this))
        addRenderLayer(ScraperStatusRenderLayer(this))
    }
}
