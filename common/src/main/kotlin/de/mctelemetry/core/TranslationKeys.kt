package de.mctelemetry.core

import de.mctelemetry.core.TranslationKeys.Commands.COMMANDS_METRIC_DATAPOINT_NOT_FOUND
import de.mctelemetry.core.TranslationKeys.Commands.COMMANDS_METRIC_NONE
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeTemplate
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.utils.dsl.components.IComponentDSLBuilder.Companion.buildComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import kotlin.toString

object TranslationKeys {
    fun join(separator: Component, vararg values: Component): MutableComponent {
        return buildComponent {
            var first = true
            for (value in values) {
                if (first) {
                    first = false
                } else {
                    +", "
                }

                append(value)
            }
        }
    }

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
            Component.translatable(ERRORS_METRICSACCESSOR_MISSING)

        fun metricNameEmpty(): MutableComponent =
            Component.translatable(ERRORS_METRIC_NAME_EMPTY)

        fun metricNameInvalidChar(char: String, index: Int): MutableComponent =
            Component.translatable(ERRORS_METRIC_NAME_INVALID_CHAR, char, index)

        fun metricNameBadStart(): MutableComponent =
            Component.translatable(ERRORS_METRIC_NAME_BAD_START)

        fun metricNameBadEnd(): MutableComponent =
            Component.translatable(ERRORS_METRIC_NAME_BAD_END)

        fun metricNameDoubleDelimiter(): MutableComponent =
            Component.translatable(ERRORS_METRIC_NAME_DOUBLE_DELIMITER)

        fun metricResponseTypeUnexpected(
            metricName: String,
            actualType: String,
            expectedType: String,
        ): MutableComponent =
            Component.translatable(
                ERRORS_METRIC_RESPONSE_TYPE_UNEXPECTED,
                metricName,
                actualType,
                expectedType,
            )

        fun worldInstrumentManagerMissing(): MutableComponent =
            Component.translatable(ERRORS_WORLD_INSTRUMENT_MANAGER_MISSING)

        fun attributeTypesIncompatible(
            sourceType: IAttributeKeyTypeTemplate<*, *>,
            targetType: IAttributeKeyTypeTemplate<*, *>,
        ): MutableComponent =
            Component.translatable(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE,
                sourceType.id.location().toString(),
                targetType.id.location().toString(),
            )

