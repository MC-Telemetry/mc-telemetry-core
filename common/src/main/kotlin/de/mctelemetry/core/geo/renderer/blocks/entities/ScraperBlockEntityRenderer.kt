package de.mctelemetry.core.geo.renderer.blocks.entities

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.blocks.entities.ScraperBlockEntity
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityCoreRenderLayer
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityStatusRenderLayer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoBlockRenderer

class ScraperBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
    GeoBlockRenderer<ScraperBlockEntity>(
        model()
    ) {

    companion object {
        val Model: GeoModel<GeoAnimatable> =
            DefaultedBlockGeoModel(ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "scraper"))

        @Suppress("UNCHECKED_CAST")
        fun <T: GeoAnimatable> model() = Model as GeoModel<T>
    }

    init {
        addRenderLayer(ScraperBlockEntityCoreRenderLayer(this))
        addRenderLayer(ScraperBlockEntityStatusRenderLayer(this))
    }
}
