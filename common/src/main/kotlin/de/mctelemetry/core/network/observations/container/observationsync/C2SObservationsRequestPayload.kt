package de.mctelemetry.core.network.observations.container.observationsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.network.observations.container.observationsync.ObservationSyncManagerServer.Companion.observationSyncManager
import dev.architectury.networking.NetworkManager
import net.minecraft.core.GlobalPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

data class C2SObservationsRequestPayload(
    val blockPos: GlobalPos,
    val requestType: ObservationRequestType,
    val clientTick: Long?,
) : CustomPacketPayload {

    sealed class ObservationRequestType {
        object Stop : ObservationRequestType()
        object Single : ObservationRequestType()
        open class Keepalive(val tickInterval: UInt) : ObservationRequestType() {
            class Start(tickInterval: UInt) : Keepalive(tickInterval)

            init {
                require(tickInterval <= 20U * 60U * 60U) // highest accepted interval: 1 hour at standard tick-speed
            }
        }


        @Suppress("ClassName")
        object STREAM_CODEC : StreamCodec<FriendlyByteBuf, ObservationRequestType> {

            override fun decode(`object`: FriendlyByteBuf): ObservationRequestType {
                val typeByte = `object`.readByte()
                when (typeByte) {
                    0.toByte() -> return Stop
                    1.toByte() -> return Single
                    2.toByte() -> {
                        val tickInterval = `object`.readVarInt()
                        require(tickInterval > 0)
                        return Keepalive(tickInterval.toUInt())
                    }
                    3.toByte() -> {
                        val tickInterval = `object`.readVarInt()
                        require(tickInterval > 0)
                        return Keepalive.Start(tickInterval.toUInt())
                    }
                    else -> throw IllegalArgumentException("Unknown observation request type: $typeByte")
                }
            }

            override fun encode(`object`: FriendlyByteBuf, object2: ObservationRequestType) {
                when (object2) {
                    is Stop -> {
                        `object`.writeByte(0)
                    }
                    is Single -> {
                        `object`.writeByte(1)
                    }
                    is Keepalive -> {
                        if (object2 is Keepalive.Start) {
                            `object`.writeByte(3)
                        } else {
                            `object`.writeByte(2)
                        }
                        // should implicitly have been checked by init of ObservationRequestType (max tickInterval 1h)
                        assert(object2.tickInterval <= Int.MAX_VALUE.toUInt())
                        `object`.writeVarInt(object2.tickInterval.toInt())
                    }
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<C2SObservationsRequestPayload> = TYPE

    companion object {

        val TYPE = CustomPacketPayload.Type<C2SObservationsRequestPayload>(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID, "observations.request"
            )
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, C2SObservationsRequestPayload> = StreamCodec.composite(
            GlobalPos.STREAM_CODEC,
            C2SObservationsRequestPayload::blockPos,
            ObservationRequestType.STREAM_CODEC,
            C2SObservationsRequestPayload::requestType,
            ByteBufCodecs.VAR_LONG,
            {
                val tick = it.clientTick
                when(tick) {
                    null -> 0L
                    -1L -> {
                        OTelCoreMod.logger.warn("Serializing {} with clientTick of -1, will be mapped to null during transport",
                                                C2SObservationsRequestPayload::class.java.simpleName)
                        0L
                    }
                    else -> tick + 1L
                }
            }
        ) { pos, type, tick ->
            if (tick == 0L) {
                C2SObservationsRequestPayload(pos, type, null)
            } else {
                C2SObservationsRequestPayload(pos, type, tick - 1)
            }
        }

        fun register() {
            NetworkManager.registerReceiver(NetworkManager.c2s(), TYPE, STREAM_CODEC, Receiver)
        }
    }

    object Receiver : NetworkManager.NetworkReceiver<C2SObservationsRequestPayload> {

        override fun receive(value: C2SObservationsRequestPayload, context: NetworkManager.PacketContext) {
            val player = context.player as ServerPlayer
            val manager: ObservationSyncManagerServer = context.player.server!!.observationSyncManager
            when(value.requestType) {
                ObservationRequestType.Stop -> {
                    manager.handleRequestStop(player, value.blockPos)
                }
                ObservationRequestType.Single -> {
                    manager.handleRequestSingle(player, value.blockPos)
                }
                is ObservationRequestType.Keepalive.Start -> {
                    manager.handleRequestStart(player, value.blockPos, value.requestType.tickInterval)
                }
                is ObservationRequestType.Keepalive -> {
                    manager.handleRequestKeepalive(player, value.blockPos, value.requestType.tickInterval)
                }
            }
        }
    }
}
