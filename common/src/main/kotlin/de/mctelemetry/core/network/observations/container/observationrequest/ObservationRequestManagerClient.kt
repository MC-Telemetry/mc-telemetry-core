package de.mctelemetry.core.network.observations.container.observationrequest

import de.mctelemetry.core.OTelCoreMod
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.GlobalPos
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Environment(EnvType.CLIENT)
@OptIn(ExperimentalTime::class)
class ObservationRequestManagerClient() : AutoCloseable {

    companion object {

        private val subLogger =
            LogManager.getLogger("${OTelCoreMod.MOD_ID}.${ObservationRequestManagerClient::class.simpleName}")

        private var clock: Clock = Clock.System
        fun setClock(clock: Clock) {
            this.clock = clock
        }

        private val SUBSCRIPTION_TIMEOUT: Duration = 500.milliseconds
        private val instance: AtomicReference<ObservationRequestManagerClient?> = AtomicReference(null)

        fun onClientConnecting() {
            val newValue = ObservationRequestManagerClient()
            runWithExceptionCleanup(newValue::close) {
                val oldValue = instance.compareAndExchange(null, newValue)
                if (oldValue != null) {
                    throw IllegalStateException("${ObservationRequestManagerClient::class.java.simpleName} is already in use: $oldValue")
                }
            }
        }

        fun onClientDisconnecting() {
            val oldValue = instance.getAndSet(null) ?: return
            oldValue.close()
        }

        fun getActiveManager(): ObservationRequestManagerClient {
            return getActiveManagerOrNull()
                ?: throw IllegalStateException("No ${ObservationRequestManagerClient::class.simpleName} currently active")
        }

        fun getActiveManagerOrNull(): ObservationRequestManagerClient? {
            return instance.get()
        }
    }


    private sealed class PendingRequest : AutoCloseable {

        abstract fun onResult(observations: ObservationSourceObservationMap): PendingRequest?

        abstract suspend fun awaitNext(): ObservationSourceObservationMap

        abstract fun closeWithoutCallback()

        class Single(val value: CompletableDeferred<ObservationSourceObservationMap> = CompletableDeferred()) :
                PendingRequest() {

            override fun onResult(observations: ObservationSourceObservationMap): Nothing? {
                value.complete(observations)
                return null
            }

            override suspend fun awaitNext(): ObservationSourceObservationMap = value.await()

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
            nextDeferred: CompletableDeferred<ObservationSourceObservationMap>? = null,
            private val removeCallback: (T) -> Unit,
        ) : PendingRequest() {

            private val scopeJob = Job()
            private val scope: CoroutineScope = CoroutineScope(scopeJob)
            private val cleanupTimer: AtomicReference<Job?> = AtomicReference(null)
            private val keepaliveJob: AtomicReference<Job?> = AtomicReference(null)

            private val internalUsage: MutableStateFlow<Int> = MutableStateFlow(0)

            private val nextDeferred: MutableStateFlow<CompletableDeferred<ObservationSourceObservationMap>?> =
                MutableStateFlow(nextDeferred)

            private val flowDeferred: CompletableDeferred<MutableStateFlow<ObservationSourceObservationMap>> =
                CompletableDeferred(scopeJob)

            private val closedRef: AtomicBoolean = AtomicBoolean(false)

            val isClosed: Boolean
                get() = closedRef.get()

            private fun ensureNotClosed() {
                if (closedRef.get()) {
                    throw IllegalStateException("Already closed")
                }
            }

            suspend fun getStateFlow(): StateFlow<ObservationSourceObservationMap> {
                subLogger.trace("Requested StateFlow from SubscriptionRequest for {}", removeCallbackArgs)
                ensureNotClosed()
                currentCoroutineContext().ensureActive()
                internalUsage.update { it + 1 }
                try {
                    currentCoroutineContext().ensureActive()
                    ensureNotClosed()
                    return flowDeferred.await().also {
                        subLogger.trace("StateFlow for SubscriptionRequest for {} ready", removeCallbackArgs)
                        currentCoroutineContext().ensureActive()
                        ensureNotClosed()
                    }
                } finally {
                    internalUsage.update { max(0, it - 1) }
                }
            }

            private fun startCleanupTimer() {
                subLogger.trace("Cleanup timer for Subscription for {} started", removeCallbackArgs)
                val storedTask = cleanupTimer.get()
                if (storedTask != null) return
                val targetTimeRef: AtomicReference<Instant> = AtomicReference(clock.now() + SUBSCRIPTION_TIMEOUT)
                var cleanupJob: Job? = null
                cleanupJob = scope.launch(start = CoroutineStart.LAZY, context = CoroutineName("CleanupJob")) {
                    subLogger.trace("Cleanup job for Subscription for {} started", removeCallbackArgs)
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
                        subLogger.trace(
                            "Cleanup job for Subscription for {} done waiting but cancelling because of job-mismatch",
                            removeCallbackArgs
                        )
                        val ex = CancellationException()
                        cancel(ex)
                        throw ex
                    }
                    subLogger.trace(
                        "Cleanup job for Subscription for {} done waiting, closing",
                        removeCallbackArgs
                    )
                    close()
                }
                val previousJob = cleanupTimer.compareAndExchange(null, cleanupJob)
                if (previousJob != null) {
                    subLogger.trace(
                        "Cleanup job for Subscription for {} prematurely cancelled because of job-mismatch",
                        removeCallbackArgs
                    )
                    cleanupJob.cancel()
                    return
                }
                cleanupJob.start()
            }

