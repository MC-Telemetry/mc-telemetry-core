package de.mctelemetry.core.items

import de.mctelemetry.core.geo.renderer.blocks.entities.ScraperBlockEntityRenderer
import de.mctelemetry.core.geo.renderer.items.blockitems.ScraperBlockItemRendererFactory
import de.mctelemetry.core.geo.renderer.items.submodelitems.SubModelItemRendererFactory
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.model.DefaultedItemGeoModel
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class GeoSubModelItem(
    properties: Properties,
    private val id: ResourceLocation,
    private val includeStatusLayer: Boolean = false
) : Item(properties), GeoItem {

    private val cache = GeckoLibUtil.createInstanceCache(this)

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
    }

    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        val model = object: DefaultedItemGeoModel<GeoSubModelItem>(id) {
            override fun getTextureResource(animatable: GeoSubModelItem): ResourceLocation {
                return ScraperBlockEntityRenderer.model<GeoSubModelItem>().getTextureResource(animatable, null)
            }
        }
        SubModelItemRendererFactory.createGeoRenderer(
            includeStatusLayer,
            model,
            consumer,
        )
    }
}
