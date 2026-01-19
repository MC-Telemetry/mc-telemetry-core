package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import io.opentelemetry.api.common.Attributes

sealed interface IObservationRecorder {

    val supportsFloating: Boolean

    interface Unresolved : IObservationRecorder {

        context(attributes: IAttributeValueStore)
        fun observe(value: Long, sourceInstance: IObservationSourceInstance<*, *, *>)
        context(attributes: IAttributeValueStore)
        fun observe(value: Double, sourceInstance: IObservationSourceInstance<*, *, *>)
        context(attributes: IAttributeValueStore)
        fun observePreferred(
            double: Double,
            long: Long,
            sourceInstance: IObservationSourceInstance<*, *, *>,
        ) = if (supportsFloating) observe(double, sourceInstance)
        else observe(long, sourceInstance)

        fun onNewSource(source: IObservationSourceInstance<*, *, *>) {}

        interface Sourceless: Unresolved {
            context(attributes: IAttributeValueStore)
            fun observe(value: Long)
            context(attributes: IAttributeValueStore)
            fun observe(value: Double)
            context(attributes: IAttributeValueStore)
            fun observePreferred(
                double: Double,
                long: Long,
            ) = if (supportsFloating) observe(double)
            else observe(long)

            context(attributes: IAttributeValueStore)
            override fun observe(value: Long, sourceInstance: IObservationSourceInstance<*, *, *>) {
                observe(value)
            }

            context(attributes: IAttributeValueStore)
            override fun observe(value: Double, sourceInstance: IObservationSourceInstance<*, *, *>) {
                observe(value)
            }

            context(attributes: IAttributeValueStore)
            override fun observePreferred(double: Double, long: Long, sourceInstance: IObservationSourceInstance<*, *, *>) {
                observePreferred(double, long)
            }
        }
    }

    interface Resolved : IObservationRecorder {

        fun observe(value: Long, attributes: Attributes, sourceInstance: IObservationSourceInstance<*, *, *>? = null)
        fun observe(value: Double, attributes: Attributes, sourceInstance: IObservationSourceInstance<*, *, *>? = null)
        fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            sourceInstance: IObservationSourceInstance<*, *, *>? = null,
        ) = if (supportsFloating) observe(double, attributes, sourceInstance)
        else observe(long, attributes, sourceInstance)

        fun onNewSource(sourceInstance: IObservationSourceInstance<*, *, *>) {}
    }
}
