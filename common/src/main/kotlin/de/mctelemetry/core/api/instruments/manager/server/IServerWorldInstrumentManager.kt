package de.mctelemetry.core.api.instruments.manager.server

import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.gauge.IWorldInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IGameInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IWorldInstrumentManager
import de.mctelemetry.core.instruments.manager.server.ServerInstrumentMetaManager
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import net.minecraft.server.MinecraftServer
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface IServerWorldInstrumentManager : IMutableInstrumentManager, IWorldInstrumentManager {

    val gameInstruments: IGameInstrumentManager

    override fun gaugeInstrument(name: String): IWorldGaugeInstrumentBuilder<*>


    override fun findLocal(pattern: Regex?): Sequence<IWorldInstrumentRegistration>

    override fun findLocal(name: String): IWorldInstrumentRegistration? {
        return findLocal(Regex("^"+Regex.escape(name)+"$")).firstOrNull()
    }

    override fun findLocalMutable(name: String): IWorldMutableInstrumentRegistration<*>? {
        return super.findLocalMutable(name) as? IWorldMutableInstrumentRegistration<*>
    }

    override fun findLocalMutable(pattern: Regex?): Sequence<IWorldMutableInstrumentRegistration<*>> {
        return findLocal(pattern).filterIsInstance<IWorldMutableInstrumentRegistration<*>>()
    }

    fun addLocalWorldMutableCallback(callback: IInstrumentAvailabilityCallback<IWorldMutableInstrumentRegistration<*>>): AutoCloseable {
        return addLocalCallback(object : IInstrumentAvailabilityCallback<IInstrumentDefinition> {
            override fun instrumentAdded(
                manager: IInstrumentManager,
                instrument: IInstrumentDefinition,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IWorldMutableInstrumentRegistration<*>)
                    callback.instrumentAdded(manager, instrument, phase)
            }

            override fun instrumentRemoved(
                manager: IInstrumentManager,
                instrument: IInstrumentDefinition,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IWorldMutableInstrumentRegistration<*>)
                    callback.instrumentRemoved(manager, instrument, phase)
            }
        })
    }

    object Events {

        val LOADING: Event<Loading> = EventFactory.createLoop()
        val LOADED: Event<Loaded> = EventFactory.createLoop()
        val UNLOADING: Event<Unloading> = EventFactory.createLoop()
        val UNLOADED: Event<Unloaded> = EventFactory.createLoop()

        fun interface Loading {

            fun worldInstrumentManagerLoading(manager: IServerWorldInstrumentManager, server: MinecraftServer)
        }

        fun interface Loaded {

            fun worldInstrumentManagerLoaded(manager: IServerWorldInstrumentManager, server: MinecraftServer)
        }

        fun interface Unloading {

            fun worldInstrumentManagerUnloading(manager: IServerWorldInstrumentManager, server: MinecraftServer)
        }

        fun interface Unloaded {

            fun worldInstrumentManagerUnloaded(manager: IServerWorldInstrumentManager, server: MinecraftServer)
        }
    }

    companion object {

        val MinecraftServer.instrumentManager: IServerWorldInstrumentManager?
            get() = ServerInstrumentMetaManager.worldInstrumentManagerForServer(this)

        fun MinecraftServer.useInstrumentManagerWhenAvailable(callback: (IServerWorldInstrumentManager) -> Unit): AutoCloseable {
            return ServerInstrumentMetaManager.whenWorldInstrumentManagerAvailable(this, callback)
        }
    }
}

inline fun IServerWorldInstrumentManager.gaugeWorldInstrument(
    name: String,
    block: IWorldGaugeInstrumentBuilder<*>.() -> Unit,
): IWorldGaugeInstrumentBuilder<*> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return gaugeInstrument(name).apply(block)
}
