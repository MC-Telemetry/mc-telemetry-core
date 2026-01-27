package de.mctelemetry.core.instruments.manager.client

import com.mojang.datafixers.util.Either
import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.instruments.definition.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.client.IClientInstrumentManager
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentManager
import de.mctelemetry.core.network.instrumentsync.S2CAllInstrumentsPayload
import de.mctelemetry.core.utils.left
import de.mctelemetry.core.utils.right
import de.mctelemetry.core.utils.runWithExceptionCleanup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import java.util.ArrayDeque
import java.util.Deque
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Environment(EnvType.CLIENT)
object ClientInstrumentMetaManager {

    // fun register() is stored in loader-specific modules as extension on the same sub-path as this file

    val activeManager: IClientInstrumentManager?
        get() = activeWorldManager

    val activeWorldManager: IClientWorldInstrumentManager?
        get() = managerRef.get().left().getOrNull()
    private val managerRef: AtomicReference<Either<ClientWorldInstrumentManager, Deque<(ClientWorldInstrumentManager) -> Unit>>> =
        AtomicReference(Either.right(ArrayDeque()))

    private fun invokeCreationCallbacks(
        manager: ClientWorldInstrumentManager,
        callbacks: Deque<(ClientWorldInstrumentManager) -> Unit>,
    ) {
        runWithExceptionCleanup(cleanup = ::destroy) {
            IClientWorldInstrumentManager.Events.CREATED.invoker().clientWorldInstrumentManagerCreated(manager)
        }
        do {
            val element = callbacks.pollFirst() ?: break
            try {
                element(manager)
            } catch (ex: RuntimeException) {
                OTelCoreMod.logger.error("Exception during WorldInstrumentManager-Callback", ex)
            }
        } while (true)
    }

    fun getExistingOrCreate(): IClientInstrumentManager {
        var oldValue: Either<ClientWorldInstrumentManager, Deque<(ClientWorldInstrumentManager) -> Unit>> =
            managerRef.get()
        val oldManager = oldValue.left().getOrNull()
        if (oldManager != null) {
            return oldManager
        }
        val newManager = ClientWorldInstrumentManager()
        val newValue: Either<ClientWorldInstrumentManager, Deque<(ClientWorldInstrumentManager) -> Unit>> =
            Either.left(newManager)
        do {
            val existingManager = oldValue.left().getOrNull()
            if (existingManager != null) {
                return existingManager
            }
            val updatedValue = managerRef.compareAndExchange(oldValue, newValue)
            if (updatedValue != oldValue) break
            oldValue = updatedValue
        } while (true)
        invokeCreationCallbacks(newManager, oldValue.right)
        return newManager
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun create(): IClientInstrumentManager {
        val newManager = ClientWorldInstrumentManager()
        val newValue: Either<ClientWorldInstrumentManager, Deque<(ClientWorldInstrumentManager) -> Unit>> =
            Either.left(newManager)
        var oldValue: Either<ClientWorldInstrumentManager, Deque<(ClientWorldInstrumentManager) -> Unit>> =
            managerRef.get()
        do {
            val existingManager = oldValue.left().getOrNull()
            if (existingManager != null) {
                throw IllegalStateException("ClientWorldInstrumentManager already exists: $existingManager")
            }
            val updatedValue = managerRef.compareAndExchange(oldValue, newValue)
            if (updatedValue != oldValue) break
            oldValue = updatedValue
        } while (true)
        invokeCreationCallbacks(newManager, oldValue.right)
        return newManager
    }

    fun populate(payload: S2CAllInstrumentsPayload) {
        val manager = managerRef.get()
            .left()
            .getOrElse { throw java.lang.IllegalStateException("No ClientWorldInstrumentManager exists") }
        if (manager.populate(payload)) {
            IClientWorldInstrumentManager.Events.POPULATED.invoker().clientWorldInstrumentManagerPopulated(manager)
        }
    }

    fun destroy() {
        var oldEntry = managerRef.get()
        var oldManager = oldEntry.left().getOrElse { return }
        IClientWorldInstrumentManager.Events.REMOVING.invoker().clientWorldInstrumentManagerRemoving(oldManager)
        val newEntry: Either<ClientWorldInstrumentManager, Deque<(ClientWorldInstrumentManager) -> Unit>> =
            Either.right(ArrayDeque())
        do {
            val storedEntry = managerRef.compareAndExchange(oldEntry, newEntry)
            if (storedEntry.left().isEmpty) return
            if (storedEntry == oldEntry) break
            oldEntry = storedEntry
        } while (true)
        oldManager = oldEntry.left
        IClientWorldInstrumentManager.Events.REMOVED.invoker().clientWorldInstrumentManagerRemoved(oldManager)
    }

    fun addReservedName(name: String) {
        val manager = managerRef.get()
            .left()
            .getOrElse { throw java.lang.IllegalStateException("No ClientWorldInstrumentManager exists") }
        manager.addReservedName(name)
    }

    fun removeReservedName(name: String) {
        val manager = managerRef.get()
            .left()
            .getOrElse { throw java.lang.IllegalStateException("No ClientWorldInstrumentManager exists") }
        manager.removeReservedName(name)
    }

    fun addReceivedInstrument(instrument: IWorldInstrumentDefinition) {
        val manager = managerRef.get()
            .left()
            .getOrElse { throw java.lang.IllegalStateException("No ClientWorldInstrumentManager exists") }
        manager.addReceivedInstrument(instrument)
    }

    fun removeReceivedInstrument(instrument: IWorldInstrumentDefinition) {
        val manager = managerRef.get()
            .left()
            .getOrElse { throw java.lang.IllegalStateException("No ClientWorldInstrumentManager exists") }
        manager.removeReceivedInstrument(instrument)
    }
}
