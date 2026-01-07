package de.mctelemetry.core.platform.fabric

import de.mctelemetry.core.platform.ModPlatform

object ModPlatformProviderImpl {
    @JvmStatic
    fun getPlatform(): ModPlatform {
        return FabricModPlatform
    }

    object FabricModPlatform : ModPlatform {
        override fun getPlatformName(): String = "Fabric"
    }
}
