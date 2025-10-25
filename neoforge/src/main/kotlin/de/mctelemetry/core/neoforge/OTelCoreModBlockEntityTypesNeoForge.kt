package de.mctelemetry.core.neoforge

import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes.registerBlockEntity
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes.writeRegister
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.blocks.entities.RedstoneScraperBlockEntity
import de.mctelemetry.core.blocks.observation.ObservationSourceContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

object OTelCoreModBlockEntityTypesNeoForge {

    fun init() {
        OTelCoreModBlockEntityTypes.REDSTONE_SCRAPER_BLOCK_ENTITY = registerBlockEntity(
            "redstone_scraper_block"
        ) {
            BlockEntityType.Builder
                .of(::RedstoneScraperBlockEntity, OTelCoreModBlocks.REDSTONE_SCRAPER_BLOCK.get())
                .build(null)
        }
        OTelCoreModBlockEntityTypes.OBSERVATION_SOURCE_CONTAINER_BLOCK_ENTITY = registerBlockEntity(
            "observation_source_container_block"
        ) {
            BlockEntityType.Builder
                .of(::ObservationSourceContainerBlockEntity) //TODO: Add blocks
                .build(null)
        }

        writeRegister()
    }
}
