package de.mctelemetry.core.geo.renderer.items.blockitems.layers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityStatusRenderLayer
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityStatusRenderLayer.Companion.getStatusAnimationForStateType
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityStatusRenderLayer.Companion.getStatusTextureForStateType
import de.mctelemetry.core.items.ScraperBlockItem
import de.mctelemetry.core.observations.model.ObservationSourceErrorState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.texture.AutoGlowingTexture
import software.bernie.geckolib.model.DefaultedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

class ScraperItemStatusRenderLayer(entityRendererIn: GeoRenderer<ScraperBlockItem>) :
    GeoRenderLayer<ScraperBlockItem>(entityRendererIn) {

    object StatusModel : DefaultedGeoModel<ScraperBlockItem>(
        ScraperBlockEntityStatusRenderLayer.StatusModel.assetSubPath
    ) {
        override fun subtype(): String = ScraperBlockEntityStatusRenderLayer.StatusModel.subtype()

        private fun getStatus(animatable: ScraperBlockItem): ObservationSourceErrorState.Type {
            return ObservationSourceErrorState.Type.Ok
        }

        @Deprecated("Deprecated")
        override fun getTextureResource(animatable: ScraperBlockItem): ResourceLocation {
            return getStatusTextureForStateType(
                getStatus(animatable)
            )
        }

        override fun getAnimationResource(animatable: ScraperBlockItem): ResourceLocation {
            return getStatusAnimationForStateType(
                getStatus(animatable)
            )
        }

        override fun getRenderType(animatable: ScraperBlockItem, texture: ResourceLocation): RenderType {
            return AutoGlowingTexture.getRenderType(texture)
        }
    }

    override fun render(
        poseStack: PoseStack,
        animatable: ScraperBlockItem,
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
        if (buffer == null) return
        val renderer = renderer
        val statusModel = StatusModel.getBakedModel(StatusModel.getModelResource(animatable, renderer))
        val statusTexture = StatusModel.getTextureResource(animatable, renderer)
        val statusRenderType = StatusModel.getRenderType(animatable, statusTexture)
        renderer.reRender(
            statusModel,
            poseStack,
            bufferSource,
            animatable,
            statusRenderType,
            bufferSource.getBuffer(statusRenderType),
            partialTick,
            packedLight,
            packedOverlay,
            renderer.getRenderColor(animatable, partialTick, packedLight).argbInt,
        )
    }
}
