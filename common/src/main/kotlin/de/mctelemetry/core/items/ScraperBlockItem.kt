package de.mctelemetry.core.items

import de.mctelemetry.core.blocks.ScraperBlock
import de.mctelemetry.core.geo.renderer.items.blockitems.ScraperBlockItemRendererFactory
import net.minecraft.world.item.BlockItem
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

class ScraperBlockItem(block: ScraperBlock, properties: Properties) : BlockItem(block, properties), GeoItem {

    private val cache = GeckoLibUtil.createInstanceCache(this)

    override fun getBlock(): ScraperBlock {
        return super.getBlock() as ScraperBlock
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
    }

    override fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        ScraperBlockItemRendererFactory.createGeoRenderer(consumer)
    }
}
