package de.mctelemetry.core.blocks.entities

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import software.bernie.geckolib.animatable.GeoBlockEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

open class ScraperBlockEntity(
    blockPos: BlockPos,
    blockState: BlockState,
    blockEntityType: BlockEntityType<out ScraperBlockEntity>
) : ObservationSourceContainerBlockEntity<ScraperBlockEntity>(
    blockEntityType,
    blockPos,
    blockState,
), GeoBlockEntity {

    private val instanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun getType(): BlockEntityType<out ScraperBlockEntity> {
        @Suppress("UNCHECKED_CAST") // known value from constructor
        return blockEntityType as BlockEntityType<out ScraperBlockEntity>
    }

    override val context: ScraperBlockEntity?
        get() = this

    override val contextClass: Class<out ScraperBlockEntity>
        get() = ScraperBlockEntity::class.java

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return instanceCache
    }

    companion object {
        operator fun invoke(blockPos: BlockPos, blockState: BlockState): ScraperBlockEntity =
            ScraperBlockEntity(blockPos, blockState, OTelCoreModBlockEntityTypes.SCRAPER_BLOCK_ENTITY.get())
    }
}
