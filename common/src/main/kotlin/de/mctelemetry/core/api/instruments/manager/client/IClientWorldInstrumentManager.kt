package de.mctelemetry.core.api.instruments.manager.client

import de.mctelemetry.core.api.instruments.builder.IRemoteWorldInstrumentDefinitionBuilder
import dev.architectury.event.Event
import dev.architectury.event.EventFactory

interface IClientWorldInstrumentManager : IClientInstrumentManager {

    fun gaugeInstrument(name: String): IRemoteWorldInstrumentDefinitionBuilder<*>

    object Events {

        val CREATED: Event<Created> = EventFactory.createLoop()
        val POPULATED: Event<Populated> = EventFactory.createLoop()
        val REMOVING: Event<Removing> = EventFactory.createLoop()
        val REMOVED: Event<Removed> = EventFactory.createLoop()

        fun interface Created {

            fun clientWorldInstrumentManagerCreated(manager: IClientWorldInstrumentManager)
        }

        fun interface Populated {

            fun clientWorldInstrumentManagerPopulated(manager: IClientWorldInstrumentManager)
        }

        fun interface Removing {

            fun clientWorldInstrumentManagerRemoving(manager: IClientWorldInstrumentManager)
        }

        fun interface Removed {

            fun clientWorldInstrumentManagerRemoved(manager: IClientWorldInstrumentManager)
        }
    }

    companion object {

        var clientWorldInstrumentManager: IClientWorldInstrumentManager? = null
            internal set
    }
}
