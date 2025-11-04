@file:OptIn(ExperimentalContracts::class)

package de.mctelemetry.core.api.metrics.managar

import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.metrics.manager.InstrumentMetaManager
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import net.minecraft.server.MinecraftServer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface IWorldInstrumentManager : IInstrumentManager {

    val gameInstruments: IGameInstrumentManager

    override fun gaugeInstrument(name: String): IWorldGaugeInstrumentBuilder<*>

    override fun findLocalMutable(name: String): IWorldMutableInstrumentRegistration<*>? {
        return super.findLocalMutable(name) as IWorldMutableInstrumentRegistration<*>?
    }

    override fun findLocalMutable(pattern: Regex): Sequence<IWorldMutableInstrumentRegistration<*>> {
        return findLocal(pattern).filterIsInstance<IWorldMutableInstrumentRegistration<*>>()
    }

    fun addLocalWorldMutableCallback(callback: IInstrumentAvailabilityCallback<IWorldMutableInstrumentRegistration<*>>): AutoCloseable {
        return addLocalCallback(object : IInstrumentAvailabilityCallback<IInstrumentRegistration> {
            override fun instrumentAdded(
                manager: IInstrumentManager,
                instrument: IInstrumentRegistration,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IWorldMutableInstrumentRegistration<*>)
                    callback.instrumentAdded(manager, instrument, phase)
            }

            override fun instrumentRemoved(
                manager: IInstrumentManager,
                instrument: IInstrumentRegistration,
                phase: IInstrumentAvailabilityCallback.Phase
            ) {
                if (instrument is IWorldMutableInstrumentRegistration<*>)
                    callback.instrumentRemoved(manager, instrument, phase)
            }
        })
    }

    interface IWorldMutableInstrumentRegistration<out R : IWorldMutableInstrumentRegistration<R>> :
            IInstrumentRegistration.Mutable<R> {

        val persistent: Boolean
    }

    object Events {

        val LOADING: Event<Loading> = EventFactory.createLoop()
        val LOADED: Event<Loaded> = EventFactory.createLoop()
        val UNLOADING: Event<Unloading> = EventFactory.createLoop()
        val UNLOADED: Event<Unloaded> = EventFactory.createLoop()

        fun interface Loading {

            fun worldInstrumentManagerLoading(manager: IWorldInstrumentManager, server: MinecraftServer)
        }

        fun interface Loaded {

            fun worldInstrumentManagerLoaded(manager: IWorldInstrumentManager, server: MinecraftServer)
        }

        fun interface Unloading {

            fun worldInstrumentManagerUnloading(manager: IWorldInstrumentManager, server: MinecraftServer)
        }

        fun interface Unloaded {

            fun worldInstrumentManagerUnloaded(manager: IWorldInstrumentManager, server: MinecraftServer)
        }
    }

    companion object {

        val MinecraftServer.instrumentManager: IWorldInstrumentManager?
            get() = InstrumentMetaManager.worldInstrumentManagerForServer(this)

        fun MinecraftServer.useInstrumentManagerWhenAvailable(callback: (IWorldInstrumentManager) -> Unit): AutoCloseable {
            return InstrumentMetaManager.whenWorldInstrumentManagerAvailable(this, callback)
        }
    }
}

inline fun IWorldInstrumentManager.gaugeWorldInstrument(
    name: String,
    block: IWorldGaugeInstrumentBuilder<*>.() -> Unit,
): IWorldGaugeInstrumentBuilder<*> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return gaugeInstrument(name).apply(block)
}
