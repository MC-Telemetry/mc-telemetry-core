package de.mctelemetry.core.neoforge

import de.mctelemetry.core.OTelCoreMod
import net.neoforged.fml.common.Mod

@Mod(OTelCoreMod.MOD_ID)
object OTelCoreModNeoForge {

    init {
        OTelCoreMod.init()
    }
}
