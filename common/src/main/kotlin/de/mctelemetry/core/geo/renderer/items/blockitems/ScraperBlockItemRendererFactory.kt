package de.mctelemetry.core.geo.renderer.items.blockitems

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import de.mctelemetry.core.geo.renderer.blocks.entities.ScraperBlockEntityRenderer
import de.mctelemetry.core.geo.renderer.items.blockitems.layers.ScraperItemCoreRenderLayer
import de.mctelemetry.core.geo.renderer.items.blockitems.layers.ScraperItemStatusRenderLayer
import de.mctelemetry.core.items.ScraperBlockItem
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import software.bernie.geckolib.animatable.client.GeoRenderProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoItemRenderer
import java.util.function.Consumer

object ScraperBlockItemRendererFactory {
    // createGeoRenderer in GeoItem explicitly requires anonymous classes for some reason
    fun createGeoRenderer(consumer: Consumer<GeoRenderProvider>) {
        consumer.accept(object : GeoRenderProvider {
            private val renderer: GeoItemRenderer<ScraperBlockItem> by lazy {
                (object : GeoItemRenderer<ScraperBlockItem>(ScraperBlockEntityRenderer.model<ScraperBlockItem>()) {
                    override fun preRender(
                        poseStack: PoseStack,
                        animatable: ScraperBlockItem,
                        model: BakedGeoModel,
                        bufferSource: MultiBufferSource?,
                        buffer: VertexConsumer?,
                        isReRender: Boolean,
                        partialTick: Float,
                        packedLight: Int,
                        packedOverlay: Int,
                        colour: Int
                    ) {
                        super.preRender(
                            poseStack,
                            animatable,
                            model,
                            bufferSource,
                            buffer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            colour
                        )
                        if (!isReRender)
                            poseStack.translate(0f, -0.51f, 0f);
                    }
                }).apply {
                    addRenderLayer(ScraperItemCoreRenderLayer(this@apply))
                    addRenderLayer(ScraperItemStatusRenderLayer(this@apply))
                }
            }

            override fun getGeoItemRenderer(): BlockEntityWithoutLevelRenderer {
                return renderer
            }
        })
    }
}
