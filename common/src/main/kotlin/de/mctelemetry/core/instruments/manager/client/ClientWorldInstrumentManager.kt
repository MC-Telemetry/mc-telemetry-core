package de.mctelemetry.core.instruments.manager.client

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.builder.IRemoteWorldInstrumentDefinitionBuilder
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.network.instrumentsync.A2AInstrumentAddedPayload
import de.mctelemetry.core.network.instrumentsync.A2AInstrumentRemovedPayload
import de.mctelemetry.core.network.instrumentsync.C2SAllInstrumentRequestPayload
import de.mctelemetry.core.network.instrumentsync.S2CAllInstrumentsPayload
import de.mctelemetry.core.utils.plus
import dev.architectury.networking.NetworkManager
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

@Environment(EnvType.CLIENT)
class ClientWorldInstrumentManager(
    reservedNames: Set<String> = setOf(),
    instruments: Collection<IClientWorldInstrumentManager.IClientWorldInstrumentDefinition> = emptyList(),
    localCallbacks: Collection<IInstrumentAvailabilityCallback<IInstrumentDefinition>> = emptyList(),
    globalCallbacks: Collection<IInstrumentAvailabilityCallback<IInstrumentDefinition>> = emptyList(),
) : IClientWorldInstrumentManager {

    companion object {

        private val subLogger: Logger =
            LoggerFactory.getLogger("${OTelCoreMod.MOD_ID}.${ClientWorldInstrumentManager::class.java.simpleName}")
    }

    private val populationJob: AtomicReference<CompletableJob> = AtomicReference(Job())
    private val populatedInitial: AtomicBoolean = AtomicBoolean(false)

    private val _reservedNames: MutableSet<String> = reservedNames.mapTo(mutableSetOf()) { it.lowercase() }
    override val reservedNames: Set<String>
        get() = dataLock.readLock().withLock { _reservedNames.toSet() }

    private val instruments: MutableMap<String, IClientWorldInstrumentManager.IClientWorldInstrumentDefinition> =
        instruments.associateByTo(mutableMapOf()) {
            it.name.lowercase()
        }

    override fun nameAvailable(name: String): Boolean {
        val lowercase = name.lowercase()
        return dataLock.readLock().withLock {
            lowercase !in reservedNames && lowercase !in instruments
        }
    }

    private val localCallbacks: ConcurrentLinkedQueue<IInstrumentAvailabilityCallback<IInstrumentDefinition>> =
        ConcurrentLinkedQueue(localCallbacks)
    private val globalCallbacks: ConcurrentLinkedQueue<IInstrumentAvailabilityCallback<IInstrumentDefinition>> =
        ConcurrentLinkedQueue(globalCallbacks)

    private val dataLock: ReadWriteLock = ReentrantReadWriteLock()

    override fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable {
        globalCallbacks.add(callback)
        return AutoCloseable { globalCallbacks.remove(callback) }
    }

    override fun addLocalCallback(callback: IInstrumentAvailabilityCallback<IInstrumentDefinition>): AutoCloseable {
        localCallbacks.add(callback)
        return AutoCloseable { localCallbacks.remove(callback) }
    }

    private inline fun <T> forEachCollectExceptions(
        callbacks: Collection<T>,
        block: (T) -> Unit,
    ): Exception? {
        return callbacks.fold(null as Exception?) { acc, it ->
            try {
                block(it)
                acc
            } catch (ex: Exception) {
                acc + ex
            }
        }
    }

    override fun instrumentAdded(
        manager: IInstrumentManager,
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (instrument !is IInstrumentDefinition) return
        val exception = forEachCollectExceptions(globalCallbacks) {
            it.instrumentAdded(manager, instrument, phase)
        }
        if (exception != null) {
            throw exception
        }
    }

    override fun instrumentRemoved(
        manager: IInstrumentManager,
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        if (instrument !is IInstrumentDefinition) return
        val exception = forEachCollectExceptions(globalCallbacks) {
            it.instrumentRemoved(manager, instrument, phase)
        }
        if (exception != null) {
            throw exception
        }
    }

    override fun findGlobal(name: String): IClientInstrumentManager.IClientInstrumentDefinition? {
        return findLocal(name)
    }

    override fun findGlobal(pattern: Regex?): Sequence<IClientInstrumentManager.IClientInstrumentDefinition> {
        return findLocal(pattern)
    }

    override fun findLocal(name: String): IClientWorldInstrumentManager.IClientWorldInstrumentDefinition? {
        return dataLock.readLock().withLock { instruments[name.lowercase()] }
    }

    override fun findLocal(pattern: Regex?): Sequence<IClientWorldInstrumentManager.IClientWorldInstrumentDefinition> {
        if (pattern == null) return dataLock.readLock().withLock {
            instruments.values.toList()
        }.asSequence()
        if (RegexOption.LITERAL in pattern.options) {
            return sequenceOf(findLocal(pattern.pattern) ?: return emptySequence())
        }
        return dataLock.readLock().withLock {
            instruments.values.filter { pattern.matches(it.name) }.toList()
        }.asSequence()
    }

    fun populate(allInstruments: S2CAllInstrumentsPayload): Boolean {
        val data = allInstruments.instruments.associateBy { it.name.lowercase() }
        var exceptionAccumulator: Exception? = null
        dataLock.writeLock().withLock {
            _reservedNames.clear()
            _reservedNames.addAll(allInstruments.reservedNames.map { it.lowercase() })
            val matchingInstrumentNames: Set<String> =
                data.mapNotNullTo(mutableSetOf()) { (remoteInstrumentName, remoteInstrument) ->
                    val localInstrument = instruments[remoteInstrumentName]
                    if (localInstrument == null) return@mapNotNullTo null
                    if (
                        localInstrument.description != remoteInstrument.description ||
                        localInstrument.unit != remoteInstrument.unit ||
                        localInstrument.attributes != remoteInstrument.attributes ||
                        localInstrument.persistent != remoteInstrument.persistent ||
                        localInstrument.supportsFloating != remoteInstrument.supportsFloating
                    ) return@mapNotNullTo null
                    remoteInstrumentName
                }
            val pendingRemovals: Collection<IWorldInstrumentDefinition> = (instruments - matchingInstrumentNames).values
            val pendingAdditions: Collection<IWorldInstrumentDefinition> = (data - matchingInstrumentNames).values
            for (removal in pendingRemovals) {
                try {
                    removeReceivedInstrument(removal)
                } catch (ex: Exception) {
                    exceptionAccumulator += ex
                }
            }
            for (addition in pendingAdditions) {
                try {
                    addReceivedInstrument(addition)
                } catch (ex: Exception) {
                    exceptionAccumulator += ex
                }
            }
        }
        populationJob.get().complete()
        return populatedInitial.compareAndSet(false, true).also {
            if(exceptionAccumulator != null) throw exceptionAccumulator
        }
    }

    override fun requestFullUpdate() {
        val job = populationJob.get()
        if (!job.isCompleted) {
            subLogger.debug("Skipping request for full update because current state is not populated anyway")
            return
        }
        NetworkManager.sendToServer(C2SAllInstrumentRequestPayload)
    }

    override suspend fun awaitFullUpdate() {
        var job = populationJob.get()
        if (!job.isCompleted) {
            job.join()
            return
        }
        val newJob = Job()
        do {
            val storedJob = populationJob.compareAndExchange(job, newJob)
            if (storedJob == job) break
            if (!storedJob.isCompleted) {
                storedJob.join()
                return
            }
            job = storedJob
        } while (true)
        NetworkManager.sendToServer(C2SAllInstrumentRequestPayload)
        newJob.join()
    }

    fun addReservedName(name: String) {
        dataLock.writeLock().withLock {
            _reservedNames.add(name.lowercase())
        }
    }

    fun removeReservedName(name: String) {
        dataLock.writeLock().withLock {
            _reservedNames.remove(name.lowercase())
        }
    }

    fun addReceivedInstrument(instrument: IWorldInstrumentDefinition) {
        val clientInstrument: IClientWorldInstrumentManager.IClientWorldInstrumentDefinition =
            instrument as? IClientWorldInstrumentManager.IClientWorldInstrumentDefinition
                ?: object : IClientWorldInstrumentManager.IClientWorldInstrumentDefinition,
                        IWorldInstrumentDefinition by instrument {}
        var exceptionAccumulator: Exception? = forEachCollectExceptions(globalCallbacks) {
            it.instrumentAdded(this, clientInstrument, IInstrumentAvailabilityCallback.Phase.PRE)
        }
        exceptionAccumulator += forEachCollectExceptions(localCallbacks) {
            it.instrumentAdded(this, clientInstrument, IInstrumentAvailabilityCallback.Phase.PRE)
        }
        dataLock.writeLock().withLock {
            instruments.put(instrument.name.lowercase(), clientInstrument)
        }
        exceptionAccumulator += forEachCollectExceptions(localCallbacks) {
            it.instrumentAdded(this, clientInstrument, IInstrumentAvailabilityCallback.Phase.POST)
        }
        exceptionAccumulator += forEachCollectExceptions(globalCallbacks) {
            it.instrumentAdded(this, clientInstrument, IInstrumentAvailabilityCallback.Phase.POST)
        }
        if (exceptionAccumulator != null) {
            throw exceptionAccumulator
        }
    }

    fun removeReceivedInstrument(instrument: IWorldInstrumentDefinition) {
        var exceptionAccumulator: Exception? = null
        var oldValue: IClientWorldInstrumentManager.IClientWorldInstrumentDefinition? = null
        dataLock.writeLock().withLock {
            instruments.compute(instrument.name.lowercase()) { _, old ->
                if (old == null) {
                    subLogger.trace("Removing not stored instrument {}", instrument)
                } else {
                    if (old.persistent != instrument.persistent ||
                        old.attributes != instrument.attributes ||
                        old.supportsFloating != instrument.supportsFloating ||
                        old.unit != instrument.unit ||
                        old.description != instrument.description
                    ) {
                        subLogger.trace("Removing mismatched instrument {}, locally stored {}", instrument, old)
                    }
                    oldValue = old
                    exceptionAccumulator += forEachCollectExceptions(globalCallbacks) {
                        it.instrumentRemoved(this, old, IInstrumentAvailabilityCallback.Phase.PRE)
                    }
                    exceptionAccumulator += forEachCollectExceptions(localCallbacks) {
                        it.instrumentRemoved(this, old, IInstrumentAvailabilityCallback.Phase.PRE)
                    }
                }
                null
            }
        }
        if (oldValue != null) {
            exceptionAccumulator += forEachCollectExceptions(localCallbacks) {
                it.instrumentRemoved(this, oldValue, IInstrumentAvailabilityCallback.Phase.POST)
            }
            exceptionAccumulator += forEachCollectExceptions(globalCallbacks) {
                it.instrumentRemoved(this, oldValue, IInstrumentAvailabilityCallback.Phase.POST)
            }
        }
        if (exceptionAccumulator != null) {
            throw exceptionAccumulator!!
        }
    }

    private class RemoteWorldInstrumentDefinitionBuilder(
        override val name: String,
    ) : IRemoteWorldInstrumentDefinitionBuilder<RemoteWorldInstrumentDefinitionBuilder> {

        override fun sendToServer() {
            NetworkManager.sendToServer(
                A2AInstrumentAddedPayload(
                    build()
                )
            )
        }

        private fun build(): IWorldInstrumentDefinition.Record = IWorldInstrumentDefinition.Record(
            name = name,
            description = description,
            unit = unit,
            attributes = attributes,
            supportsFloating = supportsFloating,
            persistent = persistent,
        )

        override var attributes: List<MappedAttributeKeyInfo<*, *>> = emptyList()
        override var description: String = ""
        override var unit: String = ""
        override var persistent: Boolean = true
        override var supportsFloating: Boolean = true
    }

    override fun gaugeInstrument(name: String): IRemoteWorldInstrumentDefinitionBuilder<*> {
        return RemoteWorldInstrumentDefinitionBuilder(name)
    }

    override fun requestInstrumentRemoval(name: String) {
        NetworkManager.sendToServer(
            A2AInstrumentRemovedPayload(
                findLocal(name) ?: IWorldInstrumentDefinition.Record(
                    name = name,
                    description = "",
                    unit = "",
                    attributes = emptyMap(),
                    supportsFloating = true,
                    persistent = true,
                )
            )
        )
    }
}
