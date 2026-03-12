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
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.texture.AutoGlowingTexture
import software.bernie.geckolib.model.DefaultedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

class ScraperBlockEntityStatusRenderLayer(entityRendererIn: GeoRenderer<ScraperBlockEntity>) :
    GeoRenderLayer<ScraperBlockEntity>(entityRendererIn) {

    companion object {

        private val statusModelLocation: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID,
            "default"
        )

        val ErrorTextureLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "textures/mcotelcore/scraper_status/scraper_error.png"
            )
        val OkTextureLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "textures/mcotelcore/scraper_status/scraper_ok.png"
            )
        val WarningTextureLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "textures/mcotelcore/scraper_status/scraper_warning.png"
            )
        val NotConfiguredTextureLocation: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID,
            "textures/mcotelcore/scraper_status/scraper_not_configured.png"
        )
        val ErrorAnimationLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "animations/mcotelcore/scraper_status/scraper_error.png"
            )
        val OkAnimationLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "animations/mcotelcore/scraper_status/scraper_ok.png"
            )
        val WarningAnimationLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "animations/mcotelcore/scraper_status/scraper_warning.png"
            )
        val NotConfiguredAnimationLocation: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID,
            "animations/mcotelcore/scraper_status/scraper_not_configured.png"
        )

        fun getStatusTextureForStateType(type: ObservationSourceErrorState.Type): ResourceLocation {
            return when (type) {
                ObservationSourceErrorState.Type.NotConfigured -> NotConfiguredTextureLocation
                ObservationSourceErrorState.Type.Ok -> OkTextureLocation
                ObservationSourceErrorState.Type.Warnings -> WarningTextureLocation
                ObservationSourceErrorState.Type.Errors -> ErrorTextureLocation
            }
        }

        fun getStatusAnimationForStateType(type: ObservationSourceErrorState.Type): ResourceLocation {
            return when (type) {
                ObservationSourceErrorState.Type.NotConfigured -> NotConfiguredAnimationLocation
                ObservationSourceErrorState.Type.Ok -> OkAnimationLocation
                ObservationSourceErrorState.Type.Warnings -> WarningAnimationLocation
                ObservationSourceErrorState.Type.Errors -> ErrorAnimationLocation
            }
        }
    }

    object StatusModel : DefaultedGeoModel<ScraperBlockEntity>(
        statusModelLocation
    ) {
        public override fun subtype(): String = "mcotelcore/scraper_status"

        val assetSubPath: ResourceLocation = statusModelLocation

        private fun getStatus(animatable: ScraperBlockEntity): ObservationSourceErrorState.Type {
            return animatable.blockState.getValue(ObservationSourceContainerBlock.ERROR)
        }

        @Deprecated("Deprecated")
        override fun getTextureResource(animatable: ScraperBlockEntity): ResourceLocation {
            return getStatusTextureForStateType(
                getStatus(animatable)
            )
        }

        override fun getAnimationResource(animatable: ScraperBlockEntity): ResourceLocation {
            return getStatusAnimationForStateType(
                getStatus(animatable)
            )
        }

        override fun getRenderType(animatable: ScraperBlockEntity, texture: ResourceLocation): RenderType? {
            if (animatable.blockState.getValue(ObservationSourceContainerBlock.ERROR) == ObservationSourceErrorState.Type.NotConfigured)
                return super.getRenderType(animatable, texture)
            return AutoGlowingTexture.getRenderType(texture)
        }
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
