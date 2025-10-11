package de.mctelemetry.core.api.metrics.managar

import dev.architectury.event.Event
import dev.architectury.event.EventFactory

interface IGameMetricsManager {
    object Events {
        val READY: Event<Ready> = EventFactory.createLoop()
        fun interface Ready {
            fun gameMetricsManagerReady(manager: IGameMetricsManager)
        }
    }
}
