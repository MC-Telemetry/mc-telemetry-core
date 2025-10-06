package de.mctelemetry.core.forge

import dev.architectury.platform.forge.EventBuses
import de.mctelemetry.core.OTelCoreMod
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(OTelCoreMod.MOD_ID)
object OTelCoreModForge {

    init {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(OTelCoreMod.MOD_ID, MOD_BUS)
        OTelCoreMod.init()
    }
}
