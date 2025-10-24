package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.common.Attributes

sealed interface IObservationObserver {

    interface Unresolved : IObservationObserver {

        fun observe(source: IObservationSource<*, *>, value: Long, attributes: IMappedAttributeValueLookup)
        fun observe(source: IObservationSource<*, *>, value: Double, attributes: IMappedAttributeValueLookup)
        fun observePreferred(
            source: IObservationSource<*, *>,
            double: Double,
            long: Long,
            attributes: IMappedAttributeValueLookup,
        )
    }

    interface Resolved : IObservationObserver {

        fun observe(value: Long, attributes: Attributes, source: IObservationSource<*, *>? = null)
        fun observe(value: Double, attributes: Attributes, source: IObservationSource<*, *>? = null)
        fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            source: IObservationSource<*, *>? = null,
        )
    }
}
