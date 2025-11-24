package de.mctelemetry.core.network.instrumentsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import dev.architectury.networking.NetworkManager
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.SharedConstants
import net.minecraft.Util
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager

data class A2AInstrumentAddedPayload(
    val instrument: IWorldInstrumentDefinition,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<A2AInstrumentAddedPayload> = TYPE

    companion object {

        private val subLogger =
            LogManager.getLogger("${OTelCoreMod.MOD_ID}.${A2AInstrumentAddedPayload::class.java.simpleName}")

        val TYPE: CustomPacketPayload.Type<A2AInstrumentAddedPayload> = CustomPacketPayload.Type(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "instruments.add"
            )
        )
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, A2AInstrumentAddedPayload> = StreamCodec.composite(
            IWorldInstrumentDefinition.Record.INTERFACE_STREAM_CODEC,
            A2AInstrumentAddedPayload::instrument,
            ::A2AInstrumentAddedPayload
        )

        fun register(client: Boolean = Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.c2s(), TYPE, STREAM_CODEC, ::receiveC2S)
            if (client) {
                NetworkManager.registerReceiver(NetworkManager.s2c(), TYPE, STREAM_CODEC, ::receiveS2C)
            } else {
                NetworkManager.registerS2CPayloadType(TYPE, STREAM_CODEC)
            }
        }


        private fun receiveC2S(
            value: A2AInstrumentAddedPayload,
            // unused but still required for signature of NetworkManager.NetworkReceiver
            @Suppress("unused")
            context: NetworkManager.PacketContext,
        ) {
            val instrument = value.instrument
            try {
                val server = context.player.server!!
                val manager = server.instrumentManager!!
                manager.gaugeWorldInstrument(instrument.name) {
                    importWorldInstrument(instrument)
                }.let {
                    if (instrument.supportsFloating)
                        it.registerMutableOfDouble()
                    else
                        it.registerMutableOfLong()
                }
            } catch (ex: RuntimeException) {
                if (SharedConstants.IS_RUNNING_IN_IDE) {
                    Util.logAndPauseIfInIde("Exception during handling of add-instrument-request: $value", ex)
                } else {
                    subLogger.debug("Exception during handling of add-instrument-request: {}", value, ex)
                }
            }
            subLogger.trace("Successfully added instrument {} to server", value.instrument)
        }

        @Environment(EnvType.CLIENT)
        private fun receiveS2C(
            value: A2AInstrumentAddedPayload,
            // unused but still required for signature of NetworkManager.NetworkReceiver
            @Suppress("unused")
            context: NetworkManager.PacketContext,
        ) {
            ClientInstrumentMetaManager.addReceivedInstrument(value.instrument)
        }
    }
}
