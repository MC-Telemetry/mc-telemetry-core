package de.mctelemetry.core.fabric

import de.mctelemetry.core.OTelCoreMod
import net.fabricmc.api.ModInitializer

object OTelCoreModFabric : ModInitializer {

    override fun onInitialize() {
        OTelCoreMod.init()
    }
}
