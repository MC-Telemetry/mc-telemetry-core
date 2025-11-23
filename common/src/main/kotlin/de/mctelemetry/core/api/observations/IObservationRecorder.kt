package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import io.opentelemetry.api.common.Attributes

sealed interface IObservationRecorder {

    interface Unresolved : IObservationRecorder {

        val supportsFloating: Boolean

        fun observe(value: Long, attributes: IMappedAttributeValueLookup, source: IObservationSource<*, *>)
        fun observe(value: Double, attributes: IMappedAttributeValueLookup, source: IObservationSource<*, *>)
        fun observePreferred(
            double: Double,
            long: Long,
            attributes: IMappedAttributeValueLookup,
            source: IObservationSource<*, *>,
        ) = if (supportsFloating) observe(double, attributes, source)
        else observe(long, attributes, source)
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
    }
}
