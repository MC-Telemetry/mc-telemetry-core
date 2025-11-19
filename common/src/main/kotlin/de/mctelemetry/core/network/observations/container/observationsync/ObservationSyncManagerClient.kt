package de.mctelemetry.core.network.observations.container.observationsync

import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import dev.architectury.networking.NetworkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.GlobalPos
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Environment(EnvType.CLIENT)
@OptIn(ExperimentalTime::class)
class ObservationSyncManagerClient() : AutoCloseable {

    companion object {

        private var clock: Clock = Clock.System
        fun setClock(clock: Clock) {
            this.clock = clock
        }

        private val SUBSCRIPTION_TIMEOUT: Duration = 500.milliseconds
        private val instance: AtomicReference<ObservationSyncManagerClient?> = AtomicReference(null)

        fun onClientConnecting() {
            val newValue = ObservationSyncManagerClient()
            runWithExceptionCleanup(newValue::close) {
                val oldValue = instance.compareAndExchange(null, newValue)
                if (oldValue != null) {
                    throw IllegalStateException("${ObservationSyncManagerClient::class.java.simpleName} is already in use: $oldValue")
                }
            }
        }

        fun onClientDisconnecting() {
            val oldValue = instance.getAndSet(null)
            if (oldValue == null) {
                throw IllegalStateException("No ${ObservationSyncManagerClient::class.java.simpleName} currently active")
            }
            oldValue.close()
        }

        fun getActiveManager(): ObservationSyncManagerClient {
            return getActiveManagerOrNull()
                ?: throw IllegalStateException("No ${ObservationSyncManagerClient::class.simpleName} currently active")
        }

        fun getActiveManagerOrNull(): ObservationSyncManagerClient? {
            return instance.get()
        }
    }


    private sealed class PendingRequest: AutoCloseable {

        abstract fun onResult(observations: S2CObservationsPayloadObservationType): PendingRequest?

        abstract suspend fun awaitNext(): S2CObservationsPayloadObservationType

        abstract fun closeWithoutCallback()

        class Single(val value: CompletableDeferred<S2CObservationsPayloadObservationType> = CompletableDeferred()) :
                PendingRequest() {

            override fun onResult(observations: S2CObservationsPayloadObservationType): Nothing? {
                value.complete(observations)
                return null
            }

            override suspend fun awaitNext(): S2CObservationsPayloadObservationType = value.await()

            override fun close() {
                value.cancel()
            }

            fun closeForSubscription() {}

            override fun closeWithoutCallback() {
                close()
            }
        }

