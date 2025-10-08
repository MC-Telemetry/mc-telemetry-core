package de.mctelemetry.core.exporters.metrics

import io.opentelemetry.api.common.Attributes
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import kotlin.collections.forEach

class MetricAccessor(
    private val collectAll: Supplier<Array<Array<Any>>>,
    private val collectDefinitions: Supplier<Array<Array<String>>>,
    private val collectDefinition: Function<String, Array<String>?>,
    private val collectNamed: Function<String, Array<Any>?>,
    private val collectDataPoint: BiFunction<String, Array<String>, Array<Any>?>,
    private val collectDataPointValue: BiFunction<String, Array<String>, Any?>,
) {

    companion object {


        private lateinit var _INSTANCE: MetricAccessor
        var INSTANCE: MetricAccessor?
            get() {
                val systemLoader = ClassLoader.getSystemClassLoader()
                return if (MetricAccessor::class.java.classLoader != systemLoader) {
                    systemLoader.loadClass(MetricAccessor::class.java.canonicalName)!!
                        .getDeclaredMethod(::getInstance.name).invoke(null) as MetricAccessor?
                } else if (::_INSTANCE.isInitialized)
                    _INSTANCE
                else
                    null
            }
            private set(value) {
                val systemLoader = ClassLoader.getSystemClassLoader()
                if (MetricAccessor::class.java.classLoader != systemLoader) {
                    throw IllegalCallerException("Class loader does not match System-ClassLoader: ${MetricAccessor::class.java.classLoader} (self) != ${ClassLoader.getSystemClassLoader()} (System)")
                } else if (::_INSTANCE.isInitialized) {
                    throw IllegalStateException("MetricAccessor-Instance has already been initialized")
                }
                if (value == null) return
                _INSTANCE = value
            }

        @JvmStatic
        fun getInstance(): MetricAccessor? {
            return INSTANCE
        }

        @JvmStatic
        fun makeInstance(
            collectAll: Supplier<Array<Array<Any>>>,
            collectDefinitions: Supplier<Array<Array<String>>>,
            collectDefinition: Function<String, Array<String>?>,
            collectNamed: Function<String, Array<Any>?>,
            collectDataPoint: BiFunction<String, Array<String>, Array<Any>?>,
            collectDataPointValue: BiFunction<String, Array<String>, Any?>,
        ) {
            INSTANCE = MetricAccessor(
                collectAll = collectAll,
                collectDefinitions = collectDefinitions,
                collectDefinition = collectDefinition,
                collectNamed = collectNamed,
                collectDataPoint = collectDataPoint,
                collectDataPointValue = collectDataPointValue,
            )
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
