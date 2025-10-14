package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.managar.IGameInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.platform.Platform
import net.minecraft.server.MinecraftServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object InstrumentMetaManager {

    fun register() {
        LifecycleEvent.SETUP.register(::onSetup)
        LifecycleEvent.SERVER_STARTING.register(::onServerStarting)
        LifecycleEvent.SERVER_STARTED.register(::onServerStarted)
        LifecycleEvent.SERVER_STOPPING.register(::onServerStopping)
        LifecycleEvent.SERVER_STOPPED.register(::onServerStopped)
    }

    private lateinit var gameInstrumentManager: GameInstrumentManager
    private val serverMetricsManagers: ConcurrentMap<MinecraftServer, WorldInstrumentManager> = ConcurrentHashMap(1)

    private fun onSetup() {
        if (::gameInstrumentManager.isInitialized) {
            return
        }
        gameInstrumentManager = GameInstrumentManager(OTelCoreMod.meter)
        IGameInstrumentManager.Events.READY.invoker().gameMetricsManagerReady(gameInstrumentManager)
    }

    private fun onServerStarting(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.computeIfAbsent(server) {
            WorldInstrumentManager.withPersistentStorage(
                gameInstrumentManager.meter,
                gameInstrumentManager,
                it
            )
        }
        runWithExceptionCleanup(cleanup = { serverMetricsManagers.remove(server, serverManager) })
        {
            serverManager.start()
            runWithExceptionCleanup(cleanup = { serverManager.stop() })
            {
                IWorldInstrumentManager.Events.LOADING.invoker().worldInstrumentManagerLoading(serverManager, server)
            }
        }
    }

    private fun onServerStarted(server: MinecraftServer) {
        val serverManager = if (Platform.isForgeLike()) {
            serverMetricsManagers.getValue(server)
        } else {
            serverMetricsManagers.getOrElse(server) { // Fabric does not trigger [LifecycleEvent.SERVER_STARTING]?
                onServerStarting(server)
                serverMetricsManagers.getValue(server)
            }
        }
        IWorldInstrumentManager.Events.LOADED.invoker().worldInstrumentManagerLoaded(serverManager, server)
    }

    private fun onServerStopping(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.getOrElse(server) { return }
        IWorldInstrumentManager.Events.UNLOADING.invoker().worldInstrumentManagerUnloading(serverManager, server)
    }

    private fun onServerStopped(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.getOrElse(server) { return }

        var exceptionAccumulator: Exception? = null
        try {
            serverManager.stop()
        } catch (ex: Exception) {
            exceptionAccumulator = ex
        }
        try {
            IWorldInstrumentManager.Events.UNLOADED.invoker().worldInstrumentManagerUnloaded(serverManager, server)
        } catch (ex: Exception) {
            exceptionAccumulator += ex
        }
        if (exceptionAccumulator != null) {
            throw exceptionAccumulator
        }
    }

    internal fun worldInstrumentManagerForServer(server: MinecraftServer): WorldInstrumentManager? {
        return serverMetricsManagers[server]
    }
}