        class Subscription<T>(
            private val removeCallbackArgs: T,
            nextDeferred: CompletableDeferred<S2CObservationsPayloadObservationType>? = null,
            private val removeCallback: (T) -> Unit,
        ) : PendingRequest() {

            private val scopeJob = Job()
            private val scope: CoroutineScope = CoroutineScope(scopeJob)
            private val cleanupTimer: AtomicReference<Job?> = AtomicReference(null)
            private val keepaliveJob: AtomicReference<Job?> = AtomicReference(null)

            private val internalUsage: MutableStateFlow<Int> = MutableStateFlow(0)

            private val nextDeferred: MutableStateFlow<CompletableDeferred<S2CObservationsPayloadObservationType>?> =
                MutableStateFlow(nextDeferred)

            private val flowDeferred: CompletableDeferred<MutableStateFlow<S2CObservationsPayloadObservationType>> =
                CompletableDeferred(scopeJob)

            private val closedRef: AtomicBoolean = AtomicBoolean(false)

            val isClosed: Boolean
                get() = closedRef.get()

            private fun ensureNotClosed() {
                if (closedRef.get()) {
                    throw IllegalStateException("Already closed")
                }
            }

            suspend fun getStateFlow(): StateFlow<S2CObservationsPayloadObservationType> {
                ensureNotClosed()
                currentCoroutineContext().ensureActive()
                internalUsage.update { it + 1 }
                try {
                    currentCoroutineContext().ensureActive()
                    ensureNotClosed()
                    return flowDeferred.await().also {
                        currentCoroutineContext().ensureActive()
                        ensureNotClosed()
                    }
                } finally {
                    internalUsage.update { max(0, it - 1) }
                }
            }

            private fun startCleanupTimer() {
                val storedTask = cleanupTimer.get()
                if (storedTask != null) return
                val targetTimeRef: AtomicReference<Instant> = AtomicReference(clock.now() + SUBSCRIPTION_TIMEOUT)
                var cleanupJob: Job? = null
                cleanupJob = scope.launch(start = CoroutineStart.LAZY, context = CoroutineName("CleanupJob")) {
                    requireNotNull(cleanupJob)
                    var storedTargetTime: Instant = targetTimeRef.get()
                    do {
                        val observedTargetTime: Instant = storedTargetTime
                        val currentTime = clock.now()
                        ensureActive()
                        delay(minOf(observedTargetTime - currentTime, SUBSCRIPTION_TIMEOUT))
                        storedTargetTime = targetTimeRef.get()
                    } while (observedTargetTime != storedTargetTime)
                    ensureActive()
                    if (cleanupJob != cleanupTimer.get()) {
                        val ex = CancellationException()
                        cancel(ex)
                        throw ex
                    }
                    close()
                }
                val previousJob = cleanupTimer.compareAndExchange(null, cleanupJob)
                if (previousJob != null) {
                    cleanupJob.cancel()
                    return
                }
                cleanupJob.start()
            }

            private fun stopCleanupTimer() {
                val job = cleanupTimer.getAndSet(null) ?: return
                job.cancel()
            }

            override fun close() {
                if (!closedRef.compareAndSet(false, true)) return
                try {
                    removeCallback(removeCallbackArgs)
                } finally {
                    closeWithoutCallbackImpl()
                }
            }

            override fun closeWithoutCallback() {
                if (!closedRef.compareAndSet(false, true)) return
                closeWithoutCallbackImpl()
            }

            private fun closeWithoutCallbackImpl() {
                try {
                    flowDeferred.cancel()
                } finally {
                    try {
                        nextDeferred.update {
                            it?.cancel()
                            null
                        }
                    } finally {
                        cleanupTimer.get()?.cancel()
                    }
                }
            }

            @Suppress("OPT_IN_USAGE")
            override fun onResult(observations: S2CObservationsPayloadObservationType): Subscription<*> {
                do {
                    val deferred = nextDeferred.value ?: break
                    deferred.complete(observations)
                } while (nextDeferred.compareAndSet(deferred, null))
                if (flowDeferred.isCompleted) {
                    flowDeferred.getCompleted().value = observations
                } else {
                    val deferredValue = MutableStateFlow(observations)
                    val cleanupTimerFlow =
                        deferredValue.subscriptionCount
                            .combine(internalUsage) { subCount, internalCount ->
                                (subCount + internalCount) > 0
                            }.combine(nextDeferred) { hasSubscribers, nextDeferredValue ->
                                hasSubscribers || nextDeferredValue != null
                            }.distinctUntilChanged().onEach {
                                if (it)
                                    stopCleanupTimer()
                                else
                                    startCleanupTimer()
                            }
                    if (flowDeferred.complete(deferredValue)) {
                        cleanupTimerFlow.launchIn(scope + CoroutineName("CleanupManagerJob"))
                    } else {
                        flowDeferred.getCompleted().value = observations
                    }
                }
                return this
            }

            fun deferredForNext(): Deferred<S2CObservationsPayloadObservationType> {
                val currentDeferred = nextDeferred.value
                if (currentDeferred != null) return currentDeferred
                val nextSubmission = CompletableDeferred<S2CObservationsPayloadObservationType>(scopeJob)
                val storedValue = nextDeferred.getAndUpdate {
                    it ?: nextSubmission
                }
                if (storedValue != null) {
                    nextSubmission.cancel()
                    return storedValue
                } else {
                    return nextSubmission
                }
            }

            override suspend fun awaitNext(): S2CObservationsPayloadObservationType {
                return deferredForNext().await()
            }

            fun startKeepaliveJob(pos: GlobalPos, updateIntervalTicks: UInt) {
                ensureNotClosed()
                if (keepaliveJob.get() != null) return
                val job = scope.launch(context = CoroutineName("KeepaliveJob"), start = CoroutineStart.LAZY) {
                    keepaliveJobImpl(pos, updateIntervalTicks)
                }
                if (keepaliveJob.compareAndSet(null, job)) {
                    job.start()
                }
            }

            private suspend fun keepaliveJobImpl(pos: GlobalPos, updateIntervalTicks: UInt) {
                try {
                    while (coroutineContext.isActive && !closedRef.get()) {
                        delay(((ObservationSyncManagerServer.MAX_AGE_TICKS * 2 / 3) * 500).milliseconds) // send keepalive after two thirds of the max age has passed
                        if (coroutineContext.isActive && !closedRef.get()) {
                            NetworkManager.sendToServer(
                                C2SObservationsRequestPayload(
                                    blockPos = pos,
                                    requestType = C2SObservationsRequestPayload.ObservationRequestType.Keepalive(
                                        updateIntervalTicks
                                    ),
                                    clientTick = null,
                                )
                            )
                        }
                    }
                } finally {
                    NetworkManager.sendToServer(
                        C2SObservationsRequestPayload(
                            blockPos = pos,
                            requestType = C2SObservationsRequestPayload.ObservationRequestType.Stop,
                            clientTick = null,
                        )
                    )
                }
            }
        }
    }

