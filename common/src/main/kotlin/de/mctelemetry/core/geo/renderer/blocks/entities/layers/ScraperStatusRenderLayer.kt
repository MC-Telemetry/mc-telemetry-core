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
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

class ScraperStatusRenderLayer(entityRendererIn: GeoRenderer<ScraperBlockEntity>) :
    GeoRenderLayer<ScraperBlockEntity>(entityRendererIn) {

    companion object {
        val StatusModelLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "geo/block/scraper_error.geo.json")
        val ErrorTextureLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "textures/scraper_status/scraper_error.png")
        val OkTextureLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "textures/scraper_status/scraper_ok.png")
        val WarningTextureLocation: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "textures/scraper_status/scraper_warning.png")
        val NotConfiguredTextureLocation: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID,
            "textures/scraper_status/scraper_not_configured.png"
        )

        fun getStatusTexture(errorState: ObservationSourceErrorState.Type): ResourceLocation {
            return when (errorState) {
                ObservationSourceErrorState.Type.NotConfigured -> NotConfiguredTextureLocation
                ObservationSourceErrorState.Type.Ok -> OkTextureLocation
                ObservationSourceErrorState.Type.Warnings -> WarningTextureLocation
                ObservationSourceErrorState.Type.Errors -> ErrorTextureLocation
            }
        }

        val STATUS_MODEL: GeoModel<ScraperBlockEntity> = DefaultedBlockGeoModel(StatusModelLocation)
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
        val statusModel = STATUS_MODEL.getBakedModel(StatusModelLocation)
        val errorState = animatable.blockState.getValue(ObservationSourceContainerBlock.ERROR)
        val statusRenderType = AutoGlowingTexture.getRenderType(getStatusTexture(errorState))
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
