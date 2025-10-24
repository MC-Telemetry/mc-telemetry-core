@file:Suppress("DuplicatedCode")

package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.api.metrics.MappedAttributeKeyInfo
import de.mctelemetry.core.api.metrics.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.ILongInstrumentRegistration
import de.mctelemetry.core.api.metrics.IMetricDefinition
import de.mctelemetry.core.api.metrics.IObservationObserver
import de.mctelemetry.core.api.metrics.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import de.mctelemetry.core.utils.Union2
import de.mctelemetry.core.utils.Union3
import de.mctelemetry.core.utils.plus
import de.mctelemetry.core.utils.runWithExceptionCleanup
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import io.opentelemetry.api.metrics.ObservableMeasurement
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal typealias InstrumentManagerBaseRegistrationUnion = Union2<
        InstrumentManagerBase.ImmutableGaugeInstrumentRegistration,
        InstrumentManagerBase.MutableGaugeInstrumentRegistration<*>,
        InstrumentManagerBase.GaugeInstrumentRegistration>

internal open class InstrumentManagerBase<GB : InstrumentManagerBase.GaugeInstrumentBuilder<*>> protected constructor(
    val meter: Meter,
    val parent: IInstrumentManager?,
    protected open val localInstruments: ConcurrentMap<String, InstrumentManagerBaseRegistrationUnion> =
        ConcurrentHashMap(),
) : IInstrumentManager {

    protected val allowRegistration: AtomicBoolean = AtomicBoolean(true)

    val allowsRegistration: Boolean
        get() = allowRegistration.get()

    init {
        localInstruments.values.forEach {
            it.value.provideUntrackCallback(this::untrackRegistration)
        }
    }

    override fun findGlobal(pattern: Regex): Sequence<IMetricDefinition> {
        val parent = parent ?: return findLocal(pattern)
        return parent.findGlobal(pattern) + findLocal(pattern)
    }

    override fun findGlobal(name: String): IMetricDefinition? {
        val parent = parent ?: return findLocal(name)
        return parent.findGlobal(name) ?: findLocal(name)
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
        return name !in localInstruments
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
        localInstruments.computeIfPresent(name) { k, v ->
            v.takeIf { v.value !== value }
        }
    }

    protected open fun createImmutableDoubleRegistration(
        builder: GB,
        callback: IInstrumentRegistration.Callback<IDoubleInstrumentRegistration>,
    ): ImmutableGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return ImmutableGaugeInstrumentRegistration(builder, preferIntegral = false, callback)
    }

    protected open fun createImmutableLongRegistration(
        builder: GB,
        callback: IInstrumentRegistration.Callback<ILongInstrumentRegistration>,
    ): ImmutableGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return ImmutableGaugeInstrumentRegistration(builder, preferIntegral = true, callback)
    }

    protected open fun createMutableDoubleRegistration(builder: GB):
            MutableGaugeInstrumentRegistration<*> {
        assertAllowsRegistration()
        return MutableGaugeInstrumentRegistration(builder, preferIntegral = false)
    }

    protected open fun createMutableLongRegistration(builder: GB):
            MutableGaugeInstrumentRegistration<*> {
        assertAllowsRegistration()
        return MutableGaugeInstrumentRegistration(builder, preferIntegral = true)
    }

    override fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*> {
        @Suppress("UNCHECKED_CAST")
        return GaugeInstrumentBuilder(name, this as InstrumentManagerBase<GaugeInstrumentBuilder<*>>)
    }

    internal open class GaugeInstrumentBuilder<GB : GaugeInstrumentBuilder<GB>>(
        override val name: String,
        val manager: InstrumentManagerBase<GaugeInstrumentBuilder<GB>>,
    ) : IGaugeInstrumentBuilder<GB> {

        override var unit: String = ""
        override var description: String = ""
        override var attributes: List<MappedAttributeKeyInfo<*, *>> = emptyList()

        override fun registerWithCallbackOfDouble(callback: IInstrumentRegistration.Callback<IDoubleInstrumentRegistration>): IDoubleInstrumentRegistration {
            val result = manager.localInstruments.compute(name) { key, old ->
                val registration = manager.createImmutableDoubleRegistration(this, callback)
                runWithExceptionCleanup(registration::close) {
                    if (old != null) throw IllegalArgumentException("Metric with name $name is already registered: $old")
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
            }
            return (result as Union2.UnionT1).value
        }

        override fun registerWithCallbackOfLong(callback: IInstrumentRegistration.Callback<ILongInstrumentRegistration>): ILongInstrumentRegistration {
            val result = manager.localInstruments.compute(name) { key, old ->
                val registration = manager.createImmutableLongRegistration(this, callback)
                runWithExceptionCleanup(registration::close) {
                    if (old != null) throw IllegalArgumentException("Metric with name $name is already registered: $old")
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
            }
            return (result as Union2.UnionT1).value
        }

        override fun registerMutableOfLong(): ILongInstrumentRegistration.Mutable<*> {
            val result = manager.localInstruments.compute(name) { key, old ->
                val registration = manager.createMutableLongRegistration(this)
                runWithExceptionCleanup(registration::close) {
                    if (old != null) throw IllegalArgumentException("Metric with name $name is already registered: $old")
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
            }
            return (result as Union2.UnionT2).value
        }

        override fun registerMutableOfDouble(): IDoubleInstrumentRegistration.Mutable<*> {
            val result = manager.localInstruments.compute(name) { key, old ->
                val registration = manager.createMutableDoubleRegistration(this)
                runWithExceptionCleanup(registration::close) {
                    if (old != null) throw IllegalArgumentException("Metric with name $name is already registered: $old")
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
            }
            return (result as Union2.UnionT2).value
        }
    }

    internal abstract class GaugeInstrumentRegistration(
        override val name: String,
        override val description: String,
        override val unit: String,
        override val attributes: Map<String, MappedAttributeKeyInfo<*, *>>,
        val preferIntegral: Boolean,
    ) : IDoubleInstrumentRegistration, ILongInstrumentRegistration {

        constructor(builder: GaugeInstrumentBuilder<*>, preferIntegral: Boolean) : this(
            builder.name,
            builder.description,
            builder.unit,
            builder.attributes.associateBy { it.baseKey.key.lowercase() },
            preferIntegral,
        ) {
            untrackCallback.set(builder.manager::untrackRegistration)
        }

        private val otelRegistration: AtomicReference<AutoCloseable?> = AtomicReference(null)
        private val untrackCallback: AtomicReference<((name: String, value: IInstrumentRegistration) -> Unit)?> =
            AtomicReference(null)

        private val closed: AtomicBoolean = AtomicBoolean(false)

        open fun observe(instrument: ObservableMeasurement) {
            if (closed.get()) {
                otelRegistration.get()?.close()
                untrackCallback.get()?.invoke(name, this)
                return
            }
            observeImpl(ResolvedObservationObserver(instrument, preferIntegral))
        }

        protected abstract fun observeImpl(recorder: IObservationObserver.Resolved)

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
            preferIntegral: Boolean,
            callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
        ) : super(name, description, unit, attributes, preferIntegral) {
            this.callback = callback
        }

        constructor(
            builder: GaugeInstrumentBuilder<*>,
            preferIntegral: Boolean,
            callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>,
        ) : super(builder, preferIntegral) {
            this.callback = callback
        }

        val callback: IInstrumentRegistration.Callback<ImmutableGaugeInstrumentRegistration>
        override fun observeImpl(recorder: IObservationObserver.Resolved) {
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
            preferIntegral: Boolean,
        ) : super(
            name,
            description,
            unit,
            attributes,
            preferIntegral,
        )

        constructor(
            builder: GaugeInstrumentBuilder<*>,
            preferIntegral: Boolean,
        ) : super(builder, preferIntegral)

        val callbacks: ConcurrentLinkedDeque<IInstrumentRegistration.Callback<T>> =
            ConcurrentLinkedDeque()

        override fun observeImpl(recorder: IObservationObserver.Resolved) {
            callbacks.forEach {
                it.observe(
                    @Suppress("UNCHECKED_CAST")
                    (this as T),
                    recorder
                )
            }
        }

        override fun addCallback(
            attributes: Attributes,
            callback: IInstrumentRegistration.Callback<T>,
        ): AutoCloseable {
            val closeCallback: AutoCloseable = AutoCloseable {
                callbacks.remove(callback)
            }
            callbacks.add(callback)
            return closeCallback
        }
    }
}
