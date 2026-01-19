package de.mctelemetry.core.network.observations.container.sync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.network.observations.container.ObservationContainerInteractionLimits
import de.mctelemetry.core.observations.model.ObservationSourceStateID
import dev.architectury.networking.NetworkManager
import net.minecraft.core.GlobalPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class C2SObservationSourceStateRemovePayload(
    val blockPos: GlobalPos,
    val stateId: ObservationSourceStateID,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<C2SObservationSourceStateRemovePayload>(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID, "observations.container.states.remove"
            )
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, C2SObservationSourceStateRemovePayload> = StreamCodec.composite(
            GlobalPos.STREAM_CODEC,
            C2SObservationSourceStateRemovePayload::blockPos,
            ByteBufCodecs.BYTE.map(Byte::toUByte, UByte::toByte),
            C2SObservationSourceStateRemovePayload::stateId,
            ::C2SObservationSourceStateRemovePayload
        )

        fun register() {
            NetworkManager.registerReceiver(NetworkManager.c2s(), TYPE, STREAM_CODEC, Receiver)
        }
    }

    object Receiver : NetworkManager.NetworkReceiver<C2SObservationSourceStateRemovePayload> {
        override fun receive(value: C2SObservationSourceStateRemovePayload, context: NetworkManager.PacketContext) {
            if (!ObservationContainerInteractionLimits.checkCanInteract(
                    context.player,
                    value.blockPos.dimension,
                    value.blockPos.pos,
                    log = true,
                    checkBlockEntity = true,
                )
            ) return
            val blockEntity = context.player.level() // cast should succeed because checkCanInteract tests for type
                .getBlockEntity(value.blockPos.pos) as ObservationSourceContainerBlockEntity
            val container = blockEntity.container
            container.removeObservationSourceState(value.stateId)
        }
    }
}
