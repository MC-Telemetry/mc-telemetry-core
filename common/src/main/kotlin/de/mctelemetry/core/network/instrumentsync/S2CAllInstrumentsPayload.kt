package de.mctelemetry.core.network.instrumentsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import dev.architectury.networking.NetworkManager
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

class S2CAllInstrumentsPayload(
    val reservedNames: Set<String>,
    val instruments: Collection<IWorldInstrumentDefinition>,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<S2CAllInstrumentsPayload> = TYPE

    companion object {

        fun fromManager(manager: IInstrumentManager): S2CAllInstrumentsPayload {
            val reservedNames: MutableSet<String> = mutableSetOf()
            val instruments = manager.findGlobal().mapNotNull { instrument ->
                if (instrument is IWorldInstrumentDefinition) {
                    instrument
                } else {
                    reservedNames.add(instrument.name.lowercase())
                    return@mapNotNull null
                }
            }.toList()
            return S2CAllInstrumentsPayload(reservedNames, instruments)
        }

        val TYPE = CustomPacketPayload.Type<S2CAllInstrumentsPayload>(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID,
                "instruments.all"
            )
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, S2CAllInstrumentsPayload> = StreamCodec.composite(
            ByteBufCodecs.collection(::HashSet, ByteBufCodecs.STRING_UTF8),
            S2CAllInstrumentsPayload::reservedNames,
            ByteBufCodecs.collection(::ArrayList, IWorldInstrumentDefinition.Record.INTERFACE_STREAM_CODEC),
            S2CAllInstrumentsPayload::instruments,
            ::S2CAllInstrumentsPayload
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
            value: S2CAllInstrumentsPayload,
            // unused but still required for signature of NetworkManager.NetworkReceiver
            @Suppress("unused")
            context: NetworkManager.PacketContext,
        ) {
            ClientInstrumentMetaManager.populate(value)
        }
    }
}
