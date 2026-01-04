package de.mctelemetry.core

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.observations.IObservationSource
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

object TranslationKeys {
    object Errors {

        const val ERRORS_METRICSACCESSOR_MISSING = "errors.${OTelCoreMod.MOD_ID}.metricsaccessor.missing"
        const val ERRORS_METRIC_NAME_EMPTY = "errors.${OTelCoreMod.MOD_ID}.metric.name.empty"
        const val ERRORS_METRIC_NAME_INVALID_CHAR = "errors.${OTelCoreMod.MOD_ID}.metric.name.invalid_char"
        const val ERRORS_METRIC_NAME_BAD_START = "errors.${OTelCoreMod.MOD_ID}.metric.name.bad_start"
        const val ERRORS_METRIC_NAME_BAD_END = "errors.${OTelCoreMod.MOD_ID}.metric.name.bad_end"
        const val ERRORS_METRIC_NAME_DOUBLE_DELIMITER = "errors.${OTelCoreMod.MOD_ID}.metric.name.double_delimiter"
        const val ERRORS_METRIC_RESPONSE_TYPE_UNEXPECTED =
            "errors.${OTelCoreMod.MOD_ID}.metric.response_type.unexpected"
        const val ERRORS_WORLD_INSTRUMENT_MANAGER_MISSING =
            "errors.${OTelCoreMod.MOD_ID}.world.instrument_manager.missing"
        const val ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE = "errors.${OTelCoreMod.MOD_ID}.attributes.type.incompatible"
        const val ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE_TARGET_DETAILED =
            "errors.${OTelCoreMod.MOD_ID}.attributes.type.incompatible.target_detailed"
        const val ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE_DETAILED =
            "errors.${OTelCoreMod.MOD_ID}.attributes.type.incompatible.detailed"
        const val ERRORS_ATTRIBUTES_MAPPING_MISSING = "errors.${OTelCoreMod.MOD_ID}.attributes.mapping.missing"
        const val ERRORS_OBSERVATIONS_UNINITIALIZED = "errors.${OTelCoreMod.MOD_ID}.observations.uninitialized"
        const val ERRORS_OBSERVATIONS_NOT_CONFIGURED = "errors.${OTelCoreMod.MOD_ID}.observations.not_configured"
        const val ERRORS_OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND =
            "errors.${OTelCoreMod.MOD_ID}.observations.configuration.instrument.not_found"
        const val ERRORS_ENUM_VALUE_NOT_FOUND = "errors.${OTelCoreMod.MOD_ID}.enum.value.not_found"

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
            sourceType: IAttributeKeyTypeTemplate<*, *>,
            targetType: IAttributeKeyTypeTemplate<*, *>,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE,
                $$"Incompatible attribute types: Cannot assign from %1$s to %2$s",
                sourceType.id.location().toString(),
                targetType.id.location().toString(),
            )

        fun attributeTypesIncompatible(
            source: AttributeDataSource.ConstantAttributeData<*>,
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE_TARGET_DETAILED,
                $$"Incompatible attribute types: Cannot assign from %1$s to %2$s ('%3$s')",
                source.type.templateType.id.location().toString(),
                target.templateType.id.location().toString(),
                target.baseKey.key,
            )

        fun attributeTypesIncompatible(
            source: AttributeDataSource.Reference<*>,
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE_DETAILED,
                $$"Incompatible attribute types: Cannot assign from %1$s ('%3$s') to %2$s ('%4$s')",
                source.type.templateType.id.location().toString(),
                target.templateType.id.location().toString(),
                when(source) {
                    is AttributeDataSource.Reference.TypedSlot<*> -> source.info.baseKey.key
                    is AttributeDataSource.Reference.ObservationSourceAttributeReference<*> -> source.attributeName
                },
                target.baseKey.key,
            )

        fun attributeTypesIncompatible(
            source: AttributeDataSource<*>,
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent = when (source) {
            is AttributeDataSource.ConstantAttributeData<*> -> attributeTypesIncompatible(source, target)
            is AttributeDataSource.Reference<*> -> attributeTypesIncompatible(source, target)
        }

        fun attributeMappingMissing(
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ATTRIBUTES_MAPPING_MISSING,
                $$"Missing attributes mapping: Cannot find source attribute for '%1$s' (%2$s)",
                target.baseKey.key,
                target.templateType.id.location().toString(),
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

        fun enumValueNotFound(name: String, enumName: String): MutableComponent =
            Component.translatableWithFallback(
                ERRORS_ENUM_VALUE_NOT_FOUND,
                $$"Enum value not found: '%1$s' of '%2$s'",
                name,
                enumName,
            )
    }

    object Commands {

        const val COMMANDS_METRIC_NAME_NOT_FOUND = "commands.${OTelCoreMod.MOD_ID}.metric.name.not_found"
        const val COMMANDS_METRIC_DATAPOINT_NOT_FOUND = "commands.${OTelCoreMod.MOD_ID}.metric.datapoint.not_found"
        const val COMMANDS_METRIC_NONE = "commands.${OTelCoreMod.MOD_ID}.metric.none"
        const val COMMANDS_MCOTEL_SCRAPE_INFO_SUCCESS = "commands.${OTelCoreMod.MOD_ID}.mcotel.scrape.info.success"
        const val COMMANDS_MCOTEL_SCRAPE_CARDINALITY_SUCCESS =
            "commands.${OTelCoreMod.MOD_ID}.mcotel.scrape.cardinality.success"
        const val COMMANDS_MCOTEL_SCRAPE_VALUE_SUCCESS =
            "commands.${OTelCoreMod.MOD_ID}.mcotel.scrape.value.success"
        const val COMMANDS_MCOTEL_METRICS_CREATE_SUCCESS =
            "commands.${OTelCoreMod.MOD_ID}.mcotel.metrics.create.success"
        const val COMMANDS_MCOTEL_METRICS_LIST_SUCCESS =
            "commands.${OTelCoreMod.MOD_ID}.mcotel.metrics.list.success"
        const val COMMANDS_MCOTEL_METRICS_DELETE_SUCCESS =
            "commands.${OTelCoreMod.MOD_ID}.mcotel.metrics.delete.success"

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

    object ObservationSources {

        const val OBSERVATIONSOURCES_REDSTONE_SCRAPER_POWER =
            "${OTelCoreMod.MOD_ID}.observation_sources.${OTelCoreMod.MOD_ID}.redstone_scraper.power"
        const val OBSERVATIONSOURCES_REDSTONE_SCRAPER_DIRECT_POWER =
            "${OTelCoreMod.MOD_ID}.observation_sources.${OTelCoreMod.MOD_ID}.redstone_scraper.power.direct"
        const val OBSERVATIONSOURCES_REDSTONE_SCRAPER_COMPARATOR =
            "${OTelCoreMod.MOD_ID}.observation_sources.${OTelCoreMod.MOD_ID}.redstone_scraper.power.comparator"

        operator fun get(source: IObservationSource<*, *>): MutableComponent = this[source.id]
        operator fun get(sourceKey: ResourceKey<IObservationSource<*, *>>): MutableComponent =
            this[sourceKey.location()]

        operator fun get(sourceID: ResourceLocation): MutableComponent =
            Component.translatable("${OTelCoreMod.MOD_ID}.observation_sources.${sourceID.namespace.lowercase()}.${sourceID.path.lowercase()}")
    }
}
