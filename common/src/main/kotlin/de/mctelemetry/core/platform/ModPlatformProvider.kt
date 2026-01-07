package de.mctelemetry.core.platform

import dev.architectury.injectables.annotations.ExpectPlatform

object ModPlatformProvider {
    @JvmStatic
    @ExpectPlatform
    fun getPlatform(): ModPlatform {
        throw AssertionError()
    }
}
