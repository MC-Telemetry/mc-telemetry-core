package de.mctelemetry.core.api.metrics.managar

import de.mctelemetry.core.api.metrics.builder.IWorldGaugeInstrumentBuilder
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import net.minecraft.server.MinecraftServer

interface IWorldInstrumentManager : IInstrumentManager {

    val gameInstruments: IGameInstrumentManager

    override fun gaugeInstrument(name: String): IWorldGaugeInstrumentBuilder<*>

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
}

inline fun IWorldInstrumentManager.gaugeWorldInstrument(
    name: String,
    block: IWorldGaugeInstrumentBuilder<*>.() -> Unit,
): IWorldGaugeInstrumentBuilder<*> = gaugeInstrument(name).apply(block)
