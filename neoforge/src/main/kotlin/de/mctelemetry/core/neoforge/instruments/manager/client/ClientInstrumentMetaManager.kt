package de.mctelemetry.core.neoforge.instruments.manager.client

import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@OnlyIn(Dist.CLIENT)
fun ClientInstrumentMetaManager.register() {
    FORGE_BUS.addListener(::onLoggingIn)
    FORGE_BUS.addListener(::onLoggingOut)
}

@OnlyIn(Dist.CLIENT)
private fun onLoggingIn(event: ClientPlayerNetworkEvent.LoggingIn) {
    ClientInstrumentMetaManager.create()
}

@OnlyIn(Dist.CLIENT)
private fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
    ClientInstrumentMetaManager.destroy()
}
