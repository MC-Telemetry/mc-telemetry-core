package de.mctelemetry.core.blocks

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.BaseEntityBlock

class NbtScraperBlock(properties: Properties) : ScraperBlock(properties.noOcclusion()) {

    override fun codec(): MapCodec<out BaseEntityBlock> {
        return CODEC
    }

    companion object {
        val CODEC: MapCodec<ObservationSourceContainerBlock> = simpleCodec(::NbtScraperBlock)
    }

}
