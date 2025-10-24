package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.api.metrics.IObservationObserver
import de.mctelemetry.core.observations.IObservationSource
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import io.opentelemetry.api.metrics.ObservableMeasurement

internal sealed class ResolvedObservationObserver<out T : ObservableMeasurement>(
    val observableMeasurement: T,
) : IObservationObserver.Resolved {

    companion object {

        operator fun invoke(
            measurement: ObservableLongMeasurement,
        ): ResolvedObservationObserver<ObservableLongMeasurement> {
            return if (measurement is ObservableDoubleMeasurement) OfMixed(measurement, true)
            else OfLong(measurement)
        }

        operator fun invoke(
            measurement: ObservableDoubleMeasurement,
        ): ResolvedObservationObserver<ObservableDoubleMeasurement> {
            return if (measurement is ObservableLongMeasurement) OfMixed(measurement, false)
            else OfDouble(measurement)
        }

        operator fun invoke(
            measurement: ObservableMeasurement,
            preferIntegral: Boolean,
        ): ResolvedObservationObserver<ObservableMeasurement> {
            val isDouble = measurement is ObservableDoubleMeasurement
            val isLong = measurement is ObservableLongMeasurement
            return if (isDouble) {
                if (isLong) {
                    OfMixed(measurement, preferIntegral)
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
            ResolvedObservationObserver<ObservableLongMeasurement>(observableMeasurement) {

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
            ResolvedObservationObserver<ObservableDoubleMeasurement>(observableMeasurement) {

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
        val preferIntegral: Boolean,
    ) : ResolvedObservationObserver<T>(observableMeasurement)
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

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>?,
        ) {
            if (preferIntegral) {
                observe(long, attributes, source)
            } else {
                observe(double, attributes, source)
            }
        }
    }
}
