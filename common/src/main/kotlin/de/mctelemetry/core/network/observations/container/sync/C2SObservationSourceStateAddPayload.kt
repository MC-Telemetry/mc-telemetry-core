package de.mctelemetry.core.network.observations.container.sync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import de.mctelemetry.core.api.observations.sourceContextType
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.network.observations.container.ObservationContainerInteractionLimits
import de.mctelemetry.core.observations.model.ObservationSourceContainer
import de.mctelemetry.core.utils.runWithExceptionCleanup
import dev.architectury.networking.NetworkManager
import net.minecraft.core.GlobalPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class C2SObservationSourceStateAddPayload(
    val blockPos: GlobalPos,
    val sourceInstance: IObservationSourceInstance<*, *, *>,
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<C2SObservationSourceStateAddPayload>(
            ResourceLocation.fromNamespaceAndPath(
                OTelCoreMod.MOD_ID, "observations.container.states.add"
            )
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, C2SObservationSourceStateAddPayload> =
            StreamCodec.composite(
                GlobalPos.STREAM_CODEC,
                C2SObservationSourceStateAddPayload::blockPos,
                IObservationSourceInstance.STREAM_CODEC,
                C2SObservationSourceStateAddPayload::sourceInstance,
                ::C2SObservationSourceStateAddPayload
            )

        fun register() {
            NetworkManager.registerReceiver(NetworkManager.c2s(), TYPE, STREAM_CODEC, Receiver)
        }
    }

    object Receiver : NetworkManager.NetworkReceiver<C2SObservationSourceStateAddPayload> {
        override fun receive(value: C2SObservationSourceStateAddPayload, context: NetworkManager.PacketContext) {
            val container: ObservationSourceContainer<ObservationSourceContainerBlockEntity>
            val sourceInstance: IObservationSourceInstance<in ObservationSourceContainerBlockEntity, *, *>
            runWithExceptionCleanup(value.sourceInstance::close) {
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
                container = blockEntity.container
                val testSourceInstance = value.sourceInstance
                if (testSourceInstance.source !in container.observationSources) {
                    // TODO: Log debug message stating that received source is not whitelisted
                    return
                }
                assert(testSourceInstance.sourceContextType.isAssignableFrom(ObservationSourceContainerBlockEntity::class.java))
                @Suppress("UNCHECKED_CAST")
                // cast is checked by being element in `container.observationSources`, all of which are of the given type
                sourceInstance =
                    testSourceInstance as IObservationSourceInstance<in ObservationSourceContainerBlockEntity, *, *>
                sourceInstance.onLoad(blockEntity)
            }
            container.addObservationSourceState(instance = sourceInstance)
        }
    }
}
