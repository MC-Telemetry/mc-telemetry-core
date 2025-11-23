package de.mctelemetry.core.network.observations.container.observationrequest

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import dev.architectury.networking.NetworkManager
import net.minecraft.core.GlobalPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

typealias S2CObservationsPayloadObservationType = Map<IObservationSource<*, *>, RecordedObservations>

data class S2CObservationsPayload(
    val blockPos: GlobalPos,
    val observations: S2CObservationsPayloadObservationType,
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
                ByteBufCodecs.registry(OTelCoreModAPI.ObservationSources),
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

        fun register() {
            NetworkManager.registerReceiver(NetworkManager.s2c(), TYPE, STREAM_CODEC, Receiver)
        }
    }

    object Receiver : NetworkManager.NetworkReceiver<S2CObservationsPayload> {

        override fun receive(value: S2CObservationsPayload, context: NetworkManager.PacketContext) {
            ObservationRequestManagerClient.getActiveManager().acceptObservationPayload(value)
        }
    }
}
