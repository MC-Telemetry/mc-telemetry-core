package de.mctelemetry.core.network.observations.container.observationrequest

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.observations.model.ObservationSourceStateID
import dev.architectury.networking.NetworkManager
import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import net.minecraft.core.GlobalPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

typealias ObservationSourceObservationMap = Map<Pair<IObservationSource<*, *>, ObservationSourceStateID>, RecordedObservations>

data class S2CObservationsPayload(
    val blockPos: GlobalPos,
    val observations: ObservationSourceObservationMap,
    val serverTick: Long?,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<S2CObservationsPayload> = TYPE

    companion object {

        val TYPE = CustomPacketPayload.Type<S2CObservationsPayload>(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID, "observations.response"
            )
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, S2CObservationsPayload> = StreamCodec.composite(
            GlobalPos.STREAM_CODEC,
            S2CObservationsPayload::blockPos,
            ByteBufCodecs.map(
                { HashMap(it) },
                StreamCodec.composite(
                    ByteBufCodecs.registry(OTelCoreModAPI.ObservationSources),
                    Pair<IObservationSource<*, *>,*>::first,
                    ByteBufCodecs.BYTE,
                    {it.second.toByte()},
                    {source,id -> source to id.toUByte()}
                ),
                RecordedObservations.STREAM_CODEC,
            ),
            S2CObservationsPayload::observations,
            ByteBufCodecs.VAR_LONG,
            {
                val tick = it.serverTick
                when (tick) {
                    null -> 0L
                    -1L -> {
                        OTelCoreMod.logger.warn(
                            "Serializing {} with serverTick of -1, will be mapped to null during transport",
                            S2CObservationsPayload::class.java.simpleName
                        )
                        0L
                    }
                    else -> tick + 1L
                }
            }
        ) { pos, observations, tick ->
            if (tick == 0L) {
                S2CObservationsPayload(pos, observations, null)
            } else {
                S2CObservationsPayload(pos, observations, tick - 1)
            }
        }

        fun register(client: Boolean = Platform.getEnvironment() == Env.CLIENT) {
            if (client) {
                NetworkManager.registerReceiver(NetworkManager.s2c(), TYPE, STREAM_CODEC, Receiver)
            } else {
                NetworkManager.registerS2CPayloadType(TYPE, STREAM_CODEC)
            }
        }
    }

    object Receiver : NetworkManager.NetworkReceiver<S2CObservationsPayload> {

        override fun receive(value: S2CObservationsPayload, context: NetworkManager.PacketContext) {
            ObservationRequestManagerClient.getActiveManager().acceptObservationPayload(value)
        }
    }
}
