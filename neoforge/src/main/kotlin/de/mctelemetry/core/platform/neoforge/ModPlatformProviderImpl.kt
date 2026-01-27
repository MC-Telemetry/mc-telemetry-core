package de.mctelemetry.core.platform.neoforge

import de.mctelemetry.core.platform.IEnergyStorageAccessor
import de.mctelemetry.core.platform.IFluidStorageAccessor
import de.mctelemetry.core.platform.IItemStorageAccessor
import de.mctelemetry.core.platform.ModPlatform

@Suppress("unused")
object ModPlatformProviderImpl {
    @JvmStatic
    fun getPlatform(): ModPlatform {
        return NeoForgeModPlatform
    }

    object NeoForgeModPlatform : ModPlatform {
        override fun getPlatformName(): String = "NeoForge"

        override fun getItemStorageAccessor(): IItemStorageAccessor = ItemStorageAccessor
        override fun getFluidStorageAccessor(): IFluidStorageAccessor = FluidStorageAccessor
        override fun getEnergyStorageAccessor(): IEnergyStorageAccessor = EnergyStorageAccessor
    }
}
