package de.mctelemetry.core.api.instruments.manager.client

import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.builder.IRemoteWorldInstrumentDefinitionBuilder
import de.mctelemetry.core.instruments.manager.client.ClientInstrumentMetaManager
import dev.architectury.event.Event
import dev.architectury.event.EventFactory

interface IClientWorldInstrumentManager : IClientInstrumentManager {

    fun gaugeInstrument(name: String): IRemoteWorldInstrumentDefinitionBuilder<*>

    fun requestInstrumentRemoval(name: String)

    fun requestFullUpdate()

    suspend fun awaitFullUpdate()


    override fun findGlobal(name: String): IClientInstrumentManager.IClientInstrumentDefinition? {
        return findGlobal(Regex.fromLiteral(name)).firstOrNull()
    }

    override fun findGlobal(pattern: Regex?): Sequence<IClientInstrumentManager.IClientInstrumentDefinition>

    override fun findLocal(name: String): IClientWorldInstrumentDefinition? {
        return findLocal(Regex.fromLiteral(name)).firstOrNull()
    }

    override fun findLocal(pattern: Regex?): Sequence<IClientWorldInstrumentDefinition>

    interface IClientWorldInstrumentDefinition :
            IClientInstrumentManager.IClientInstrumentDefinition,
            IWorldInstrumentDefinition


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

        val clientWorldInstrumentManager: IClientWorldInstrumentManager?
            get() = ClientInstrumentMetaManager.activeWorldManager
    }
}
