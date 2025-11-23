package de.mctelemetry.core.api.instruments.manager

import dev.architectury.event.Event
import dev.architectury.event.EventFactory

interface IGameInstrumentManager : IMutableInstrumentManager {

    object Events {

        val READY: Event<Ready> = EventFactory.createLoop()

        fun interface Ready {

            fun gameMetricsManagerReady(manager: IGameInstrumentManager)
        }
    }
}
