package de.mctelemetry.core.platform

interface ModPlatform {

    fun getPlatformName(): String

    companion object : ModPlatform by ModPlatformProvider.getPlatform()
}
