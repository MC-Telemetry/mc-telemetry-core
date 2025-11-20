package de.mctelemetry.core.network.observations.container.settings

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.api.metrics.OTelCoreModAPI
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.network.observations.container.ObservationContainerInteractionLimits
import de.mctelemetry.core.observations.model.ObservationSourceConfiguration
import de.mctelemetry.core.observations.model.ObservationSourceState
import de.mctelemetry.core.utils.toShortString
import dev.architectury.networking.NetworkManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.GlobalPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class C2SObservationSourceSettingsUpdatePayload(
    val pos: GlobalPos,
    val source: IObservationSource<*, *>,
    val configuration: ObservationSourceConfiguration?,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?>? {
        return TYPE
    }

    companion object {

        val TYPE = CustomPacketPayload.Type<C2SObservationSourceSettingsUpdatePayload>(
            ResourceLocation.fromNamespaceAndPath(OTelCoreMod.MOD_ID, "observation_source.settings")
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, C2SObservationSourceSettingsUpdatePayload> =
            StreamCodec.composite(
                GlobalPos.STREAM_CODEC,
                C2SObservationSourceSettingsUpdatePayload::pos,
                ByteBufCodecs.registry(OTelCoreModAPI.ObservationSources),
                C2SObservationSourceSettingsUpdatePayload::source,
                ByteBufCodecs.optional(ObservationSourceConfiguration.STREAM_CODEC).map(
                    Optional<ObservationSourceConfiguration>::getOrNull,
                    Optional<ObservationSourceConfiguration>::ofNullable
                ),
                C2SObservationSourceSettingsUpdatePayload::configuration,
                ::C2SObservationSourceSettingsUpdatePayload,
            )

        fun register() {
            NetworkManager.registerReceiver(NetworkManager.c2s(), TYPE, STREAM_CODEC, Receiver)
        }

        @Environment(EnvType.CLIENT)
        fun ObservationSourceState.sendConfigurationUpdate(pos: GlobalPos, value: ObservationSourceConfiguration?) {
            NetworkManager.sendToServer(C2SObservationSourceSettingsUpdatePayload(
                pos,
                source,
                value,
            ))
        }
    }

    object Receiver : NetworkManager.NetworkReceiver<C2SObservationSourceSettingsUpdatePayload> {

        override fun receive(
            value: C2SObservationSourceSettingsUpdatePayload,
            context: NetworkManager.PacketContext,
        ) {
            val level = context.player.level().let {
                if (it.dimension() == value.pos.dimension) it
                else if (!it.isClientSide) {
                    (it as ServerLevel).server.getLevel(value.pos.dimension)
                        ?: throw NoSuchElementException("Could not find level ${value.pos.dimension}")
                } else {
                    throw IllegalArgumentException("Received clientSide-level in C2S-Receiver")
                }
            }
            if (!ObservationContainerInteractionLimits.checkCanInteract(
                    context.player,
                    value.pos.dimension,
                    value.pos.pos,
                    log = true,
                    checkBlockEntity = false
                )
            ) {
                return
            }
            val blockEntity = level.getBlockEntity(value.pos.pos)
            if (blockEntity !is ObservationSourceContainerBlockEntity) {
                return
            }
            @Suppress("UNCHECKED_CAST")
            // cast is only over generics. If cast would fail, the entry is simply
            // not found in observationStates.
            val state = (blockEntity.observationStates as Map<IObservationSource<*, *>, ObservationSourceState>)
                .getOrElse(value.source) {
                    throw NoSuchElementException("Could not find observation source ${value.source} in ${value.pos.toShortString()}")
                }
            state.configuration = value.configuration
        }
    }
}
