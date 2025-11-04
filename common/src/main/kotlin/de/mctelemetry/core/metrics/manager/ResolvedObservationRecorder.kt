package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.api.metrics.IObservationRecorder
import de.mctelemetry.core.api.metrics.IObservationSource
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import io.opentelemetry.api.metrics.ObservableMeasurement

internal sealed class ResolvedObservationRecorder<out T : ObservableMeasurement>(
    val observableMeasurement: T,
) : IObservationRecorder.Resolved {

    companion object {

        operator fun invoke(
            measurement: ObservableLongMeasurement,
        ): ResolvedObservationRecorder<ObservableLongMeasurement> {
            return if (measurement is ObservableDoubleMeasurement) OfMixed(measurement, true)
            else OfLong(measurement)
        }

        operator fun invoke(
            measurement: ObservableDoubleMeasurement,
        ): ResolvedObservationRecorder<ObservableDoubleMeasurement> {
            return if (measurement is ObservableLongMeasurement) OfMixed(measurement, false)
            else OfDouble(measurement)
        }

        operator fun invoke(
            measurement: ObservableMeasurement,
            supportsFloating: Boolean,
        ): ResolvedObservationRecorder<ObservableMeasurement> {
            val isDouble = measurement is ObservableDoubleMeasurement
            val isLong = measurement is ObservableLongMeasurement
            return if (isDouble) {
                if (isLong) {
                    OfMixed(measurement, supportsFloating)
                } else {
                    OfDouble(measurement)
                }
            } else {
                if (isLong) {
                    OfLong(measurement)
                } else {
                    throw UnsupportedOperationException("Unsupported ObservableMeasurement (does not implement ObservableLongMeasurement or ObservableDoubleMeasurement): $measurement")
                }
            }
        }
    }

    internal class OfLong(observableMeasurement: ObservableLongMeasurement) :
            ResolvedObservationRecorder<ObservableLongMeasurement>(observableMeasurement) {

        override val supportsFloating: Boolean = false

        override fun observe(
            value: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observableMeasurement.record(value, attributes)
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observe(value.toLong(), attributes, source)
        }

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observe(long, attributes, source)
        }
    }

    internal class OfDouble(observableMeasurement: ObservableDoubleMeasurement) :
            ResolvedObservationRecorder<ObservableDoubleMeasurement>(observableMeasurement) {

        override val supportsFloating: Boolean = true

        override fun observe(
            value: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observe(value.toDouble(), attributes, source)
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observableMeasurement.record(value, attributes)
        }

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observe(double, attributes, source)
        }
    }

    internal class OfMixed<T>(
        observableMeasurement: T,
        override val supportsFloating: Boolean,
    ) : ResolvedObservationRecorder<T>(observableMeasurement)
            where T : ObservableDoubleMeasurement, T : ObservableLongMeasurement {

        override fun observe(
            value: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observableMeasurement.record(value, attributes)
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            observableMeasurement.record(value, attributes)
        }
    }
}
