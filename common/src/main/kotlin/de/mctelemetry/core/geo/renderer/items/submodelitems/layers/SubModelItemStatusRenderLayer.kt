package de.mctelemetry.core.geo.renderer.items.submodelitems.layers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityStatusRenderLayer
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityStatusRenderLayer.Companion.getStatusAnimationForStateType
import de.mctelemetry.core.geo.renderer.blocks.entities.layers.ScraperBlockEntityStatusRenderLayer.Companion.getStatusTextureForStateType
import de.mctelemetry.core.items.GeoSubModelItem
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

class SubModelItemStatusRenderLayer(entityRendererIn: GeoRenderer<GeoSubModelItem>) :
    GeoRenderLayer<GeoSubModelItem>(entityRendererIn) {

    object StatusModel : DefaultedGeoModel<GeoSubModelItem>(
        ScraperBlockEntityStatusRenderLayer.StatusModel.assetSubPath
    ) {
        override fun subtype(): String = ScraperBlockEntityStatusRenderLayer.StatusModel.subtype()

        private fun getStatus(animatable: GeoSubModelItem): ObservationSourceErrorState.Type {
            return ObservationSourceErrorState.Type.NotConfigured
        }

        @Deprecated("Deprecated")
        override fun getTextureResource(animatable: GeoSubModelItem): ResourceLocation {
            return getStatusTextureForStateType(
                getStatus(animatable)
            )
        }

        override fun getAnimationResource(animatable: GeoSubModelItem): ResourceLocation {
            return getStatusAnimationForStateType(
                getStatus(animatable)
            )
        }
    }

    override fun render(
        poseStack: PoseStack,
        animatable: GeoSubModelItem,
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
        val statusRenderType = StatusModel.getRenderType(animatable, statusTexture) ?: return
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
