package de.mctelemetry.core.platform.neoforge

import de.mctelemetry.core.platform.ModPlatform

object ModPlatformProviderImpl {
    @JvmStatic
    fun getPlatform(): ModPlatform {
        return NeoForgeModPlatform
    }

    object NeoForgeModPlatform : ModPlatform {
        override fun getPlatformName(): String = "NeoForge"
    }
}