        fun attributeTypesIncompatible(
            source: AttributeDataSource.ConstantAttributeData<*>,
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent =
            Component.translatable(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE_TARGET_DETAILED,
                source.type.templateType.id.location().toString(),
                target.templateType.id.location().toString(),
                target.baseKey.key,
            )

        fun attributeTypesIncompatible(
            source: AttributeDataSource.Reference<*>,
            target: MappedAttributeKeyInfo<*, *>,
        ): MutableComponent =
            Component.translatable(
                ERRORS_ATTRIBUTES_TYPE_INCOMPATIBLE_DETAILED,
                source.type.templateType.id.location().toString(),
                target.templateType.id.location().toString(),
                when (source) {
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
            Component.translatable(
                ERRORS_ATTRIBUTES_MAPPING_MISSING,
                target.baseKey.key,
                target.templateType.id.location().toString(),
            )

        fun observationsUninitialized(): MutableComponent =
            Component.translatable(ERRORS_OBSERVATIONS_UNINITIALIZED)

        fun observationsNotConfigured(): MutableComponent =
            Component.translatable(ERRORS_OBSERVATIONS_NOT_CONFIGURED)

        fun observationsConfigurationInstrumentNotFound(name: String): MutableComponent =
            Component.translatable(ERRORS_OBSERVATIONS_CONFIGURATION_INSTRUMENT_NOT_FOUND, name)

        fun enumValueNotFound(name: String, enumName: String): MutableComponent =
            Component.translatable(ERRORS_ENUM_VALUE_NOT_FOUND, name, enumName)
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
            Component.translatable(COMMANDS_METRIC_NAME_NOT_FOUND, name)

        fun metricDatapointNotFound(name: String, labelMap: Map<String, String>): MutableComponent =
            Component.translatable(COMMANDS_METRIC_DATAPOINT_NOT_FOUND, name, labelMap.toString())

        fun noMetrics(): MutableComponent =
            Component.translatable(COMMANDS_METRIC_NONE)

        fun scrapeInfoSuccess(count: Int): MutableComponent =
            Component.translatable(COMMANDS_MCOTEL_SCRAPE_INFO_SUCCESS, count)

        fun scrapeCardinalitySuccess(count: Int, totalCardinality: Long): MutableComponent =
            Component.translatable(COMMANDS_MCOTEL_SCRAPE_CARDINALITY_SUCCESS, count, totalCardinality)

        fun scrapeValueSuccess(count: Int, sum: Double): MutableComponent =
            Component.translatable(COMMANDS_MCOTEL_SCRAPE_VALUE_SUCCESS, count, sum)

        fun metricsDeleteSuccess(definition: IInstrumentDefinition): MutableComponent =
            Component.translatable(COMMANDS_MCOTEL_METRICS_DELETE_SUCCESS, definition.name)

        fun metricsListSuccess(count: Int, scope: String): MutableComponent =
            Component.translatable(COMMANDS_MCOTEL_METRICS_LIST_SUCCESS, count, scope)

        fun metricsCreateSuccess(definition: IInstrumentDefinition): MutableComponent =
            Component.translatable(COMMANDS_MCOTEL_METRICS_CREATE_SUCCESS, definition.name)
    }

    object ObservationSources {

        operator fun get(source: IObservationSource<*, *>): MutableComponent = this[source.id]
        operator fun get(sourceKey: ResourceKey<IObservationSource<*, *>>): MutableComponent =
            this[sourceKey.location()]

        operator fun get(sourceID: ResourceLocation): MutableComponent =
            Component.translatable("${OTelCoreMod.MOD_ID}.observation_sources.${sourceID.namespace.lowercase()}.${sourceID.path.lowercase()}")
    }

    object AttributeTypes {

        operator fun get(source: IAttributeKeyTypeTemplate<*, *>): MutableComponent = this[source.id]
        operator fun get(sourceKey: ResourceKey<IAttributeKeyTypeTemplate<*, *>>): MutableComponent =
            this[sourceKey.location()]

        operator fun get(sourceID: ResourceLocation): MutableComponent =
            Component.translatable("${OTelCoreMod.MOD_ID}.attribute_types.${sourceID.namespace.lowercase()}.${sourceID.path.lowercase()}")
    }

    object KeyMappings {

        const val CATEGORY = "key.${OTelCoreMod.MOD_ID}.category"
        const val OPEN_INSTRUMENT_MANAGER = "key.${OTelCoreMod.MOD_ID}.open_instrument_manager"
    }

    object Ui {

        const val DELETE = "ui.${OTelCoreMod.MOD_ID}.delete"
        const val ADD_ATTRIBUTES = "ui.${OTelCoreMod.MOD_ID}.add_attributes"
        const val TYPES = "ui.${OTelCoreMod.MOD_ID}.types"
        const val NONE = "ui.${OTelCoreMod.MOD_ID}.none"
        const val CUSTOM = "ui.${OTelCoreMod.MOD_ID}.custom"
        const val AND = "ui.${OTelCoreMod.MOD_ID}.and"
        const val STATE_OKAY = "ui.${OTelCoreMod.MOD_ID}.state.okay"
        const val STATE_WARNING = "ui.${OTelCoreMod.MOD_ID}.state.warning"
        const val STATE_ERROR = "ui.${OTelCoreMod.MOD_ID}.state.error"
        const val STATE_WARNING_SINGULAR = "ui.${OTelCoreMod.MOD_ID}.state.warning.singular"
        const val STATE_WARNING_PLURAL = "ui.${OTelCoreMod.MOD_ID}.state.warning.plural"
        const val STATE_ERROR_SINGULAR = "ui.${OTelCoreMod.MOD_ID}.state.error.singular"
        const val STATE_ERROR_PLURAL = "ui.${OTelCoreMod.MOD_ID}.state.error.plural"
        const val PREVIEW_PENDING = "ui.${OTelCoreMod.MOD_ID}.preview.pending"
        const val PREVIEW_NONE = "ui.${OTelCoreMod.MOD_ID}.preview.none"
        const val PREVIEW_MORE = "ui.${OTelCoreMod.MOD_ID}.preview.more"
        const val PREVIEW_COMMON_ATTRIBUTES = "ui.${OTelCoreMod.MOD_ID}.preview.common_attributes"
        const val PREVIEW_VALUES = "ui.${OTelCoreMod.MOD_ID}.preview.values"

        fun delete(): MutableComponent =
            Component.translatable(DELETE)

        fun addAttributes(): MutableComponent =
            Component.translatable(ADD_ATTRIBUTES)

        fun types(): MutableComponent =
            Component.translatable(TYPES)

        fun none(): MutableComponent =
            Component.translatable(NONE)

        fun custom(): MutableComponent =
            Component.translatable(CUSTOM)

        fun stateOkay(): MutableComponent =
            Component.translatable(STATE_OKAY)

        fun stateWarning(): MutableComponent =
            Component.translatable(STATE_WARNING)

        fun stateError(): MutableComponent =
            Component.translatable(STATE_ERROR)

        fun stateWarningCount(count: Int): MutableComponent =
            numbering(
                Component.translatable(STATE_WARNING_SINGULAR),
                Component.translatable(STATE_WARNING_PLURAL),
                count
            )

        fun stateErrorCount(count: Int): MutableComponent =
            numbering(
                Component.translatable(STATE_ERROR_SINGULAR),
                Component.translatable(STATE_ERROR_PLURAL),
                count
            )

        fun previewPending(): MutableComponent =
            Component.translatable(PREVIEW_PENDING)

        fun previewNone(): MutableComponent =
            Component.translatable(PREVIEW_NONE)

        fun previewMore(count: Int): MutableComponent =
            Component.translatable(PREVIEW_MORE, count.toString())

        fun previewCommonAttributes(): MutableComponent =
            Component.translatable(PREVIEW_COMMON_ATTRIBUTES)

        fun previewValues(): MutableComponent =
            Component.translatable(PREVIEW_VALUES)

        private fun numbering(singular: Component, plural: Component, count: Int): MutableComponent {
            return if (count <= 0) {
                Component.empty()
            } else if (count == 1) {
                buildComponent {
                    +"1 "
                    +singular
                }
            } else {
                buildComponent {
                    +count.toString()
                    +" "
                    +plural
                }
            }
        }

        fun and(): MutableComponent =
            Component.translatable(AND)
    }
}
