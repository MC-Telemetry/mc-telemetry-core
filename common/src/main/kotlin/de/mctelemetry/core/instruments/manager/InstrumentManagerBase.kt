@file:Suppress("DuplicatedCode")

package de.mctelemetry.core.instruments.manager

import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.mctelemetry.core.api.instruments.DuplicateInstrumentException
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentSubRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.api.instruments.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.utils.InstrumentAvailabilityLogger
import de.mctelemetry.core.utils.Union2
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.ObservableMeasurement
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal typealias InstrumentManagerBaseRegistrationUnion = Union2<
        InstrumentManagerBase.ImmutableGaugeInstrumentRegistration,
        InstrumentManagerBase.MutableGaugeInstrumentRegistration<*>,
        InstrumentManagerBase.GaugeInstrumentRegistration>

internal abstract class InstrumentManagerBase<GB : InstrumentManagerBase.GaugeInstrumentBuilder<*>> protected constructor(
    val meter: Meter,
    protected open val localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion> =
        ConcurrentHashMap(),
    protected open val localCallbacks: ConcurrentLinkedQueue<IInstrumentAvailabilityCallback<IInstrumentDefinition>> = ConcurrentLinkedQueue(),
) : IMutableInstrumentManager {

    protected val allowRegistration: AtomicBoolean = AtomicBoolean(true)

    val allowsRegistration: Boolean
        get() = allowRegistration.get()

    init {
        localInstruments.values.forEach {
            it.value.provideUntrackCallback(this::untrackRegistration)
        }
    }

    init {
        addLocalCallback(InstrumentAvailabilityLogger(this, local = true))
    }

    override fun findLocal(name: String): IInstrumentRegistration? {
        return localInstruments.getOrElse(name.lowercase()) { return null }.value
    }

    override fun findLocal(pattern: Regex): Sequence<IInstrumentRegistration> {
        if (RegexOption.LITERAL in pattern.options) {
            val local = findLocal(pattern.pattern)
            return if (local != null) sequenceOf(local) else emptySequence()
        }
        val insensitivePattern = if (RegexOption.IGNORE_CASE in pattern.options) pattern else {
            Regex(pattern.pattern, pattern.options + RegexOption.IGNORE_CASE)
        }
        return localInstruments.asSequence().mapNotNull { (name, value) ->
            if (!insensitivePattern.matches(name)) return@mapNotNull null
            value.value
        }
    }

    override fun nameAvailable(name: String): Boolean {
        return name.lowercase() !in localInstruments
    }

    protected fun unregisterAllLocal() {
        var firstException: Exception? = null
        outerLoop@ do {
            val (key, value) = localInstruments.entries.firstOrNull() ?: break@outerLoop
            try {
                value.value.close()
            } catch (ex: Exception) {
                if (firstException == null)
                    firstException = ex
                else
                    firstException.addSuppressed(ex)
            }
            localInstruments.remove(key, value)
        } while (true)
        if (firstException != null)
            throw firstException
    }

    protected fun assertAllowsRegistration() {
        if (!allowsRegistration) throw IllegalStateException("Manager does not accept new registrations")
    }

    protected fun untrackRegistration(name: String, value: IInstrumentRegistration) {
        var exceptionAccumulator: Exception? = null
        try {
            triggerOwnInstrumentRemoved(value, IInstrumentAvailabilityCallback.Phase.PRE)
        } catch (ex: Exception) {
            exceptionAccumulator = ex
        }
        try {
            localInstruments.computeIfPresent(name.lowercase()) { k, v ->
                v.takeIf { v.value !== value }
            }
        } catch (ex: Exception) {
            exceptionAccumulator += ex
        }
        try {
            triggerOwnInstrumentRemoved(value, IInstrumentAvailabilityCallback.Phase.POST)
        } catch (ex: Exception) {
            exceptionAccumulator += ex
        }
        if (exceptionAccumulator != null) throw exceptionAccumulator
    }

    protected open fun createImmutableDoubleRegistration(
        builder: GB,
        callback: IInstrumentRegistration.Callback<IDoubleInstrumentRegistration>,
    ): ImmutableGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return ImmutableGaugeInstrumentRegistration(builder, supportsFloating = true, callback)
    }

    protected open fun createImmutableLongRegistration(
        builder: GB,
        callback: IInstrumentRegistration.Callback<ILongInstrumentRegistration>,
    ): ImmutableGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return ImmutableGaugeInstrumentRegistration(builder, supportsFloating = false, callback)
    }

    protected open fun createMutableDoubleRegistration(builder: GB):
            MutableGaugeInstrumentRegistration<*> {
        assertAllowsRegistration()
        return MutableGaugeInstrumentRegistration(builder, supportsFloating = true)
    }

    protected open fun createMutableLongRegistration(builder: GB):
            MutableGaugeInstrumentRegistration<*> {
        assertAllowsRegistration()
        return MutableGaugeInstrumentRegistration(builder, supportsFloating = false)
    }

    override fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*> {
        @Suppress("UNCHECKED_CAST")
        return GaugeInstrumentBuilder(name, this as InstrumentManagerBase<GaugeInstrumentBuilder<*>>)
    }

    protected open fun triggerOwnInstrumentAdded(
        instrument: IInstrumentRegistration,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        val completedCallbacks =
            ArrayDeque<IInstrumentAvailabilityCallback<IInstrumentRegistration>>(localCallbacks.size)
        val cascadeEarly = when (phase) {
            IInstrumentAvailabilityCallback.Phase.PRE -> true
            IInstrumentAvailabilityCallback.Phase.POST -> false
        }
        try {
            if (cascadeEarly) {
                instrumentAdded(this, instrument, phase)
            }
            for (callback in localCallbacks) {
                callback.instrumentAdded(this, instrument, phase)
                completedCallbacks.add(callback)
            }
            if (!cascadeEarly) {
                instrumentAdded(this, instrument, phase)
            }
        } catch (ex: Exception) {
            handleInstrumentAddedCancellation(this, instrument, phase, completedCallbacks, ex, callLocalRemover = true)
            throw ex
        }
    }

    protected open fun <T : IMetricDefinition> handleInstrumentAddedCancellation(
        manager: IInstrumentManager,
        instrument: T,
        additionPhase: IInstrumentAvailabilityCallback.Phase,
        undoCallbacks: ArrayDeque<IInstrumentAvailabilityCallback<T>>,
        exception: Exception,
        callLocalRemover: Boolean,
    ) {
        when (additionPhase) {
            IInstrumentAvailabilityCallback.Phase.PRE -> {}
            IInstrumentAvailabilityCallback.Phase.POST -> {
                //insert artificial removed-pre phase before local state is updated
                for (callback in undoCallbacks) {
                    try {
                        callback.instrumentRemoved(
                            this,
                            instrument,
                            IInstrumentAvailabilityCallback.Phase.PRE
                        )
                    } catch (ex2: Exception) {
                        exception.addSuppressed(ex2)
                    }
                }
            }
        }
        if (callLocalRemover) {
            try {
                removeLocalInstrumentDuringAddException(instrument, additionPhase)
            } catch (ex2: Exception) {
                exception.addSuppressed(ex2)
            }
        }
        for (callback in undoCallbacks) {
            try {
                callback.instrumentRemoved(this, instrument, IInstrumentAvailabilityCallback.Phase.POST)
            } catch (ex2: Exception) {
                exception.addSuppressed(ex2)
            }
        }
    }

    protected open fun removeLocalInstrumentDuringAddException(
        instrument: IMetricDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        when (phase) {
            IInstrumentAvailabilityCallback.Phase.PRE -> {
                // add-pre is called inside compute-block for new value.
                // Because the triggering exception is rethrown, the newly added value is not actually stored and
                // nothing needs to be done.
            }
            IInstrumentAvailabilityCallback.Phase.POST -> {
                // value is already stored, needs to be closed (removes itself)
                try {
                    (instrument as IInstrumentRegistration).close()
                } finally {
                    assert(localInstruments[instrument.name.lowercase()] != instrument) {
                        "Instrument $instrument still existed in localInstruments despite being removed"
                    }
                }
            }
        }
    }


    protected open fun triggerOwnInstrumentRemoved(
        instrument: IInstrumentRegistration,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        var exceptionAccumulator: Exception? = null
        val cascadeEarly = when (phase) {
            IInstrumentAvailabilityCallback.Phase.PRE -> true
            IInstrumentAvailabilityCallback.Phase.POST -> false
        }
        if (cascadeEarly) {
            try {
                instrumentRemoved(this, instrument, IInstrumentAvailabilityCallback.Phase.PRE)
            } catch (ex: Exception) {
                exceptionAccumulator = ex
            }
        }
        for (callback in localCallbacks) {
            try {
                callback.instrumentRemoved(this, instrument, phase)
            } catch (ex: Exception) {
                exceptionAccumulator += ex
            }
        }
        if (!cascadeEarly) {
            try {
                instrumentRemoved(this, instrument, IInstrumentAvailabilityCallback.Phase.POST)
            } catch (ex: Exception) {
                exceptionAccumulator += ex
            }
        }
        if (exceptionAccumulator != null) {
            throw exceptionAccumulator
        }
    }

    override fun addLocalCallback(callback: IInstrumentAvailabilityCallback<IInstrumentDefinition>): AutoCloseable {
        if (!localCallbacks.add(callback))
            throw IllegalStateException("Cannot add local callback") // should never occur for ConcurrentLinkedQueue
        return AutoCloseable {
            localCallbacks.remove(callback)
        }
    }

    open class Root<GB : GaugeInstrumentBuilder<*>> protected constructor(
        meter: Meter,
        localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion> = ConcurrentHashMap(),
        protected val globalManagerMap: ConcurrentMap<String, IInstrumentManager> = ConcurrentHashMap(),
        protected val globalCallbacks: ConcurrentLinkedQueue<IInstrumentAvailabilityCallback<IMetricDefinition>> = ConcurrentLinkedQueue(),
    ) : InstrumentManagerBase<GB>(meter, localInstruments) {

        init {
            addGlobalCallback(InstrumentAvailabilityLogger(this, local = false))
        }

        override fun findGlobal(pattern: Regex): Sequence<IMetricDefinition> {
            if (RegexOption.LITERAL in pattern.options) {
                val result = findGlobal(pattern.pattern)
                return if (result != null) sequenceOf(result) else emptySequence()
            }
            val insensitivePattern = if (RegexOption.IGNORE_CASE in pattern.options) pattern else {
                Regex(pattern.pattern, pattern.options + RegexOption.IGNORE_CASE)
            }
            return globalManagerMap.asSequence().mapNotNull { (name, value) ->
                if (!insensitivePattern.matches(name)) return@mapNotNull null
                value
            }.distinct().flatMap { it.findLocal(insensitivePattern) }
        }

        override fun findGlobal(name: String): IInstrumentDefinition? {
            val manager = globalManagerMap[name.lowercase()] ?: return null
            return manager.findLocal(name)
        }

        override fun instrumentAdded(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase,
        ) {
            when (phase) {
                IInstrumentAvailabilityCallback.Phase.PRE -> {
                    globalManagerMap.compute(instrument.name.lowercase()) { key, old ->
                        if (old != null) {
                            val localOfOld = old.findLocal(key)
                            if (localOfOld != instrument) {
                                throw DuplicateInstrumentException(
                                    key,
                                    existing = localOfOld,
                                    message = if (localOfOld == null)
                                        "Metric with name $key already exists in $old"
                                    else
                                        "Metric with name $key already exists in $old: $localOfOld"
                                )
                            } else {
                                old
                            }
                        } else {
                            manager
                        }
                    }
                }
                IInstrumentAvailabilityCallback.Phase.POST -> {
                    val stored = globalManagerMap[instrument.name.lowercase()]
                    assert(stored == manager) {
                        "Stored manager ($stored) does not match manager argument ($manager) during POST phase of instrumentAdded"
                    }
                    assert(stored?.findLocal(instrument.name) == instrument) {
                        "Stored manager ($stored) does not know instrument argument ($instrument) during POST phase of instrumentAdded"
                    }
                }
            }
            val completedCallbacks =
                ArrayDeque<IInstrumentAvailabilityCallback<IMetricDefinition>>(globalCallbacks.size)
            try {
                for (callback in globalCallbacks) {
                    callback.instrumentAdded(manager, instrument, phase)
                    completedCallbacks.addLast(callback)
                }
            } catch (ex: Exception) {
                handleInstrumentAddedCancellation(
                    manager,
                    instrument,
                    phase,
                    completedCallbacks,
                    ex,
                    callLocalRemover = false
                )
                throw ex
            }
        }

        override fun instrumentRemoved(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase,
        ) {
            var exceptionAccumulator: Exception? = null
            try {
                when (phase) {
                    IInstrumentAvailabilityCallback.Phase.POST -> {
                        globalManagerMap.computeIfPresent(instrument.name.lowercase()) { key, old ->
                            val localOfOld = old.findLocal(key)
                            if (localOfOld != null) {
                                throw NoSuchElementException(
                                    if (localOfOld == instrument)
                                        "Metric with name $key still exists in $old"
                                    else
                                        "Metric with name $key still exists with different value in $old: $localOfOld"
                                )
                            }
                            null
                        }
                    }
                    IInstrumentAvailabilityCallback.Phase.PRE -> {}
                }
            } catch (ex: Exception) {
                exceptionAccumulator = ex
            }
            for (callback in globalCallbacks) {
                try {
                    callback.instrumentRemoved(manager, instrument, phase)
                } catch (ex: Exception) {
                    exceptionAccumulator += ex
                }
            }
            if (exceptionAccumulator != null) throw exceptionAccumulator
        }

        override fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable {
            if (!globalCallbacks.add(callback))
                throw IllegalStateException("Cannot add local callback") // should never occur for ConcurrentLinkedQueue
            return AutoCloseable {
                globalCallbacks.remove(callback)
            }
        }
    }

    open class Child<GB : GaugeInstrumentBuilder<GB>> protected constructor(
        meter: Meter,
        val parent: InstrumentManagerBase<*>,
        localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion> = ConcurrentHashMap(),
    ) : InstrumentManagerBase<GB>(meter, localInstruments) {

        override fun findGlobal(pattern: Regex): Sequence<IMetricDefinition> {
            return parent.findGlobal(pattern)
        }

        override fun findGlobal(name: String): IMetricDefinition? {
            return parent.findGlobal(name)
        }

        override fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable {
            return parent.addGlobalCallback(callback)
        }

        override fun instrumentAdded(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase,
        ) {
            parent.instrumentAdded(manager, instrument, phase)
        }

        override fun instrumentRemoved(
            manager: IInstrumentManager,
            instrument: IMetricDefinition,
            phase: IInstrumentAvailabilityCallback.Phase,
        ) {
            parent.instrumentRemoved(manager, instrument, phase)
        }
    }

    internal open class GaugeInstrumentBuilder<GB : GaugeInstrumentBuilder<GB>>(
        override val name: String,
        val manager: InstrumentManagerBase<GaugeInstrumentBuilder<GB>>,
    ) : IGaugeInstrumentBuilder<GB> {

        init {
            try {
                Validators.parseOTelName(name, stopAtInvalid = false)
            } catch (ex: CommandSyntaxException) {
                throw IllegalArgumentException("Invalid metric name: $name", ex)
            }
        }

        override var unit: String = ""
        override var description: String = ""
        override var attributes: List<MappedAttributeKeyInfo<*, *>> = emptyList()

        override fun registerWithCallbackOfDouble(callback: IInstrumentRegistration.Callback<IDoubleInstrumentRegistration>): IDoubleInstrumentRegistration {
            val result = manager.localInstruments.compute(name) { key, old ->
                if (old != null) throw IllegalArgumentException("Metric with name $name is already registered: $old")
                val registration = manager.createImmutableDoubleRegistration(this, callback)
                runWithExceptionCleanup(registration::close) {
                    manager.triggerOwnInstrumentAdded(registration, IInstrumentAvailabilityCallback.Phase.PRE)
                    val otelRegistration = manager.meter.gaugeBuilder(name).let {
                        if (unit.isNotEmpty()) it.setUnit(unit) else it
                    }.let {
                        if (description.isNotEmpty()) it.setDescription(description) else it
                    }.buildWithCallback {
                        registration.observe(it)
                    }
                    registration.provideOTelRegistration(otelRegistration)
                    Union2.of1(registration)
                }
            } as Union2.UnionT1
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }

        override fun registerWithCallbackOfLong(callback: IInstrumentRegistration.Callback<ILongInstrumentRegistration>): ILongInstrumentRegistration {
            val result = manager.localInstruments.compute(name) { key, old ->
                if (old != null) throw DuplicateInstrumentException(name, old.value)
                val registration = manager.createImmutableLongRegistration(this, callback)
                runWithExceptionCleanup(registration::close) {
                    manager.triggerOwnInstrumentAdded(registration, IInstrumentAvailabilityCallback.Phase.PRE)
                    val otelRegistration = manager.meter.gaugeBuilder(name).let {
                        if (unit.isNotEmpty()) it.setUnit(unit) else it
                    }.let {
                        if (description.isNotEmpty()) it.setDescription(description) else it
                    }.ofLongs().buildWithCallback {
                        registration.observe(it)
                    }
                    registration.provideOTelRegistration(otelRegistration)
                    Union2.of1(registration)
                }
            } as Union2.UnionT1
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }

        override fun registerMutableOfLong(): ILongInstrumentRegistration.Mutable<*> {
            val result = manager.localInstruments.compute(name) { key, old ->
                if (old != null) throw DuplicateInstrumentException(name, old.value)
                val registration = manager.createMutableLongRegistration(this)
                runWithExceptionCleanup(registration::close) {
                    manager.triggerOwnInstrumentAdded(registration, IInstrumentAvailabilityCallback.Phase.PRE)
                    val otelRegistration = manager.meter.gaugeBuilder(name).let {
                        if (unit.isNotEmpty()) it.setUnit(unit) else it
                    }.let {
                        if (description.isNotEmpty()) it.setDescription(description) else it
                    }.ofLongs().buildWithCallback {
                        registration.observe(it)
                    }
                    registration.provideOTelRegistration(otelRegistration)
                    Union2.of2(registration)
                }
            } as Union2.UnionT2
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }

        override fun registerMutableOfDouble(): IDoubleInstrumentRegistration.Mutable<*> {
            val result = manager.localInstruments.compute(name) { key, old ->
                if (old != null) throw DuplicateInstrumentException(name, old.value)
                val registration = manager.createMutableDoubleRegistration(this)
                runWithExceptionCleanup(registration::close) {
                    manager.triggerOwnInstrumentAdded(registration, IInstrumentAvailabilityCallback.Phase.PRE)
                    val otelRegistration = manager.meter.gaugeBuilder(name).let {
                        if (unit.isNotEmpty()) it.setUnit(unit) else it
                    }.let {
                        if (description.isNotEmpty()) it.setDescription(description) else it
                    }.buildWithCallback {
                        registration.observe(it)
                    }
                    registration.provideOTelRegistration(otelRegistration)
                    Union2.of2(registration)
                }
            } as Union2.UnionT2
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }
    }

    internal abstract class GaugeInstrumentRegistration(
        override val name: String,
        override val description: String,
        override val unit: String,
        override val attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
        val supportsFloating: Boolean,
    ) : IDoubleInstrumentRegistration, ILongInstrumentRegistration {

        constructor(builder: GaugeInstrumentBuilder<*>, supportsFloating: Boolean) : this(
            builder.name,
            builder.description,
            builder.unit,
            builder.attributes.associateBy { it.baseKey.key.lowercase() },
            supportsFloating,
        ) {
            untrackCallback.set(builder.manager::untrackRegistration)
        }

        private val otelRegistration: AtomicReference<AutoCloseable?> = AtomicReference(null)
        private val untrackCallback: AtomicReference<((name: String, value: IInstrumentRegistration) -> Unit)?> =
            AtomicReference(null)

        protected val closed: AtomicBoolean = AtomicBoolean(false)

        open fun observe(instrument: ObservableMeasurement) {
            if (closed.get()) {
                otelRegistration.get()?.close()
                untrackCallback.get()?.invoke(name, this)
                return
            }
            observe(ResolvedObservationRecorder(instrument, supportsFloating = supportsFloating))
        }

        abstract override fun observe(recorder: IObservationRecorder.Resolved)

        fun provideOTelRegistration(otelRegistration: AutoCloseable) {
            val previous = this.otelRegistration.compareAndExchange(null, otelRegistration)
            if (previous != null) throw IllegalStateException("Unregister callback already provided: $previous (tried to set $otelRegistration)")
            if (closed.get())
                otelRegistration.close()
        }

        fun provideUntrackCallback(callback: (name: String, value: IInstrumentRegistration) -> Unit) {
            val previous = untrackCallback.compareAndExchange(null, callback)
            if (previous != null) throw IllegalStateException("Untrack callback already provided: $previous (tried to set $callback)")
            if (closed.get())
                callback.invoke(name, this)
        }

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            var ex: Exception? = null
            try {
                otelRegistration.get()!!.close()
            } catch (ex2: Exception) {
                ex = ex2
            }
            try {
                untrackCallback.get()!!.invoke(name, this)
            } catch (ex2: Exception) {
                if (ex == null)
                    ex = ex2
                else
                    ex.addSuppressed(ex2)
            }
            if (ex != null) throw ex
        }
    }

    internal open class ImmutableGaugeInstrumentRegistration :
            GaugeInstrumentRegistration {

        constructor(
            name: String,
            description: String,
            unit: String,
            attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
            supportsFloating: Boolean,
            callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
        ) : super(name, description, unit, attributes, supportsFloating) {
            this.callback = callback
        }

        constructor(
            builder: GaugeInstrumentBuilder<*>,
            supportsFloating: Boolean,
            callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
        ) : super(builder, supportsFloating) {
            this.callback = callback
        }

        val callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>
        override fun observe(recorder: IObservationRecorder.Resolved) {
            callback.observe(this, recorder)
        }

        override fun close() {
            var accumulator: Exception? = null
            try {
                super.close()
            } catch (ex: Exception) {
                accumulator = ex
            }
            try {
                callback.onRemove(this)
            } catch (ex: Exception) {
                accumulator += ex
            }
            if (accumulator != null) throw accumulator
        }
    }

    internal open class MutableGaugeInstrumentRegistration<T : MutableGaugeInstrumentRegistration<T>> :
            GaugeInstrumentRegistration,
            IDoubleInstrumentRegistration.Mutable<T>,
            ILongInstrumentRegistration.Mutable<T> {

        constructor(
            name: String,
            description: String,
            unit: String,
            attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
            supportsFloating: Boolean,
        ) : super(
            name,
            description,
            unit,
            attributes,
            supportsFloating,
        )

        constructor(
            builder: GaugeInstrumentBuilder<*>,
            supportsFloating: Boolean,
        ) : super(builder, supportsFloating)

        val callbacks: ConcurrentLinkedDeque<IInstrumentRegistration.Callback<T>> =
            ConcurrentLinkedDeque()

        override fun observe(recorder: IObservationRecorder.Resolved) {
            callbacks.forEach {
                it.observe(
                    @Suppress("UNCHECKED_CAST")
                    (this@MutableGaugeInstrumentRegistration as T),
                    recorder
                )
            }
        }

        override fun addCallback(
            attributes: Attributes,
            callback: IInstrumentRegistration.Callback<T>,
        ): IInstrumentSubRegistration<T> {
            val closeCallback: IInstrumentSubRegistration<T> = object : IInstrumentSubRegistration<T> {
                override val baseInstrument: T =
                    @Suppress("UNCHECKED_CAST")
                    (this@MutableGaugeInstrumentRegistration as T)

                override fun close() {
                    callbacks.remove(callback)
                }
            }
            callbacks.add(callback)
            return closeCallback
        }

        override fun close() {
            var accumulator: Exception? = null
            try {
                super.close()
            } catch (ex: Exception) {
                accumulator = ex
            }
            do {
                val callback = callbacks.pollFirst() ?: break
                try {
                    callback.onRemove(
                        @Suppress("UNCHECKED_CAST")
                        (this@MutableGaugeInstrumentRegistration as T)
                    )
                } catch (ex: Exception) {
                    accumulator += ex
                }
            } while (true)
            if (accumulator != null) throw accumulator
        }
    }
}
