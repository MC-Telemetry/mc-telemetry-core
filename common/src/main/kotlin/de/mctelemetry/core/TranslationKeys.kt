package de.mctelemetry.core

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

        fun metricsAccessorMissing(): MutableComponent = Component.translatableWithFallback(
            ERRORS_METRICSACCESSOR_MISSING,
            "No metrics-accessor found"
        )

        fun metricNameEmpty(): MutableComponent = Component.translatableWithFallback(
            ERRORS_METRIC_NAME_EMPTY,
            "Metric name must not be empty"
        )

        fun metricNameInvalidChar(char: Char, index: Int): MutableComponent = Component.translatableWithFallback(
            ERRORS_METRIC_NAME_INVALID_CHAR,
            $$"Metric name has invalid character at index %2$c: %1$d",
            char,
            index,
        )

        fun metricNameBadStart(): MutableComponent = Component.translatableWithFallback(
            ERRORS_METRIC_NAME_BAD_START,
            "Metric name must start with a letter"
        )

        fun metricNameBadEnd(): MutableComponent = Component.translatableWithFallback(
            ERRORS_METRIC_NAME_BAD_END,
            "Metric name must end with a letter or digit"
        )

        fun metricNameDoubleDelimiter(): MutableComponent = Component.translatableWithFallback(
            ERRORS_METRIC_NAME_DOUBLE_DELIMITER,
            "Metric name must not have two delimiter ('.' and '_') in a row"
        )
    }

    object Commands {
        const val COMMANDS_METRIC_NAME_NOT_FOUND = "commands.mctelemetry.core.metric.name.not_found"
        const val COMMANDS_METRIC_NONE = "commands.mctelemetry.core.metric.none"

        fun metricNameNotFound(name: String): MutableComponent = Component.translatableWithFallback(
            COMMANDS_METRIC_NAME_NOT_FOUND,
            $$"Metric not found: %1$s",
            name
        )

        fun noMetrics(): MutableComponent = Component.translatableWithFallback(
            COMMANDS_METRIC_NONE,
            "No metrics",
        )
    }
}
