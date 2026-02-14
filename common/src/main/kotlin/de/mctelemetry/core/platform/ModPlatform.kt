package de.mctelemetry.core.platform

interface ModPlatform {

    fun getPlatformName(): String

    fun getItemStorageAccessor(): IItemStorageAccessor
    fun getFluidStorageAccessor(): IFluidStorageAccessor
    fun getEnergyStorageAccessor(): IEnergyStorageAccessor

    companion object : ModPlatform by ModPlatformProvider.getPlatform()
}
