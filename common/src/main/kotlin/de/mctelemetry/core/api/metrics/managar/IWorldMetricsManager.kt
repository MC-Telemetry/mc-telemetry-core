package de.mctelemetry.core.api.metrics.managar

import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import net.minecraft.server.MinecraftServer

interface IWorldMetricsManager {

    val gameMetrics: IGameMetricsManager

    object Events {

        val LOADING: Event<Loading> = EventFactory.createLoop()
        val LOADED: Event<Loaded> = EventFactory.createLoop()
        val UNLOADING: Event<Unloading> = EventFactory.createLoop()
        val UNLOADED: Event<Unloaded> = EventFactory.createLoop()

        fun interface Loading {

            fun worldMetricsManagerLoading(manager: IWorldMetricsManager, server: MinecraftServer)
        }

        fun interface Loaded {

            fun worldMetricsManagerLoaded(manager: IWorldMetricsManager, server: MinecraftServer)
        }

        fun interface Unloading {

            fun worldMetricsManagerUnloading(manager: IWorldMetricsManager, server: MinecraftServer)
        }

        fun interface Unloaded {

            fun worldMetricsManagerUnloaded(manager: IWorldMetricsManager, server: MinecraftServer)
        }
    }
}
