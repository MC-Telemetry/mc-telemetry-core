package de.mctelemetry.core.geo.renderer.blocks.entities.layers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.entities.ScraperBlockEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.DefaultedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer
import kotlin.jvm.optionals.getOrElse

class ScraperCoreRenderLayer(entityRendererIn: GeoRenderer<ScraperBlockEntity>) :
    GeoRenderLayer<ScraperBlockEntity>(entityRendererIn) {

    object CoreModel : DefaultedGeoModel<ScraperBlockEntity>(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID,
            "default"
        )
    ) {
        override fun subtype(): String = "mcotelcore/scraper_core"

        private fun getBlockResourceLocation(animatable: ScraperBlockEntity): ResourceLocation? {
            val holder = animatable.blockState.blockHolder
            return holder.unwrapKey().getOrElse {
                logMissingBlockKeyWarning(holder)
                return null
            }.location()
        }

        private fun getCoreTextureForBlockResource(blockResourceLocation: ResourceLocation): ResourceLocation {
            return blockResourceLocation.withPath("textures/mcotelcore/scraper_core/${blockResourceLocation.path}.png")
        }

        private fun getCoreAnimationForBlockResource(blockResourceLocation: ResourceLocation): ResourceLocation {
            return blockResourceLocation.withPath("animations/mcotelcore/scraper_core/${blockResourceLocation.path}.animation.json")
        }

        @Deprecated("Deprecated")
        override fun getTextureResource(animatable: ScraperBlockEntity): ResourceLocation {
            return getCoreTextureForBlockResource(
                getBlockResourceLocation(animatable)
                    ?: return MissingTextureAtlasSprite.getLocation()
            )
        }

        override fun getAnimationResource(animatable: ScraperBlockEntity): ResourceLocation {
            return getCoreAnimationForBlockResource(
                getBlockResourceLocation(animatable)
                    ?: return MissingTextureAtlasSprite.getLocation().withSuffix(".animation.json")
            )
        }
    }

    companion object {

        private val missingBlockWarningsLogged: MutableSet<Block> = mutableSetOf()
        private fun logMissingBlockKeyWarning(holder: Holder<Block>) {
            val block = holder.value()!!
            if (missingBlockWarningsLogged.add(block)) {
                OTelCoreMod.logger.warn("Could not find id of block {}", block)
            }
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
        val coreModel = CoreModel.getBakedModel(CoreModel.getModelResource(animatable))
        val coreTexture = CoreModel.getTextureResource(animatable, renderer)
        val coreRenderType = CoreModel.getRenderType(animatable, coreTexture) ?: return
        renderer.reRender(
            coreModel,
            poseStack,
            bufferSource,
            animatable,
            coreRenderType,
            if (coreRenderType === renderType) buffer else bufferSource.getBuffer(coreRenderType),
            partialTick,
            packedLight,
            packedOverlay,
            renderer.getRenderColor(animatable, partialTick, packedLight).argbInt,
        )
    }
}
