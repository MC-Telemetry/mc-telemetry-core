@file:Suppress("DuplicatedCode")

package de.mctelemetry.core.instruments.manager

import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.DuplicateInstrumentException
import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.gauge.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.ILongInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.histogram.IHistogramInstrument
import de.mctelemetry.core.api.instruments.histogram.builder.IHistogramInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.IInstrumentAvailabilityCallback
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import de.mctelemetry.core.instruments.manager.gauge.GaugeInstrumentRegistration
import de.mctelemetry.core.instruments.manager.gauge.ImmutableGaugeInstrumentRegistration
import de.mctelemetry.core.instruments.manager.gauge.MutableGaugeInstrumentRegistration
import de.mctelemetry.core.instruments.manager.histogram.NativeHistogram
import de.mctelemetry.core.utils.InstrumentAvailabilityLogger
import de.mctelemetry.core.utils.Union3
import de.mctelemetry.core.utils.Validators
import de.mctelemetry.core.utils.forEachCollect
import de.mctelemetry.core.utils.forEachRethrow
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.metrics.Meter
import it.unimi.dsi.fastutil.doubles.DoubleRBTreeSet
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean

internal typealias InstrumentManagerBaseRegistrationUnion = Union3<
        ImmutableGaugeInstrumentRegistration,
        MutableGaugeInstrumentRegistration<*>,
        IHistogramInstrument,
        IInstrumentDefinition>

