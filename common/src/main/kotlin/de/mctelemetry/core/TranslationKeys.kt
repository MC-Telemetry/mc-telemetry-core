package de.mctelemetry.core

import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.attributes.IMappedAttributeKeyType
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object TranslationKeys {
    object Errors {

        const val ERRORS_METRICSACCESSOR_MISSING = "errors.mctelemetry.core.metricsaccessor.missing"
        const val ERRORS_METRIC_NAME_EMPTY = "errors.mctelemetry.core.metric.name.empty"
        const val ERRORS_METRIC_NAME_INVALID_CHAR = "errors.mctelemetry.core.metric.name.invalid_char"
        const val ERRORS_METRIC_NAME_BAD_START = "errors.mctelemetry.core.metric.name.bad_start"
        const val ERRORS_METRIC_NAME_BAD_END = "errors.mctelemetry.core.metric.name.bad_end"
        const val ERRORS_METRIC_NAME_DOUBLE_DELIMITER = "errors.mctelemetry.core.metric.name.double_delimiter"
        const val ERRORS_METRIC_RESPONSE_TYPE_UNEXPECTED = "errors.mctelemetry.core.metric.response_type.unexpected"
        const val ERRORS_WORLD_INSTRUMENT_MANAGER_MISSING = "errors.mctelemetry.core.world.instrument_manager.missing"
        const val ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE = "errors.mctelemetry.core.attributes.type.incompatible"
        const val ERRORS_ATTRIBUTES_MAPPING_MISSING = "errors.mctelemetry.core.attributes.mapping.missing"
        const val ERRORS_OBSERVATIONS_UNINITIALIZED = "errors.mctelemetry.core.observations.uninitialized"
        const val ERRORS_OBSERVATIONS_NOT_CONFIGURED = "errors.mctelemetry.core.observations.not_configured"
        const val ERRORS_OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND = "errors.mctelemetry.core.observations.configuration.instrument.not_found"

        fun metricsAccessorMissing(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_METRICSACCESSOR_MISSING,
                "No metrics-accessor found"
            )

        fun metricNameEmpty(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_METRIC_NAME_EMPTY,
                "Metric name must not be empty"
            )

        fun metricNameInvalidChar(char: String, index: Int): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_METRIC_NAME_INVALID_CHAR,
                $$"Metric name has invalid character at index %2$c: %1$d",
                char,
                index,
            )

        fun metricNameBadStart(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_METRIC_NAME_BAD_START,
                "Metric name must start with a letter"
            )

        fun metricNameBadEnd(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_METRIC_NAME_BAD_END,
                "Metric name must end with a letter or digit"
            )

        fun metricNameDoubleDelimiter(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_METRIC_NAME_DOUBLE_DELIMITER,
                "Metric name must not have two delimiter ('.' and '_') in a row"
            )

        fun metricResponseTypeUnexpected(
            metricName: String,
            actualType: String,
            expectedType: String,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_METRIC_RESPONSE_TYPE_UNEXPECTED,
                $$"Unexpected metric response type for %1$s: Got %2$s but expected %3$s",
                metricName,
                actualType,
                expectedType,
            )

        fun worldInstrumentManagerMissing(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_WORLD_INSTRUMENT_MANAGER_MISSING,
                "Instrument manager missing for world"
            )

        fun attributeTypesIncompatible(
            sourceType: IMappedAttributeKeyType<*, *>,
            targetType: IMappedAttributeKeyType<*, *>,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE,
                $$"Incompatible attribute types: Cannot assign from %1$s to %2$s",
                sourceType.id.toString(),
                targetType.id.toString(),
            )

        fun attributeTypesIncompatible(
            source: MappedAttributeKeyInfo<*, *>,
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE,
                $$"Incompatible attribute types: Cannot assign from %1$s ('%3$s') to %2$s ('%4$s')",
                source.type.id.toString(),
                target.type.id.toString(),
                source.baseKey.key,
                target.baseKey.key,
            )

        fun attributeMappingMissing(
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ATTRIBUTES_MAPPING_MISSING,
                $$"Missing attributes mapping: Cannot find source attribute for '%1$s' (%2$s)",
                target.baseKey.key,
                target.type.id.toString(),
            )

        fun observationsUninitialized(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_OBSERVATIONS_UNINITIALIZED,
                "Observations not initialized",
            )
        fun observationsNotConfigured(): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_OBSERVATIONS_NOT_CONFIGURED,
                "Observations not configured",
            )
        fun observationsConfigurationInstrumentNotFound(name: String): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND,
                $$"Instrument not found: '%1$s'",
                name
            )
    }

    object Commands {

        const val COMMANDS_METRIC_NAME_NOT_FOUND = "commands.mctelemetry.core.metric.name.not_found"
        const val COMMANDS_METRIC_DATAPOINT_NOT_FOUND = "commands.mctelemetry.core.metric.datapoint.not_found"
        const val COMMANDS_METRIC_NONE = "commands.mctelemetry.core.metric.none"
        const val COMMANDS_MCOTEL_SCRAPE_INFO_SUCCESS = "commands.mctelemetry.core.mcotel.scrape.info.success"
        const val COMMANDS_MCOTEL_SCRAPE_CARDINALITY_SUCCESS =
            "commands.mctelemetry.core.mcotel.scrape.cardinality.success"
        const val COMMANDS_MCOTEL_SCRAPE_VALUE_SUCCESS =
            "commands.mctelemetry.core.mcotel.scrape.value.success"
        const val COMMANDS_MCOTEL_METRICS_CREATE_SUCCESS =
            "commands.mctelemetry.core.mcotel.metrics.create.success"
        const val COMMANDS_MCOTEL_METRICS_LIST_SUCCESS =
            "commands.mctelemetry.core.mcotel.metrics.list.success"
        const val COMMANDS_MCOTEL_METRICS_DELETE_SUCCESS =
            "commands.mctelemetry.core.mcotel.metrics.delete.success"

        fun metricNameNotFound(name: String): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_METRIC_NAME_NOT_FOUND,
                $$"Metric not found: %1$s",
                name
            )

        fun metricDatapointNotFound(name: String, labelMap: Map<String, String>): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_METRIC_DATAPOINT_NOT_FOUND,
                $$"Datapoint not found: %1$s with %2$s",
                name,
                labelMap.toString()
            )

        fun noMetrics(): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_METRIC_NONE,
                "No metrics",
            )

        fun scrapeInfoSuccess(count: Int): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_MCOTEL_SCRAPE_INFO_SUCCESS,
                $$"Found %1$s metrics",
                count
            )

        fun scrapeCardinalitySuccess(count: Int, totalCardinality: Long): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_MCOTEL_SCRAPE_CARDINALITY_SUCCESS,
                $$"Found %1$s metrics with a combined cardinality of %2$s",
                count,
                totalCardinality
            )

        fun scrapeValueSuccess(count: Int, sum: Double): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_MCOTEL_SCRAPE_VALUE_SUCCESS,
                $$"Found %1$s data-points with a sum of %2$s",
                count,
                sum
            )

        fun metricsDeleteSuccess(definition: IInstrumentDefinition): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_MCOTEL_METRICS_DELETE_SUCCESS,
                $$"Successfully deleted metric '%1$s'",
                definition.name
            )

        fun metricsListSuccess(count: Int, scope: String): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_MCOTEL_METRICS_LIST_SUCCESS,
                $$"Found %1$s metrics in scope '%2$s'",
                count,
                scope
            )

        fun metricsCreateSuccess(definition: IInstrumentDefinition): MutableComponent =
            Component.translatableWithFallback(
                COMMANDS_MCOTEL_METRICS_CREATE_SUCCESS,
                $$"Successfully created metric '%1$s'",
                definition.name
            )
    }
}
