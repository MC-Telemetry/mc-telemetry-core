package de.mctelemetry.core.network.instrumentsync

import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.instruments.definition.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager.Companion.useInstrumentManagerWhenAvailable
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.networking.NetworkManager
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import kotlin.contracts.contract

class SyncSubscriptions(private val server: MinecraftServer) : IInstrumentAvailabilityCallback<IMetricDefinition> {

    companion object {

        fun register() {
            PlayerEvent.PLAYER_JOIN.register(::onPlayerJoin)
            LifecycleEvent.SERVER_STARTED.register { server ->
                val instance = SyncSubscriptions(server)
                server.useInstrumentManagerWhenAvailable { manager ->
                    manager.addGlobalCallback(instance)
                }
            }
        }


        fun onPlayerJoin(player: ServerPlayer) {
            player.server.useInstrumentManagerWhenAvailable {
                NetworkManager.sendToPlayer(player, S2CAllInstrumentsPayload.fromManager(it))
            }
        }
    }

    private fun isSynchronizedInstrument(manager: IInstrumentManager, instrument: IMetricDefinition): Boolean {
        contract {
            returns(true) implies (instrument is IWorldInstrumentDefinition && manager is IServerWorldInstrumentManager)
        }
        if (instrument !is IWorldInstrumentDefinition) return false
        if (manager !is IServerWorldInstrumentManager) return false
        return true
    }

    override fun instrumentAdded(
        manager: IInstrumentManager,
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (phase != IInstrumentAvailabilityCallback.Phase.POST) return
        if (isSynchronizedInstrument(manager, instrument)) {
            NetworkManager.sendToPlayers(server.playerList.players, A2AInstrumentAddedPayload.S2C(instrument))
        } else {
            NetworkManager.sendToPlayers(
                server.playerList.players,
                S2CReservedNameAddedPayload(instrument.name.lowercase())
            )
        }
    }

    override fun instrumentRemoved(
        manager: IInstrumentManager,
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (phase != IInstrumentAvailabilityCallback.Phase.POST) return
        if (isSynchronizedInstrument(manager, instrument)) {
            NetworkManager.sendToPlayers(server.playerList.players, A2AInstrumentRemovedPayload.S2C(instrument))
        } else {
            NetworkManager.sendToPlayers(
                server.playerList.players,
                S2CReservedNameRemovedPayload(instrument.name.lowercase())
            )
        }
    }
}
