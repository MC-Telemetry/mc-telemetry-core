package de.mctelemetry.core.network.instrumentsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import dev.architectury.networking.NetworkManager
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import io.netty.buffer.ByteBuf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class S2CReservedNameAddedPayload(
    val name: String,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<S2CReservedNameAddedPayload> = TYPE

    companion object {

        val TYPE: CustomPacketPayload.Type<S2CReservedNameAddedPayload> = CustomPacketPayload.Type(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "instruments.reserved.add"
            )
        )
        val STREAM_CODEC: StreamCodec<ByteBuf, S2CReservedNameAddedPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            S2CReservedNameAddedPayload::name,
            ::S2CReservedNameAddedPayload
        )

        fun register(client: Boolean = Platform.getEnvironment() == Env.CLIENT) {
            if (client) {
                NetworkManager.registerReceiver(NetworkManager.s2c(), TYPE, STREAM_CODEC, ::receive)
            } else {
                NetworkManager.registerS2CPayloadType(TYPE, STREAM_CODEC)
            }
        }

        @Environment(EnvType.CLIENT)
        private fun receive(
            value: S2CReservedNameAddedPayload,
            // unused but still required for signature of NetworkManager.NetworkReceiver
            @Suppress("unused")
            context: NetworkManager.PacketContext,
        ) {
            ClientInstrumentMetaManager.addReservedName(value.name)
        }
    }
}
