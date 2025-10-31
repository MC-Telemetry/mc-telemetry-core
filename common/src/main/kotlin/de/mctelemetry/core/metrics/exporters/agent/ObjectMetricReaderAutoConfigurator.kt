package de.mctelemetry.core.metrics.exporters.agent

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider

class ObjectMetricReaderAutoConfigurator : AutoConfigurationCustomizerProvider {

    private val isDebug = "true".equals(System.getenv("MCTELEMETRYCORE_DEBUG"), ignoreCase = true) ||
            "true".equals(System.getProperty("mctelemetrycore.debug"), ignoreCase = true)

    // Logger unavailable in Agent-Extension context.
    private fun debugLog(message: String) {
        if (isDebug) {
            println(message)
        }
    }

    override fun customize(autoConfiguration: AutoConfigurationCustomizer) {
        autoConfiguration.addMeterProviderCustomizer { provider, props ->
            debugLog("Customizing meter provider on ClassLoader ${ObjectMetricReaderAutoConfigurator::class.java.classLoader}")
            val newReader = ObjectMetricReader()
            provider.registerMetricReader(newReader).also {
                ObjectMetricsAccessor.provideCallbacks(
                    collectAll = newReader::collect,
                    collectDefinitions = newReader::collectDefinitions,
                    collectDefinition = newReader::collectDefinition,
                    collectNamed = newReader::collectNamed,
                    collectDataPoint = newReader::collectDataPoint,
                    collectDataPointValue = newReader::collectDataPointValue,
                )
            }
        }
    }
}
