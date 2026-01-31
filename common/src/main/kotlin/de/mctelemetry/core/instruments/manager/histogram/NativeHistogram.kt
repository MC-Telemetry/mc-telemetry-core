package de.mctelemetry.core.instruments.manager.histogram

import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.histogram.IHistogramInstrument
import de.mctelemetry.core.api.observations.IObservationSourceInstance
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleHistogram
import io.opentelemetry.api.metrics.LongHistogram
import it.unimi.dsi.fastutil.doubles.DoubleCollection
import it.unimi.dsi.fastutil.doubles.DoubleRBTreeSet
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet

sealed class NativeHistogram(
    override val name: String,
    override val description: String,
    override val unit: String,
    attributes: Collection<MappedAttributeKeyInfo<*, *>>,
    boundaries: DoubleCollection,
) : IHistogramInstrument.Resolved {

    override val attributes: Map<String, MappedAttributeKeyInfo<*, *>> = attributes.associateBy { it.baseKey.key }

    final override val boundaries: DoubleSortedSet = DoubleRBTreeSet(boundaries)

    class OfLong(
        name: String,
        description: String,
        unit: String,
        attributes: Collection<MappedAttributeKeyInfo<*, *>>,
        boundaries: DoubleCollection,
        private val histogram: LongHistogram
    ) : NativeHistogram(name, description, unit, attributes, boundaries) {

        override val supportsFloating: Boolean
            get() = false

        override fun observe(
            value: Long,
            attributes: Attributes,
            sourceInstance: IObservationSourceInstance<*, *, *>?
        ) {
            histogram.record(value, attributes)
        }

        override fun observe(
            value: Double,
            attributes: Attributes,
            sourceInstance: IObservationSourceInstance<*, *, *>?
        ) {
            observe(value.toLong(), attributes, sourceInstance)
        }

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            sourceInstance: IObservationSourceInstance<*, *, *>?
        ) {
            observe(long, attributes)
        }
    }

    class OfDouble(
        name: String,
        description: String,
        unit: String,
        attributes: Collection<MappedAttributeKeyInfo<*, *>>,
        boundaries: DoubleCollection,
        private val histogram: DoubleHistogram
    ) : NativeHistogram(name, description, unit, attributes, boundaries) {

        override val supportsFloating: Boolean
            get() = true

        override fun observe(
            value: Double,
            attributes: Attributes,
            sourceInstance: IObservationSourceInstance<*, *, *>?
        ) {
            histogram.record(value, attributes)
        }

        override fun observe(
            value: Long,
            attributes: Attributes,
            sourceInstance: IObservationSourceInstance<*, *, *>?
        ) {
            observe(value.toDouble(), attributes, sourceInstance)
        }

        override fun observePreferred(
            double: Double,
            long: Long,
            attributes: Attributes,
            sourceInstance: IObservationSourceInstance<*, *, *>?
        ) {
            observe(double, attributes)
        }
    }
}
