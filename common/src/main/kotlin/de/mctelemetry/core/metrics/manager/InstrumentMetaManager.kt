package de.mctelemetry.core.metrics.manager

import com.mojang.datafixers.util.Either
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.managar.IGameInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.platform.Platform
import net.minecraft.server.MinecraftServer
import java.util.Deque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentMap
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

object InstrumentMetaManager {

    fun register() {
        LifecycleEvent.SETUP.register(::onSetup)
        LifecycleEvent.SERVER_STARTING.register(::onServerStarting)
        LifecycleEvent.SERVER_STARTED.register(::onServerStarted)
        LifecycleEvent.SERVER_STOPPING.register(::onServerStopping)
        LifecycleEvent.SERVER_STOPPED.register(::onServerStopped)
    }

    private lateinit var gameInstrumentManager: GameInstrumentManager
    private val serverMetricsManagers: ConcurrentMap<MinecraftServer, Either<WorldInstrumentManager, Deque<(WorldInstrumentManager) -> Unit>>> =
        ConcurrentHashMap(1)

    private fun onSetup() {
        if (::gameInstrumentManager.isInitialized) {
            return
        }
        gameInstrumentManager = GameInstrumentManager(OTelCoreMod.meter)
        IGameInstrumentManager.Events.READY.invoker().gameMetricsManagerReady(gameInstrumentManager)
    }

    private fun onServerStarting(server: MinecraftServer) {
        val serverManagerEntry = serverMetricsManagers.compute(server) { server, previous ->
            val callbacks: Deque<(WorldInstrumentManager) -> Unit>?
            if (previous != null) {
                if (previous.left().isPresent) return@compute previous
                callbacks = previous.right().get()
            } else {
                callbacks = null
            }
            Either.left(WorldInstrumentManager.withPersistentStorage(
                gameInstrumentManager.meter,
                gameInstrumentManager,
                server,
            ).also { manager ->
                if(callbacks != null) {
                    do {
                        val element = callbacks.pollFirst() ?: break
                        try {
                            element(manager)
                        } catch (ex: RuntimeException) {
                            OTelCoreMod.logger.error("Exception during WorldInstrumentManager-Callback", ex)
                        }
                    }
                    while (true)
                }
            })
        }!!
        val serverManager = serverManagerEntry.left().get()
        runWithExceptionCleanup(cleanup = { serverMetricsManagers.remove(server, serverManagerEntry) })
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
            serverMetricsManagers.getValue(server).left().get()
        } else {
            val storedManagerOrCallback = serverMetricsManagers[server]
            if (storedManagerOrCallback == null || storedManagerOrCallback.left().isEmpty) { // Fabric does not trigger [LifecycleEvent.SERVER_STARTING]?
                onServerStarting(server)
                serverMetricsManagers.getValue(server).left().get()
            } else {
                storedManagerOrCallback.left().get()
            }
        }
        IWorldInstrumentManager.Events.LOADED.invoker().worldInstrumentManagerLoaded(serverManager, server)
    }

    private fun onServerStopping(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.getOrElse(server) { return }.left().getOrElse { return }
        IWorldInstrumentManager.Events.UNLOADING.invoker().worldInstrumentManagerUnloading(serverManager, server)
    }

    private fun onServerStopped(server: MinecraftServer) {
        val serverManager = serverMetricsManagers.getOrElse(server) { return }.left().getOrElse { return }

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
        return serverMetricsManagers[server]?.left()?.getOrNull()
    }

    internal fun whenWorldInstrumentManagerAvailable(
        server: MinecraftServer,
        callback: (WorldInstrumentManager) -> Unit,
    ): AutoCloseable {
        var closer: AutoCloseable? = null
        val newValue = serverMetricsManagers.compute(server) { _, previous ->
            if (previous == null) {
                val queue = ConcurrentLinkedDeque<(WorldInstrumentManager) -> Unit>()
                queue.add(callback)
                closer = AutoCloseable { queue.removeFirstOccurrence(callback) }
                Either.right(queue)
            } else if (previous.left().isEmpty) {
                val queue = previous.right().get()
                queue.add(callback)
                closer = AutoCloseable { queue.removeFirstOccurrence(callback) }
                previous
            } else {
                previous
            }
        }!!
        newValue.left().getOrNull()?.run(callback)
        return closer ?: AutoCloseable {}
    }
}