internal abstract class InstrumentManagerBase<
        GB : InstrumentManagerBase.GaugeInstrumentBuilder<GB>
        > protected constructor(
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
            val value = it.value
            if (value is GaugeInstrumentRegistration)
                value.provideUntrackCallback(this::untrackRegistration)
        }
    }

    init {
        addLocalCallback(InstrumentAvailabilityLogger(this, local = true))
    }

    override fun findLocal(name: String): IInstrumentDefinition? {
        return localInstruments.getOrElse(name.lowercase()) { return null }.value
    }

    override fun findLocal(pattern: Regex?): Sequence<IInstrumentDefinition> {
        if (pattern == null) {
            return localInstruments.values.asSequence().map { it.value }
        }
        val insensitivePattern = if (RegexOption.IGNORE_CASE in pattern.options) pattern else {
            Regex(pattern.pattern, pattern.options + RegexOption.IGNORE_CASE)
        }
        return localInstruments.asSequence().mapNotNull { (name, value) ->
            if (!insensitivePattern.containsMatchIn(name)) return@mapNotNull null
            value.value
        }
    }

    override fun nameAvailable(name: String): Boolean {
        return name.lowercase() !in localInstruments
    }

    protected fun unregisterAllLocal() {
        var firstException: Exception? = null
        outerLoop@ do {
            val (key, union) = localInstruments.entries.firstOrNull() ?: break@outerLoop
            val value = union.value
            if (value is AutoCloseable) {
                try {
                    value.close()
                } catch (ex: Exception) {
                    if (firstException == null)
                        firstException = ex
                    else
                        firstException.addSuppressed(ex)
                }
            }
            localInstruments.remove(key, union)
        } while (true)
        if (firstException != null)
            throw firstException
    }

    protected fun assertAllowsRegistration() {
        if (!allowsRegistration) throw IllegalStateException("Manager does not accept new registrations")
    }

    internal fun untrackRegistration(name: String, value: IInstrumentRegistration) {
        var exceptionAccumulator: Exception? = null
        try {
            triggerOwnInstrumentRemoved(value, IInstrumentAvailabilityCallback.Phase.PRE)
        } catch (ex: Exception) {
            exceptionAccumulator = ex
        }
        try {
            localInstruments.computeIfPresent(name.lowercase()) { _, v ->
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

    protected open fun createDoubleHistogram(builder: HistogramInstrumentBuilder): IHistogramInstrument {
        assertAllowsRegistration()
        val name: String = builder.name
        val unit: String = builder.unit
        val description: String = builder.description
        val attributes: List<MappedAttributeKeyInfo<*, *>> = builder.attributes
        val boundaries: DoubleSortedSet = builder.boundaries
        var otelBuilder = meter.histogramBuilder(name)
        if (unit.isNotEmpty()) otelBuilder = otelBuilder.setUnit(unit)
        if (description.isNotEmpty()) otelBuilder = otelBuilder.setDescription(description)
        if (boundaries.isNotEmpty()) otelBuilder = otelBuilder.setExplicitBucketBoundariesAdvice(boundaries.toList())
        return NativeHistogram.OfDouble(
            name = name,
            description = description,
            unit = unit,
            attributes = attributes,
            boundaries = boundaries,
            histogram = otelBuilder.build()
        )
    }

    protected open fun createLongHistogram(builder: HistogramInstrumentBuilder): IHistogramInstrument {
        assertAllowsRegistration()
        val name: String = builder.name
        val unit: String = builder.unit
        val description: String = builder.description
        val attributes: List<MappedAttributeKeyInfo<*, *>> = builder.attributes
        val boundaries: DoubleSortedSet = builder.boundaries
        var otelBuilder = meter.histogramBuilder(name)
        if (unit.isNotEmpty()) otelBuilder = otelBuilder.setUnit(unit)
        if (description.isNotEmpty()) otelBuilder = otelBuilder.setDescription(description)
        if (boundaries.isNotEmpty()) otelBuilder = otelBuilder.setExplicitBucketBoundariesAdvice(boundaries.toList())
        return NativeHistogram.OfLong(
            name = name,
            description = description,
            unit = unit,
            attributes = attributes,
            boundaries = boundaries,
            histogram = otelBuilder.ofLongs().build()
        )
    }

    override fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*> {
        return GaugeInstrumentBuilder(name, this)
    }

    override fun histogramInstrument(name: String): IHistogramInstrumentBuilder<*> {
        return HistogramInstrumentBuilder(name, this)
    }

    protected open fun triggerOwnInstrumentAdded(
        instrument: IInstrumentDefinition,
        phase: IInstrumentAvailabilityCallback.Phase,
    ) {
        val completedCallbacks =
            ArrayDeque<IInstrumentAvailabilityCallback<IInstrumentDefinition>>(localCallbacks.size)
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
        exceptionAccumulator = localCallbacks.forEachCollect(exceptionAccumulator) {
            it.instrumentRemoved(this, instrument, phase)
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

    open class Root<GB : GaugeInstrumentBuilder<GB>> protected constructor(
        meter: Meter,
        localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion> = ConcurrentHashMap(),
        protected val globalManagerMap: ConcurrentMap<String, IInstrumentManager> = ConcurrentHashMap(),
        protected val globalCallbacks: ConcurrentLinkedQueue<IInstrumentAvailabilityCallback<IMetricDefinition>> = ConcurrentLinkedQueue(),
    ) : InstrumentManagerBase<GB>(meter, localInstruments) {

        init {
            addGlobalCallback(InstrumentAvailabilityLogger(this, local = false))
        }

        override fun findGlobal(pattern: Regex?): Sequence<IMetricDefinition> {
            if (pattern == null) {
                return globalManagerMap.values.asSequence().distinct().flatMap { it.findLocal() }
            }
            val insensitivePattern = if (RegexOption.IGNORE_CASE in pattern.options) pattern else {
                Regex(pattern.pattern, pattern.options + RegexOption.IGNORE_CASE)
            }
            return globalManagerMap.asSequence().mapNotNull { (name, value) ->
                if (!insensitivePattern.containsMatchIn(name)) return@mapNotNull null
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
            globalCallbacks.forEachRethrow(exceptionAccumulator) {
                it.instrumentRemoved(manager, instrument, phase)
            }
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

        override fun findGlobal(pattern: Regex?): Sequence<IMetricDefinition> {
            return parent.findGlobal(pattern)
        }

        override fun findGlobal(name: String): IMetricDefinition? {
            return parent.findGlobal(name)
        }

        override fun addGlobalCallback(callback: IInstrumentAvailabilityCallback<IMetricDefinition>): AutoCloseable {
            return parent.addGlobalCallback(callback)
        }

        override fun nameAvailable(name: String): Boolean {
            return super.nameAvailable(name) && parent.nameAvailable(name)
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
        val manager: InstrumentManagerBase<GB>,
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
            val result = manager.localInstruments.compute(name) { _, old ->
                if (old != null) throw IllegalArgumentException("Metric with name $name is already registered: $old")
                @Suppress("UNCHECKED_CAST")
                val registration = manager.createImmutableDoubleRegistration(this as GB, callback)
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
                    Union3.of1(registration)
                }
            } as Union3.UnionT1
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }

        override fun registerWithCallbackOfLong(callback: IInstrumentRegistration.Callback<ILongInstrumentRegistration>): ILongInstrumentRegistration {
            val result = manager.localInstruments.compute(name) { _, old ->
                if (old != null) throw DuplicateInstrumentException(name, old.value)
                @Suppress("UNCHECKED_CAST")
                val registration = manager.createImmutableLongRegistration(this as GB, callback)
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
                    Union3.of1(registration)
                }
            } as Union3.UnionT1
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }

        override fun registerMutableOfLong(): ILongInstrumentRegistration.Mutable<*> {
            val result = manager.localInstruments.compute(name) { _, old ->
                if (old != null) throw DuplicateInstrumentException(name, old.value)
                @Suppress("UNCHECKED_CAST")
                val registration = manager.createMutableLongRegistration(this as GB)
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
                    Union3.of2(registration)
                }
            } as Union3.UnionT2
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }

        override fun registerMutableOfDouble(): IDoubleInstrumentRegistration.Mutable<*> {
            val result = manager.localInstruments.compute(name) { _, old ->
                if (old != null) throw DuplicateInstrumentException(name, old.value)
                @Suppress("UNCHECKED_CAST")
                val registration = manager.createMutableDoubleRegistration(this as GB)
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
                    Union3.of2(registration)
                }
            } as Union3.UnionT2
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }
    }

    internal open class HistogramInstrumentBuilder(
        override val name: String,
        val manager: InstrumentManagerBase<*>,
    ) : IHistogramInstrumentBuilder<HistogramInstrumentBuilder> {
        override var unit: String = ""
        override var description: String = ""
        override var attributes: List<MappedAttributeKeyInfo<*, *>> = emptyList()
        override val boundaries: DoubleSortedSet = DoubleRBTreeSet()
        override var supportsFloating: Boolean = true

        override fun build(): IHistogramInstrument {
            val result = manager.localInstruments.compute(name) { _, old ->
                if (old != null) throw DuplicateInstrumentException(name, old.value)
                val histogram = if (supportsFloating)
                    manager.createDoubleHistogram(this)
                else
                    manager.createLongHistogram(this)
                if(histogram is AutoCloseable) {
                    runWithExceptionCleanup(histogram::close) {
                        manager.triggerOwnInstrumentAdded(histogram, IInstrumentAvailabilityCallback.Phase.PRE)
                    }
                } else {
                    manager.triggerOwnInstrumentAdded(histogram, IInstrumentAvailabilityCallback.Phase.PRE)
                }
                Union3.of3(histogram)
            } as Union3.UnionT3
            manager.triggerOwnInstrumentAdded(result.value, IInstrumentAvailabilityCallback.Phase.POST)
            return result.value
        }
    }
}
