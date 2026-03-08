package de.mctelemetry.core.blocks.entities

import de.mctelemetry.core.api.OTelCoreModAPI
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.model.DefaultedBlockGeoModel
import software.bernie.geckolib.renderer.GeoBlockRenderer
import software.bernie.geckolib.util.GeckoLibUtil

open class ScraperBlockEntity(
    blockPos: BlockPos,
    blockState: BlockState,
    blockEntityType: BlockEntityType<out ScraperBlockEntity>
) : ObservationSourceContainerBlockEntity(
    blockEntityType,
    blockPos,
    blockState,
), GeoBlockEntity {

    private val instanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun getType(): BlockEntityType<out ScraperBlockEntity> {
        @Suppress("UNCHECKED_CAST") // known value from constructor
        return blockEntityType as BlockEntityType<out ScraperBlockEntity>
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return instanceCache
    }

    companion object {
        operator fun invoke(blockPos: BlockPos, blockState: BlockState): ScraperBlockEntity =
            ScraperBlockEntity(blockPos, blockState, OTelCoreModBlockEntityTypes.SCRAPER_BLOCK_ENTITY.get())
    }

    class ScraperBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
        GeoBlockRenderer<ScraperBlockEntity>(
            DefaultedBlockGeoModel(ResourceLocation.fromNamespaceAndPath(OTelCoreModAPI.MOD_ID, "scraper"))
        ) {
    }
}
