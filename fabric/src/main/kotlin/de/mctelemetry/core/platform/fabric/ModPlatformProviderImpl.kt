package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.IItemStorageAccessor
import de.mctelemetry.core.platform.ModPlatform

@Suppress("unused")
object ModPlatformProviderImpl {
    @JvmStatic
    fun getPlatform(): ModPlatform {
        return FabricModPlatform
    }

    object FabricModPlatform : ModPlatform {
        override fun getPlatformName(): String = "Fabric"

        override fun getItemStorageAccessor(): IItemStorageAccessor = ItemStorageAccessor
    }
}