            private fun stopCleanupTimer() {
                subLogger.trace("Cleanup timer for Subscription for {} stopping", removeCallbackArgs)
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
                        try {
                            cleanupTimer.get()?.cancel()
                        } finally {
                            scope.cancel()
                        }
                    }
                }
            }

            @Suppress("OPT_IN_USAGE")
            override fun onResult(observations: ObservationSourceObservationMap): Subscription<*> {
                subLogger.trace("Received result for Subscription for {}", removeCallbackArgs)
                do {
                    val deferred = nextDeferred.value ?: break
                    deferred.complete(observations)
                    subLogger.trace(
                        "Triggered deferred {} for Subscription for {}",
                        deferred,
                        removeCallbackArgs
                    )
                } while (nextDeferred.compareAndSet(deferred, null))
                if (flowDeferred.isCompleted) {
                    subLogger.trace(
                        "Providing value to existing flowDeferred for Subscription for {}",
                        removeCallbackArgs
                    )
                    flowDeferred.getCompleted().value = observations
                } else {
                    subLogger.trace(
                        "No existing flowDeferred found for Subscription for {}, creating new",
                        removeCallbackArgs
                    )
                    val deferredValue = MutableStateFlow(observations)
                    val loggedSubscriptionCountFlow = deferredValue.subscriptionCount.onEach {
                        subLogger.trace(
                            "Subscriber count for flowDeferred for Subscription for {}: {}",
                            removeCallbackArgs,
                            it
                        )
                    }
                    val loggedInternalUsageCountFlow = internalUsage.onEach {
                        subLogger.trace(
                            "Internal usage count for Subscription for {}: {}",
                            removeCallbackArgs,
                            it
                        )
                    }
                    val loggedHasDeferredFlow = nextDeferred.map { it != null }.distinctUntilChanged().onEach {
                        subLogger.trace("Deferred present for Subscription for {}: {}", removeCallbackArgs, it)
                    }
                    val cleanupTimerFlow = loggedSubscriptionCountFlow
                        .combine(loggedInternalUsageCountFlow) { subCount, internalCount ->
                            (subCount + internalCount) > 0
                        }.combine(loggedHasDeferredFlow) { hasSubscribers, nextDeferredValue ->
                            hasSubscribers || nextDeferredValue
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

            fun deferredForNext(): Deferred<ObservationSourceObservationMap> {
                val currentDeferred = nextDeferred.value
                if (currentDeferred != null) return currentDeferred
                val nextSubmission = CompletableDeferred<ObservationSourceObservationMap>(scopeJob)
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

            override suspend fun awaitNext(): ObservationSourceObservationMap {
                return deferredForNext().await()
            }

            fun startKeepaliveJob(pos: GlobalPos, updateIntervalTicks: UInt) {
                ensureNotClosed()
                if (keepaliveJob.get() != null) return
                val job = scope.launch(context = CoroutineName("KeepaliveJob"), start = CoroutineStart.LAZY) {
                    subLogger.trace(
                        "Keepalive Job for Subscription for {} started",
                        removeCallbackArgs
                    )
                    keepaliveJobImpl(pos, updateIntervalTicks)
                }
                if (keepaliveJob.compareAndSet(null, job)) {
                    job.start()
                } else {
                    job.cancel()
                }
            }

            private suspend fun keepaliveJobImpl(pos: GlobalPos, updateIntervalTicks: UInt) {
                try {
                    while (coroutineContext.isActive && !closedRef.get()) {
                        delay((((ObservationRequestManagerServer.MAX_AGE_TICKS / 20.0) * 2 / 3)).seconds) // send keepalive after two thirds of the max age has passed
                        if (coroutineContext.isActive && !closedRef.get()) {
                            subLogger.trace(
                                "Keepalive for Subscription for {} triggered",
                                removeCallbackArgs
                            )
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
                    subLogger.trace(
                        "Keepalive for Subscription for {} complete, sending stop signal",
                        removeCallbackArgs
                    )
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
                } catch (ex: Exception) {
                    exceptionStore += ex
                }
                null
            }
        }
        try {
            assert(pendingRequests.isEmpty())
        } catch (ex: Exception) {
            if (exceptionStore != null) {
                ex.addSuppressed(exceptionStore)
            }
            throw ex
        }
        if (exceptionStore != null) {
            throw exceptionStore
        }
    }


    suspend fun requestSingleObservation(pos: GlobalPos): ObservationSourceObservationMap {
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
    ): StateFlow<ObservationSourceObservationMap> {
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
