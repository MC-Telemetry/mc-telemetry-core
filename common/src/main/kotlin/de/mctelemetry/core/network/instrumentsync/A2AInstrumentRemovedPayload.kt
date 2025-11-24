package de.mctelemetry.core.network.instrumentsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.instrumentManager
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

data class A2AInstrumentRemovedPayload(
    val instrument: IWorldInstrumentDefinition,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<A2AInstrumentRemovedPayload> = TYPE

    companion object {

        private val subLogger =
            LogManager.getLogger("${OTelCoreMod.MOD_ID}.${A2AInstrumentRemovedPayload::class.java.simpleName}")

        val TYPE: CustomPacketPayload.Type<A2AInstrumentRemovedPayload> = CustomPacketPayload.Type(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "instruments.remove"
            )
        )
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, A2AInstrumentRemovedPayload> = StreamCodec.composite(
            IWorldInstrumentDefinition.Record.INTERFACE_STREAM_CODEC,
            A2AInstrumentRemovedPayload::instrument,
            ::A2AInstrumentRemovedPayload
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
            value: A2AInstrumentRemovedPayload,
            // unused but still required for signature of NetworkManager.NetworkReceiver
            @Suppress("unused")
            context: NetworkManager.PacketContext,
        ) {
            val instrument = value.instrument
            try {
                val server = context.player.server!!
                val manager = server.instrumentManager!!
                val mutableRegistration = manager.findLocalMutable(instrument.name)
                    ?: throw NoSuchElementException("Could not find local mutable instrument with name ${instrument.name}")
                mutableRegistration.close()
            } catch (ex: RuntimeException) {
                if (SharedConstants.IS_RUNNING_IN_IDE) {
                    Util.logAndPauseIfInIde("Exception during handling of remove-instrument-request: $value", ex)
                } else {
                    subLogger.debug("Exception during handling of remove-instrument-request: {}", value, ex)
                }
            }
            subLogger.trace("Successfully removed instrument {} from server", value.instrument)
        }

        @Environment(EnvType.CLIENT)
        private fun receiveS2C(
            value: A2AInstrumentRemovedPayload,
            // unused but still required for signature of NetworkManager.NetworkReceiver
            @Suppress("unused")
            context: NetworkManager.PacketContext,
        ) {
            ClientInstrumentMetaManager.removeReceivedInstrument(value.instrument)
        }
    }
}
