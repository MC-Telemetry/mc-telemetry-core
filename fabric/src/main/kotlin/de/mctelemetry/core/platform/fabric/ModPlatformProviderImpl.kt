package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.IItemStorageAccessor
import de.mctelemetry.core.platform.ModPlatform
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel

object ModPlatformProviderImpl {
    @JvmStatic
    fun getPlatform(): ModPlatform {
        return FabricModPlatform
    }

    object FabricModPlatform : ModPlatform {
        override fun getPlatformName(): String = "Fabric"

        override fun getItemStorageAccessor(
            level: ServerLevel,
            position: BlockPos,
            facing: Direction?
        ): IItemStorageAccessor {
            return ItemStorageAccessor(level, position, facing)
        }
    }
}
