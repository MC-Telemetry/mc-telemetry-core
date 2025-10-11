package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.api.metrics.managar.IGameMetricsManager
import de.mctelemetry.core.api.metrics.managar.IWorldMetricsManager
import de.mctelemetry.core.utils.runWithExceptionCleanup
import dev.architectury.event.events.common.LifecycleEvent
import net.minecraft.server.MinecraftServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object MetricMetaManager {

    fun register() {
        LifecycleEvent.SETUP.register(::onSetup)
        LifecycleEvent.SERVER_STARTING.register(::onServerStarting)
        LifecycleEvent.SERVER_STARTED.register(::onServerStarted)
        LifecycleEvent.SERVER_STOPPING.register(::onServerStopping)
        LifecycleEvent.SERVER_STOPPED.register(::onServerStopped)
    }

    private lateinit var gameMetricsManager: GameMetricsManager
    private val serverMetricsManagers: ConcurrentMap<MinecraftServer, WorldMetricsManager> = ConcurrentHashMap(1)

    private fun onSetup() {
        if (::gameMetricsManager.isInitialized) {
            return
        }
        gameMetricsManager = GameMetricsManager()
        IGameMetricsManager.Events.READY.invoker().gameMetricsManagerReady(gameMetricsManager)
    }

    private fun onServerStarting(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.computeIfAbsent(server) {
            WorldMetricsManager(
                gameMetricsManager,
                it
            )
        }
        runWithExceptionCleanup(cleanup = { serverMetricsManagers.remove(server, serverManager) })
        {
            serverManager.start()
            runWithExceptionCleanup(cleanup = { serverManager.stop() })
            {
                IWorldMetricsManager.Events.LOADING.invoker().worldMetricsManagerLoading(serverManager, server)
            }
        }
    }

    private fun onServerStarted(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.getValue(server)
        IWorldMetricsManager.Events.LOADED.invoker().worldMetricsManagerLoaded(serverManager, server)
    }

    private fun onServerStopping(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.getOrElse(server) { return }
        IWorldMetricsManager.Events.UNLOADING.invoker().worldMetricsManagerUnloading(serverManager, server)
    }

    private fun onServerStopped(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.getOrElse(server) { return }
        IWorldMetricsManager.Events.UNLOADED.invoker().worldMetricsManagerUnloaded(serverManager, server)
    }
}
