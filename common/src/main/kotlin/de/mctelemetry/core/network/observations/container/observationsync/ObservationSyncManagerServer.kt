package de.mctelemetry.core.network.observations.container.observationsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.network.observations.container.ObservationContainerInteractionLimits
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.networking.NetworkManager
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import org.apache.logging.log4j.LogManager
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.math.min

class ObservationSyncManagerServer(
    tickInterval: UInt = DEFAULT_TICK_INTERVAL,
) {

    companion object {

        private val subLogger =
            LogManager.getLogger("${OTelCoreMod.MOD_ID}.${ObservationSyncManagerServer::class.simpleName}")

        const val DEFAULT_TICK_INTERVAL: UInt = 20U

        const val MAX_AGE_TICKS: Int = (20 * 60 * 3) / 2 // 1.5 minutes at 20tps

        private val instanceMap: ConcurrentMap<MinecraftServer, ObservationSyncManagerServer> = ConcurrentHashMap()

        fun getObservationSyncManagerOrNull(server: MinecraftServer): ObservationSyncManagerServer? {
            return instanceMap[server]
        }

        fun getObservationSyncManagerOrCreate(server: MinecraftServer): ObservationSyncManagerServer {
            return instanceMap.computeIfAbsent(server) { ObservationSyncManagerServer() }
        }

        val MinecraftServer.observationSyncManager: ObservationSyncManagerServer
            get() = getObservationSyncManagerOrCreate(this)

        fun registerListeners() {
            LifecycleEvent.SERVER_STARTING.register {
                getObservationSyncManagerOrCreate(it)
            }
            LifecycleEvent.SERVER_STOPPING.register {
                instanceMap.remove(it)
            }
            TickEvent.SERVER_POST.register {
                val manager = getObservationSyncManagerOrNull(it) ?: return@register
                manager.tick(it)
            }
        }
    }

    private class PlayerBlockRegistration(
        val player: ServerPlayer,
        val updateIntervalTicks: UInt,
        var nextUpdateTick: Long,
        var maxAgeTick: Long,
    ) {

        constructor(player: ServerPlayer, updateIntervalTicks: UInt, currentTick: Long) : this(
            player,
            updateIntervalTicks,
            currentTick,
            currentTick + MAX_AGE_TICKS
        )

        fun isTimeout(currentTick: Long): Boolean {
            return currentTick > maxAgeTick
        }

        fun updateTick(currentTick: Long): Boolean {
            if (currentTick > nextUpdateTick) {
                nextUpdateTick = currentTick + updateIntervalTicks.toLong()
                return true
            }
            return false
        }

        fun updateTimeout(currentTick: Long) {
            maxAgeTick = currentTick + MAX_AGE_TICKS
        }

        fun validForBlock(dimension: ResourceKey<Level>, pos: BlockPos): Boolean {
            return ObservationContainerInteractionLimits.checkCanInteract(
                player,
                dimension,
                pos,
            )
        }
    }

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    private val cooldownTicksRemaining: AtomicInteger = AtomicInteger(0)
    var tickInterval: UInt = tickInterval
        set(value) {
            field = value
            val intValue = if (value < Int.MAX_VALUE.toUInt()) value.toInt() else Int.MAX_VALUE
            cooldownTicksRemaining.updateAndGet { min(it, intValue) }
        }

    fun clearCooldownTicks() {
        cooldownTicksRemaining.set(0)
    }

    private val levelMaps: MutableMap<
            ResourceKey<Level>,
            ConcurrentMap<
                    BlockPos,
                    MutableMap<
                            ServerPlayer,
                            PlayerBlockRegistration>>> =
        Collections.synchronizedMap(mutableMapOf())

    fun checkCanRequest(
        player: ServerPlayer,
        pos: GlobalPos,
        checkDimension: Boolean = true,
        checkDistance: Boolean = true,
        checkLoaded: Boolean = true,
        checkBlockEntity: Boolean = true,
    ): Boolean {
        return ObservationContainerInteractionLimits.checkCanInteract(
            player,
            pos.dimension,
            pos.pos,
            log = true,
            checkDimension = checkDimension,
            checkDistance = checkDistance,
            checkLoaded = checkLoaded,
            checkBlockEntity = checkBlockEntity,
        )
    }

    private inline fun updateRegistration(
        player: ServerPlayer,
        pos: GlobalPos,
        crossinline transform: (PlayerBlockRegistration?) -> PlayerBlockRegistration?,
    ) {
        readWriteLock.readLock().withLock {
            val levelMap = levelMaps.computeIfAbsent(pos.dimension()) { ConcurrentHashMap() }
            levelMap.compute(pos.pos) { _, oldPlayerMap ->
                if (oldPlayerMap == null) {
                    val result = transform(null)
                    return@compute if (result != null) {
                        mutableMapOf(player to result)
                    } else {
                        null
                    }
                }
                val input = oldPlayerMap[player]
                val result = transform(input)
                if (input != result) {
                    if (result == null) {
                        oldPlayerMap.remove(player, input)
                        if (oldPlayerMap.isEmpty())
                            return@compute null
                    } else {
                        oldPlayerMap[player] = result
                    }
                }
                oldPlayerMap
            }
        }
    }

    fun handleRequestSingle(player: ServerPlayer, pos: GlobalPos, log: Boolean = true) {
        if (!checkCanRequest(player, pos)) {
            handleRequestStop(player, pos)
            return
        }
        if (log) {
            subLogger.trace("Received RequestSingle from {} for {}@{}", player, pos.dimension, pos.pos)
        }
        val currentTick = player.server.tickCount.toLong()
        updateRegistration(player, pos) {
            it ?: PlayerBlockRegistration(
                player,
                updateIntervalTicks = UInt.MAX_VALUE,
                nextUpdateTick = currentTick,
                maxAgeTick = currentTick - 1,
            )
        }
    }

    fun handleRequestStop(player: ServerPlayer, pos: GlobalPos, log: Boolean = true) {
        if (log) {
            subLogger.trace("Received Stop from {} for {}@{}", player, pos.dimension, pos.pos)
        }
        readWriteLock.readLock().withLock {
            val map = levelMaps[pos.dimension()] ?: return
            map.computeIfPresent(pos.pos) { _, old ->
                old.remove(player)
                old
            }
        }
    }

    fun handleRequestStart(player: ServerPlayer, pos: GlobalPos, tickInterval: UInt, log: Boolean = true) {
        // accept request even if block is not currently correct block entity -> might be changed in time for next tick
        if (!checkCanRequest(player, pos, checkBlockEntity = false)) {
            handleRequestStop(player, pos, log = false)
            return
        }
        val currentTick = player.server.tickCount.toLong()
        if (log) {
            subLogger.trace(
                "Received Start from {} for {}@{} with interval {}",
                player,
                pos.dimension,
                pos.pos,
                tickInterval
            )
        }
        updateRegistration(player, pos) {
            if (it == null)
                PlayerBlockRegistration(
                    player,
                    updateIntervalTicks = tickInterval,
                    currentTick = currentTick,
                )
            else {
                it.updateTimeout(currentTick)
                it
            }
        }
    }


    fun handleRequestKeepalive(player: ServerPlayer, pos: GlobalPos, tickInterval: UInt, log: Boolean = true) {
        if (!checkCanRequest(player, pos)) {
            handleRequestStop(player, pos, log = false)
            return
        }
        val currentTick = player.server.tickCount.toLong()
        if (log) {
            subLogger.trace(
                "Received Keepalive from {} for {}@{} with interval {}",
                player,
                pos.dimension,
                pos.pos,
                tickInterval
            )
        }
        updateRegistration(player, pos) {
            if (it == null)
                PlayerBlockRegistration(
                    player,
                    updateIntervalTicks = tickInterval,
                    currentTick = currentTick,
                )
            else {
                it.updateTimeout(currentTick)
                it
            }
        }
    }

    fun sendObservation(
        currentTick: Long,
        level: ServerLevel,
        pos: BlockPos,
        players: Collection<ServerPlayer>,
    ) {
        val observations: Map<IObservationSource<*, *>, RecordedObservations>
        if (!level.isLoaded(pos)) return
        val entity = level.getBlockEntity(pos)
        if (entity !is ObservationSourceContainerBlockEntity) return
        val container = entity.containerIfInitialized ?: return
        val memoryRecorder = MemoryObservationRecorder()
        container.observe(memoryRecorder, forceObservation = true)
        observations = memoryRecorder.recordedAsMap()
        subLogger.trace(
            "Sending {} observation-points for {}@{} to players {}",
            observations.values.sumOf { it.observations.size },
            level.dimension().location(),
            pos,
            players
        )
        NetworkManager.sendToPlayers(
            players,
            S2CObservationsPayload(
                blockPos = GlobalPos(level.dimension(), pos),
                observations = observations,
                serverTick = currentTick
            )
        )
    }

    fun tick(server: MinecraftServer) {
        if (cooldownTicksRemaining.getAndUpdate { (it + 1) % tickInterval.toInt() } != 0) return
        readWriteLock.writeLock().withLock {
            val currentTick = server.tickCount.toLong()
            var removePendingRegistrations: ArrayDeque<PlayerBlockRegistration>? = null
            var removePendingBlocks: ArrayDeque<BlockPos>? = null
            var updatePendingRegistrations: ArrayDeque<ServerPlayer>? = null
            for ((level, levelMap) in levelMaps.entries) {
                val serverLevel: ServerLevel? = server.getLevel(level)
                for ((block, registrationMap) in levelMap.entries) {
                    if (registrationMap.isNotEmpty()) {
                        if (!ObservationContainerInteractionLimits.checkIsInteractable(serverLevel, block)) {
                            removePendingRegistrations = removePendingRegistrations ?: ArrayDeque()
                            removePendingRegistrations.addAll(registrationMap.values)
                            subLogger.trace(
                                "Removing {} registrations for {}@{} because base block is no longer valid",
                                registrationMap.values.size,
                                level.location(),
                                block
                            )
                        } else {
                            for (registration in registrationMap.values) {
                                if (!registration.validForBlock(level, block)) {
                                    removePendingRegistrations = removePendingRegistrations ?: ArrayDeque()
                                    removePendingRegistrations.add(registration)
                                    subLogger.trace(
                                        "Removing {} because no longer valid for {}@{}",
                                        registration,
                                        level.location(),
                                        block
                                    )
                                } else {
                                    if (registration.isTimeout(currentTick)) {
                                        removePendingRegistrations = removePendingRegistrations ?: ArrayDeque()
                                        removePendingRegistrations.add(registration)
                                        subLogger.trace("Removing {} because of timeout", registration)
                                    }
                                    if (registration.updateTick(currentTick)) {
                                        updatePendingRegistrations = updatePendingRegistrations ?: ArrayDeque()
                                        updatePendingRegistrations.add(registration.player)
                                        subLogger.trace("Scheduling {} for payload", registration)
                                    }
                                }
                            }
                        }
                        if (!updatePendingRegistrations.isNullOrEmpty()) {
                            sendObservation(currentTick, serverLevel!!, block, updatePendingRegistrations)
                            updatePendingRegistrations.clear()
                        }
                        if (!removePendingRegistrations.isNullOrEmpty()) {
                            for (registration in removePendingRegistrations) {
                                registrationMap.remove(registration.player, registration)
                            }
                            removePendingRegistrations.clear()
                        }
                    }
                    if (registrationMap.isEmpty()) { // not else-if because might be result of deletion above
                        removePendingBlocks = removePendingBlocks ?: ArrayDeque()
                        removePendingBlocks.add(block)
                    }
                }
                if (!removePendingBlocks.isNullOrEmpty()) {
                    for (block in removePendingBlocks) {
                        levelMap.remove(block)
                    }
                    removePendingBlocks.clear()
                }
            }
        }
    }
}
