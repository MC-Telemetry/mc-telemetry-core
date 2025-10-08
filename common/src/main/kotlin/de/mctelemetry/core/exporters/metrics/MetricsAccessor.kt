package de.mctelemetry.core.exporters.metrics

import io.opentelemetry.api.common.Attributes
import java.lang.reflect.AccessFlag
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import kotlin.Any
import kotlin.Array
import kotlin.collections.forEach

class MetricsAccessor(
    private val collectAll: Supplier<Array<Array<Any>>>,
    private val collectDefinitions: Supplier<Array<Array<String>>>,
    private val collectDefinition: Function<String, Array<String>?>,
    private val collectNamed: Function<String, Array<Any>?>,
    private val collectDataPoint: BiFunction<String, Array<String>, Array<Any>?>,
    private val collectDataPointValue: BiFunction<String, Array<String>, Any?>,
) {

    companion object {


        private val isDebug = "true".equals(System.getenv("MCTELEMETRYCORE_DEBUG"), ignoreCase = true)

        // Logger unavailable in Agent-Extension context.
        private fun debugLog(message: String) {
            if (isDebug) {
                println(message)
            }
        }

        private lateinit var collectAll: Supplier<Array<Array<Any>>>
        private lateinit var collectDefinitions: Supplier<Array<Array<String>>>
        private lateinit var collectDefinition: Function<String, Array<String>?>
        private lateinit var collectNamed: Function<String, Array<Any>?>
        private lateinit var collectDataPoint: BiFunction<String, Array<String>, Array<Any>?>
        private lateinit var collectDataPointValue: BiFunction<String, Array<String>, Any?>


        private lateinit var _INSTANCE: MetricsAccessor
        val INSTANCE: MetricsAccessor?
            get() {
                if (::_INSTANCE.isInitialized) {
                    return _INSTANCE
                }
                val callbackArray = getSynchronizedCallbacks() ?: return null
                @Suppress("UNCHECKED_CAST")
                _INSTANCE = MetricsAccessor(
                    collectAll = callbackArray[0] as Supplier<Array<Array<Any>>>,
                    collectDefinitions = callbackArray[1] as Supplier<Array<Array<String>>>,
                    collectDefinition = callbackArray[2] as Function<String, Array<String>?>,
                    collectNamed = callbackArray[3] as Function<String, Array<Any>?>,
                    collectDataPoint = callbackArray[4] as BiFunction<String, Array<String>, Array<Any>?>,
                    collectDataPointValue = callbackArray[5] as BiFunction<String, Array<String>, Any?>,
                )
                return _INSTANCE
            }


        /**
         * Returns an array of callbacks registered with [::provideCallbacks].
         *
         * This method should *not* be used directly and is only made public to allow reflection-access.
         */
        // PublishedApi makes internal visibility public in non-kotlin code.
        @JvmStatic
        @PublishedApi
        internal fun getSynchronizedCallbacks(): Array<Any>? {
            val syncClassLoader = getSynchronizationClassLoader()
            if (syncClassLoader != Companion::class.java.classLoader) {
                debugLog("Retrieving callbacks for ${Companion::class.java.classLoader} from $syncClassLoader")

                // We were not loaded by the synchronization-ClassLoader, load ourselves and call this method there.

                // JvmStatic defines this method as static *on the containing class* (="MetricsAccessor").
                // The companion-method still exists, but is not truly static.
                val accessorClass = syncClassLoader.loadClass(MetricsAccessor::class.java.canonicalName)
                val callbackGetter = accessorClass.getDeclaredMethod(::getSynchronizedCallbacks.name)
                @Suppress("UNCHECKED_CAST")
                return (callbackGetter.invoke(null) as Array<Any>?).also {
                    debugLog("Callbacks retrieved for ${Companion::class.java.classLoader} from $syncClassLoader: $it")
                }
            }
            if (
                !Companion::collectAll.isInitialized ||
                !Companion::collectDefinitions.isInitialized ||
                !Companion::collectDefinition.isInitialized ||
                !Companion::collectNamed.isInitialized ||
                !Companion::collectDataPoint.isInitialized ||
                !Companion::collectDataPointValue.isInitialized
            ) {
                debugLog("Callback uninitialized, returning null from ${Companion::class.java.classLoader}")
                return null
            }
            debugLog("Callbacks initialized, returning them from ${Companion::class.java.classLoader}")
            return arrayOf(
                collectAll,
                collectDefinitions,
                collectDefinition,
                collectNamed,
                collectDataPoint,
                collectDataPointValue,
            )
        }

        private fun getSynchronizationClassLoader(): ClassLoader {
            return ClassLoader.getSystemClassLoader()
        }

        @JvmStatic
        fun provideCallbacks(
            collectAll: Supplier<Array<Array<Any>>>,
            collectDefinitions: Supplier<Array<Array<String>>>,
            collectDefinition: Function<String, Array<String>?>,
            collectNamed: Function<String, Array<Any>?>,
            collectDataPoint: BiFunction<String, Array<String>, Array<Any>?>,
            collectDataPointValue: BiFunction<String, Array<String>, Any?>,
        ) {
            val syncClassLoader = getSynchronizationClassLoader()
            if (syncClassLoader != Companion::class.java.classLoader) {
                debugLog("Providing callbacks by ${Companion::class.java.classLoader} to $syncClassLoader")
                // We were not loaded by the synchronization-ClassLoader, load ourselves and call this method there.

                // JvmStatic defines this method as static *on the containing class* (="MetricsAccessor").
                // The companion-method still exists, but is not truly static.
                val accessorClass = syncClassLoader.loadClass(MetricsAccessor::class.java.canonicalName)
                val provideCallbacksMethod = accessorClass.getDeclaredMethod(
                    ::provideCallbacks.name,
                    Supplier::class.java,
                    Supplier::class.java,
                    Function::class.java,
                    Function::class.java,
                    BiFunction::class.java,
                    BiFunction::class.java,
                )
                assert(AccessFlag.STATIC in provideCallbacksMethod.accessFlags()) { "$provideCallbacksMethod is not static" }
                provideCallbacksMethod.invoke(
                    null,
                    collectAll,
                    collectDefinitions,
                    collectDefinition,
                    collectNamed,
                    collectDataPoint,
                    collectDataPointValue,
                )
                debugLog("Callbacks provided by ${Companion::class.java.classLoader} to $syncClassLoader")
                return
            }
            debugLog("Storing provided callbacks on ${Companion::class.java.classLoader}")
            // Guaranteed to be on the synchronization-ClassLoader.
            if (
                Companion::collectAll.isInitialized ||
                Companion::collectDefinitions.isInitialized ||
                Companion::collectDefinition.isInitialized ||
                Companion::collectNamed.isInitialized ||
                Companion::collectDataPoint.isInitialized ||
                Companion::collectDataPointValue.isInitialized
            ) throw IllegalStateException("Callbacks already initialized")
            Companion.collectAll = collectAll
            Companion.collectDefinitions = collectDefinitions
            Companion.collectDefinition = collectDefinition
            Companion.collectNamed = collectNamed
            Companion.collectDataPoint = collectDataPoint
            Companion.collectDataPointValue = collectDataPointValue
        }
    }

    fun collect(): Map<String, ObjectMetricReconverter.MetricDataReadback> {
        return ObjectMetricReconverter.convertMetrics(collectAll.get())
    }

    fun collectDefinitions(): Map<String, ObjectMetricReconverter.MetricDefinitionReadback> {
        return ObjectMetricReconverter.convertMetricDefinitions(collectDefinitions.get())
    }

    fun collectDefinition(name: String): ObjectMetricReconverter.MetricDefinitionReadback? {
        return ObjectMetricReconverter.convertMetricDefinition(collectDefinition.apply(name) ?: return null)
    }

    fun collectNamed(name: String): ObjectMetricReconverter.MetricDataReadback? {
        return ObjectMetricReconverter.convertMetric(collectNamed.apply(name) ?: return null)
    }

    fun collectDataPoint(name: String, attributes: Map<String, String>): ObjectMetricReconverter.MetricDataReadback? {
        val attributeArray = arrayOfNulls<String>(attributes.size * 2)
        var i = 0
        attributes.forEach {
            attributeArray[i++] = it.key
            attributeArray[i++] = it.value
        }
        @Suppress("UNCHECKED_CAST")
        return ObjectMetricReconverter.convertMetric(
            collectDataPoint.apply(name, attributeArray as Array<String>) ?: return null
        )
    }

    fun collectDataPoint(name: String, attributes: Attributes): ObjectMetricReconverter.MetricDataReadback? {
        val attributeArray = arrayOfNulls<String>(attributes.size() * 2)
        var i = 0
        attributes.forEach { key, value ->
            attributeArray[i++] = key.key
            attributeArray[i++] = value.toString()
        }
        @Suppress("UNCHECKED_CAST")
        return ObjectMetricReconverter.convertMetric(
            collectDataPoint.apply(name, attributeArray as Array<String>) ?: return null
        )
    }

    fun collectDataPointValue(
        name: String,
        attributes: Map<String, String>,
    ): ObjectMetricReconverter.MetricValueReadback? {
        val attributeArray = arrayOfNulls<String>(attributes.size * 2)
        var i = 0
        attributes.forEach {
            attributeArray[i++] = it.key
            attributeArray[i++] = it.value
        }
        @Suppress("UNCHECKED_CAST")
        return ObjectMetricReconverter.convertMetricDataPointValue(
            collectDataPointValue.apply(name, attributeArray as Array<String>) ?: return null
        )
    }

    fun collectDataPointValue(name: String, attributes: Attributes): ObjectMetricReconverter.MetricValueReadback? {
        val attributeArray = arrayOfNulls<String>(attributes.size() * 2)
        var i = 0
        attributes.forEach { key, value ->
            attributeArray[i++] = key.key
            attributeArray[i++] = value.toString()
        }
        @Suppress("UNCHECKED_CAST")
        return ObjectMetricReconverter.convertMetricDataPointValue(
            collectDataPointValue.apply(name, attributeArray as Array<String>) ?: return null
        )
    }
}