    private val closedRef: AtomicBoolean = AtomicBoolean(false)

    private val pendingRequests: ConcurrentMap<GlobalPos, PendingRequest> = ConcurrentHashMap()

    override fun close() {
        if (!closedRef.compareAndSet(false, true)) return
        var exceptionStore: Exception? = null
        pendingRequests.keys.toList().forEach { pos ->
            pendingRequests.computeIfPresent(pos) { _, oldValue ->
                // close during compute to use synchronization of ConcurrentMap
                try {
                    oldValue.closeWithoutCallback()
                } catch (ex: Exception){
                    exceptionStore += ex
                }
                null
            }
        }
        try {
            assert(pendingRequests.isEmpty())
        } catch (ex: Exception){
            if(exceptionStore != null) {
                ex.addSuppressed(exceptionStore)
            }
            throw ex
        }
        if(exceptionStore != null) {
            throw exceptionStore
        }
    }


    suspend fun requestSingleObservation(pos: GlobalPos): S2CObservationsPayloadObservationType {
        if (closedRef.get()) throw IllegalStateException("Already closed")
        val request = pendingRequests.compute(pos) { _, oldRequest ->
            if (closedRef.get()) throw IllegalStateException("Already closed")
            if (oldRequest == null || (oldRequest is PendingRequest.Subscription<*> && oldRequest.isClosed)) {
                PendingRequest.Single().also {
                    NetworkManager.sendToServer(
                        C2SObservationsRequestPayload(
                            blockPos = pos,
                            requestType = C2SObservationsRequestPayload.ObservationRequestType.Single,
                            clientTick = null,
                        )
                    )
                }
            } else oldRequest
        }!!
        return request.awaitNext()
    }

    suspend fun requestObservations(
        pos: GlobalPos,
        updateIntervalTicks: UInt,
    ): StateFlow<S2CObservationsPayloadObservationType> {
        if (closedRef.get()) throw IllegalStateException("Already closed")
        val subscription: PendingRequest.Subscription<*> = pendingRequests.compute(pos) { _, old ->
            if (closedRef.get()) throw IllegalStateException("Already closed")
            when (old) {
                is PendingRequest.Subscription<*> if !old.isClosed -> {
                    return@compute old // bypasses `also` for server-subscription below
                }
                is PendingRequest.Single -> {
                    PendingRequest.Subscription(pos, old.value, removeCallback = pendingRequests::remove)
                }
                else -> {
                    PendingRequest.Subscription(pos, removeCallback = pendingRequests::remove)
                }
            }.also { subscription ->
                // also is bypassed by early return@compute if existing request is already a subscription

                // closeWithoutCallback because exception already cancels storing of this value
                runWithExceptionCleanup(subscription::closeWithoutCallback) {
                    NetworkManager.sendToServer(
                        C2SObservationsRequestPayload(
                            blockPos = pos,
                            requestType = C2SObservationsRequestPayload.ObservationRequestType.Keepalive.Start(
                                updateIntervalTicks
                            ),
                            clientTick = null,
                        )
                    )
                    subscription.startKeepaliveJob(pos, updateIntervalTicks)
                }
            }
        } as PendingRequest.Subscription<*>
        return subscription.getStateFlow()
    }

    fun acceptObservationPayload(payload: S2CObservationsPayload) {
        if (closedRef.get()) throw IllegalStateException("Already closed")
        var exceptionStore: Exception? = null
        pendingRequests.computeIfPresent(payload.blockPos) { _, request ->
            if (closedRef.get()) throw IllegalStateException("Already closed")
            try {
                request.onResult(payload.observations)
            } catch (ex: Exception) {
                exceptionStore = ex
                if (request is PendingRequest.Subscription<*>) {
                    try {
                        request.closeWithoutCallback()
                    } catch (ex2: Exception) {
                        ex.addSuppressed(ex2)
                    }
                }
                null
            }
        }
        if (exceptionStore != null)
            throw exceptionStore
    }
}
