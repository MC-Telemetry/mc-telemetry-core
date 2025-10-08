package de.mctelemetry.core.exporters.metrics

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider
import io.opentelemetry.sdk.metrics.export.MetricReader
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier

class MetricsGameReaderAutoConfigurator : AutoConfigurationCustomizerProvider {

    override fun customize(autoConfiguration: AutoConfigurationCustomizer) {
        val agentLoader = MetricsGameReaderAutoConfigurator::class.java.classLoader
        val appLoader: ClassLoader = ClassLoader.getSystemClassLoader()
        val readerClass: Class<*> = agentLoader.loadClass("de.mctelemetry.core.exporters.metrics.ObjectMetricReader")
        val readerConstructor: Constructor<*> = readerClass.getConstructor()
        val accessorClass: Class<*> = appLoader.loadClass("de.mctelemetry.core.exporters.metrics.MetricAccessor")
        val accessorMakeInstance: Method = accessorClass.getMethod(
            "makeInstance",
            Supplier::class.java,
            Supplier::class.java,
            Function::class.java,
            Function::class.java,
            BiFunction::class.java,
            BiFunction::class.java,
        )
        autoConfiguration.addMeterProviderCustomizer { provider, props ->
            val newReader = readerConstructor.newInstance()
            val collectAll = readerClass.getDeclaredMethod("collect")
            val collectDefinitions = readerClass.getDeclaredMethod("collectDefinitions")
            val collectDefinition = readerClass.getDeclaredMethod("collectDefinition", String::class.java)
            val collectNamed = readerClass.getDeclaredMethod("collectNamed", String::class.java)
            val collectDataPoint = readerClass.getDeclaredMethod("collectDataPoint", String::class.java, String::class.java.arrayType())
            val collectDataPointValue = readerClass.getDeclaredMethod("collectDataPointValue", String::class.java, String::class.java.arrayType())
            @Suppress("UNCHECKED_CAST")
            provider.registerMetricReader(newReader as MetricReader).also {
                accessorMakeInstance.invoke(
                    null,
                    Supplier<Array<Array<Any>>> {
                        collectAll.invoke(newReader) as Array<Array<Any>>
                    }, Supplier<Array<Array<String>>> {
                        collectDefinitions.invoke(newReader) as Array<Array<String>>
                    }, Function<String, Array<String>?> {
                        collectDefinition.invoke(newReader, it) as Array<String>?
                    }, Function<String, Array<Any>?> {
                        collectNamed.invoke(newReader, it) as Array<Any>?
                    }, BiFunction<String, Array<String>, Array<Any>?> { a, b ->
                        collectDataPoint.invoke(newReader, a, b) as Array<Any>?
                    }, BiFunction<String, Array<String>, Any?> { a, b ->
                        collectDataPointValue.invoke(newReader, a, b)
                    }
                )
            }
        }
    }
}
