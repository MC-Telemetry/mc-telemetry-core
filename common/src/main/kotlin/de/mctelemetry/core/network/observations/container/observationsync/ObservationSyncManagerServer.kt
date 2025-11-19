package de.mctelemetry.core.network.observations.container.observationsync

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.metrics.IObservationSource
import de.mctelemetry.core.blocks.entities.ObservationSourceContainerBlockEntity
import de.mctelemetry.core.network.observations.container.observationsync.ObservationSyncManagerServer.PlayerBlockRegistration.Companion.RANGE
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
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.min

class ObservationSyncManagerServer(
    tickInterval: UInt = DEFAULT_TICK_INTERVAL,
) {

    companion object {

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
            currentTick + updateIntervalTicks.toLong(),
            currentTick + MAX_AGE_TICKS
        )

        companion object {


            const val RANGE: Double = 40.0

            @OptIn(ExperimentalContracts::class)
            fun blockIsValid(level: ServerLevel?, pos: BlockPos, checkBlockEntity: Boolean = true): Boolean {
                contract {
                    returns(true) implies (level != null)
                }
                level ?: return false
                if (!level.isLoaded(pos)) return false
                if (checkBlockEntity) {
                    val blockEntity = level.getBlockEntity(pos)
                    if (blockEntity !is ObservationSourceContainerBlockEntity) {
                        return false
                    }
                }
                return true
            }
        }

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
            val playerPos = player.position()
            if (player.level().dimension() != dimension)
                return false
            val deltaX = pos.x + 0.5 - playerPos.x
            val deltaZ = pos.z + 0.5 - playerPos.z
            return !(deltaX > RANGE || deltaX < -RANGE || deltaZ > RANGE || deltaZ < -RANGE)
        }
    }

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    private val cooldownTicksRemaining: AtomicInteger = AtomicInteger(0)
    var tickInterval: UInt = tickInterval
        set(value) {
            field = value
            val intValue = if(value < Int.MAX_VALUE.toUInt()) value.toInt() else Int.MAX_VALUE
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
        requestName: String,
        checkDimension: Boolean = true,
        checkDistance: Boolean = true,
        checkLoaded: Boolean = true,
        checkBlockEntity: Boolean = true,
    ): Boolean {
        val playerLevel = player.level()
        if (checkDimension && playerLevel.dimension() != pos.dimension) {
            OTelCoreMod.logger.trace(
                "Ignoring {} because of dimension mismatch: Requested {} in {} but player is in {}",
                requestName,
                pos.pos,
                pos.dimension,
                playerLevel.dimension()
            )
            return false
        }
        if (checkDistance) {
            val playerPos = player.position()
            val deltaX = pos.pos.x + 0.5 - playerPos.x
            val deltaZ = pos.pos.z + 0.5 - playerPos.z
            if (deltaX > RANGE || deltaX < -RANGE || deltaZ > RANGE || deltaZ < -RANGE) {
                OTelCoreMod.logger.trace(
                    "Ignoring {} because of distance: Requested {} in {} but player is {},{} (x,z) blocks away",
                    requestName,
                    pos.pos,
                    pos.dimension,
                    abs(deltaX),
                    abs(deltaZ),
                )
                return false
            }
        }
        if ((checkLoaded || checkBlockEntity) && !playerLevel.isLoaded(pos.pos)) {
            OTelCoreMod.logger.trace(
                "Ignoring {} because position is unloaded: Requested {} in {}",
                requestName,
                pos.pos,
                pos.dimension,
            )
            return false
        } else if (checkBlockEntity) {
            val blockEntity = playerLevel.getBlockEntity(pos.pos)
            if (blockEntity !is ObservationSourceContainerBlockEntity) {
                OTelCoreMod.logger.trace(
                    "Ignoring {} because position does not contain an {}: Requested {} in {} is {}",
                    requestName,
                    ObservationSourceContainerBlockEntity::class.java.simpleName,
                    pos.pos,
                    pos.dimension,
                    blockEntity
                )
                return false
            }
        }
        return true
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

    fun handleRequestSingle(player: ServerPlayer, pos: GlobalPos) {
        if (!checkCanRequest(player, pos, "ObservationsRequestSingle")) {
            handleRequestStop(player, pos)
            return
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

    fun handleRequestStop(player: ServerPlayer, pos: GlobalPos) {
        readWriteLock.readLock().withLock {
            val map = levelMaps[pos.dimension()] ?: return
            map.computeIfPresent(pos.pos) { _, old ->
                old.remove(player)
                old
            }
        }
    }

    fun handleRequestStart(player: ServerPlayer, pos: GlobalPos, tickInterval: UInt) {
        // accept request even if block is not currently correct block entity -> might be changed in time for next tick
        if (!checkCanRequest(player, pos, "ObservationsRequestStart", checkBlockEntity = false)) {
            handleRequestStop(player, pos)
            return
        }
        val currentTick = player.server.tickCount.toLong()
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


    fun handleRequestKeepalive(player: ServerPlayer, pos: GlobalPos, tickInterval: UInt) {
        if (!checkCanRequest(player, pos, "ObservationsRequestKeepalive")) {
            handleRequestStop(player, pos)
            return
        }
        val currentTick = player.server.tickCount.toLong()
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
        val container = entity.container ?: return
        val memoryRecorder = MemoryObservationRecorder()
        container.observe(memoryRecorder, forceObservation = true)
        observations = memoryRecorder.recordedAsMap()
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
        if(cooldownTicksRemaining.getAndUpdate { (it +1) % tickInterval.toInt() } != 0) return
        readWriteLock.writeLock().withLock {
            val currentTick = server.tickCount.toLong()
            var removePendingRegistrations: ArrayDeque<PlayerBlockRegistration>? = null
            var removePendingBlocks: ArrayDeque<BlockPos>? = null
            var updatePendingRegistrations: ArrayDeque<ServerPlayer>? = null
            for ((level, levelMap) in levelMaps.entries) {
                val serverLevel: ServerLevel? = server.getLevel(level)
                for ((block, registrationMap) in levelMap.entries) {
                    if (!PlayerBlockRegistration.blockIsValid(serverLevel, block)) {
                        removePendingRegistrations = removePendingRegistrations ?: ArrayDeque()
                        removePendingRegistrations.addAll(registrationMap.values)
                    } else {
                        for (registration in registrationMap.values) {
                            if (!registration.validForBlock(level, block)) {
                                removePendingRegistrations = removePendingRegistrations ?: ArrayDeque()
                                removePendingRegistrations.add(registration)
                            } else {
                                if (registration.isTimeout(currentTick)) {
                                    removePendingRegistrations = removePendingRegistrations ?: ArrayDeque()
                                    removePendingRegistrations.add(registration)
                                }
                                if (registration.updateTick(currentTick)) {
                                    updatePendingRegistrations = updatePendingRegistrations ?: ArrayDeque()
                                    updatePendingRegistrations.add(registration.player)
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
                        if (registrationMap.isEmpty()) {
                            removePendingBlocks = removePendingBlocks ?: ArrayDeque()
                            removePendingBlocks.add(block)
                        }
                        removePendingRegistrations.clear()
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
