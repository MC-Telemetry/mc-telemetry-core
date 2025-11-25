package de.mctelemetry.core.fabric.instruments.manager.client

import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener

@Environment(EnvType.CLIENT)
fun ClientInstrumentMetaManager.register() {
    ClientPlayConnectionEvents.INIT.register(::onPlayInit)
    ClientPlayConnectionEvents.DISCONNECT.register(::onPlayDisconnect)
}

@Environment(EnvType.CLIENT)
private fun onPlayInit(handler: ClientPacketListener, client: Minecraft) {
    ClientInstrumentMetaManager.create()
}

@Environment(EnvType.CLIENT)
private fun onPlayDisconnect(handler: ClientPacketListener, client: Minecraft) {
    ClientInstrumentMetaManager.destroy()
}
