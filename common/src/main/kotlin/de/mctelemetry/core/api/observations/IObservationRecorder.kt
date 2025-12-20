package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import io.opentelemetry.api.common.Attributes

sealed interface IObservationRecorder {

    interface Unresolved : IObservationRecorder {

        val supportsFloating: Boolean

        context(attributes: IMappedAttributeValueLookup)
        fun observe(value: Long, source: IObservationSource<*, *>)
        context(attributes: IMappedAttributeValueLookup)
        fun observe(value: Double, source: IObservationSource<*, *>)
        context(attributes: IMappedAttributeValueLookup)
        fun observePreferred(
            double: Double,
            long: Long,
            source: IObservationSource<*, *>,
        ) = if (supportsFloating) observe(double, source)
        else observe(long, source)

        fun onNewSource(source: IObservationSource<*, *>) {}
    }

    interface Resolved : IObservationRecorder {

        val supportsFloating: Boolean
        fun observe(value: Long, attributes: Attributes, source: IObservationSource<*, *>? = null)
        fun observe(value: Double, attributes: Attributes, source: IObservationSource<*, *>? = null)
        fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>? = null,
        ) = if (supportsFloating) observe(double, attributes, source)
        else observe(long, attributes, source)

        fun onNewSource(source: IObservationSource<*, *>) {}
    }
}
