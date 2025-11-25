package de.mctelemetry.core.network.instrumentsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
import dev.architectury.networking.NetworkManager
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object C2SAllInstrumentRequestPayload : CustomPacketPayload {

    val TYPE: CustomPacketPayload.Type<C2SAllInstrumentRequestPayload> = CustomPacketPayload.Type(
        ResourceLocation.fromNamespaceAndPath(
            OTelCoreMod.MOD_ID, "instruments.all.request"
        )
    )

    val STREAM_CODEC: StreamCodec<ByteBuf, C2SAllInstrumentRequestPayload> = StreamCodec.unit(
        C2SAllInstrumentRequestPayload
    )

    override fun type(): CustomPacketPayload.Type<C2SAllInstrumentRequestPayload> = TYPE

    fun register() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), TYPE, STREAM_CODEC, ::onReceive)
    }


    private fun onReceive(
        // unused but still required for signature of NetworkManager.NetworkReceiver
        @Suppress("unused")
        value: C2SAllInstrumentRequestPayload,
        context: NetworkManager.PacketContext,
    ) {
        val player = context.player as ServerPlayer
        NetworkManager.sendToPlayer(player, S2CAllInstrumentsPayload.fromManager(player.server.instrumentManager!!))
    }
}
