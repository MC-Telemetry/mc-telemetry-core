package de.mctelemetry.core.geo.renderer.blocks.entities.layers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.ObservationSourceContainerBlock
import de.mctelemetry.core.blocks.entities.ScraperBlockEntity
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.GeckoLibCache
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer
import software.bernie.geckolib.util.GeckoLibUtil

class ScraperStatusRenderLayer(entityRendererIn: GeoRenderer<ScraperBlockEntity>) :
    GeoRenderLayer<ScraperBlockEntity>(entityRendererIn) {

    companion object {
        //val NotConfiguredTexture = ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "scraper_ok")
        val ErrorModelLocation = ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "textures/block/scraper_error.png")
        //val ErrorModelLocation = ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "geo/block/scraper_error")
        operator fun get(errorState: ObservationSourceErrorState.Type): ResourceLocation? {
            return when(errorState) {
                ObservationSourceErrorState.Type.Errors -> ErrorModelLocation
                else -> null
            }
        }

        val ERROR_STATUS_MODEL: GeoModel<ScraperBlockEntity> = DefaultedBlockGeoModel(ErrorModelLocation)
    }

    override fun render(
        poseStack: PoseStack,
        animatable: ScraperBlockEntity,
        bakedModel: BakedGeoModel,
        renderType: RenderType?,
        bufferSource: MultiBufferSource,
        buffer: VertexConsumer?,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int
    ) {
        super.render(
            poseStack,
            animatable,
            bakedModel,
            renderType,
            bufferSource,
            buffer,
            partialTick,
            packedLight,
            packedOverlay
        )
        if(buffer == null) return
        poseStack.pushPose()
        try {
            val renderType2 = RenderType.entityTranslucent(ErrorModelLocation)
            poseStack.scale(1.00001F,1.00001F,1.00001F)
            renderer.reRender(
                bakedModel,//ERROR_STATUS_MODEL.getBakedModel(ErrorModelLocation),
                poseStack,
                bufferSource,
                animatable,
                renderType2,
                bufferSource.getBuffer(renderType2),
                partialTick,
                packedLight,
                packedOverlay,
                renderer.getRenderColor(animatable, partialTick, packedLight).argbInt,
            )
        } finally {
            poseStack.popPose()
        }
    }

    override fun getTextureResource(animatable: ScraperBlockEntity): ResourceLocation {
        val errorState = animatable.blockState.getValue(ObservationSourceContainerBlock.ERROR)
        return ScraperStatusRenderLayer[errorState] ?: super.getTextureResource(animatable)
    }
}
