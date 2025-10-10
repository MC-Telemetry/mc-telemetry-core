package de.mctelemetry.core.neoforge

import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes.registerBlockEntity
import de.mctelemetry.core.blocks.entities.OTelCoreModBlockEntityTypes.writeRegister
import de.mctelemetry.core.blocks.OTelCoreModBlocks
import de.mctelemetry.core.blocks.entities.RubyBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

object OTelCoreModBlockEntityTypesNeoForge {
    fun init() {
        OTelCoreModBlockEntityTypes.RUBY_BLOCK_ENTITY = registerBlockEntity(
            "ruby_block"
        ) {
            BlockEntityType.Builder
                .of(::RubyBlockEntity, OTelCoreModBlocks.RUBY_BLOCK.get())
                .build(null)
        }

        writeRegister()
    }
}
