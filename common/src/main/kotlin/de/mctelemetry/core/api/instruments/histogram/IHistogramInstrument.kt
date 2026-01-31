package de.mctelemetry.core.api.instruments.histogram

import de.mctelemetry.core.api.attributes.stores.AttributeKeyMapAttributeStore
import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.observations.IObservationRecorder
import io.opentelemetry.api.common.Attributes
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet

sealed interface IHistogramInstrument : IInstrumentDefinition {
    val boundaries: DoubleSortedSet

    interface Resolved : IHistogramInstrument, IObservationRecorder.Resolved {
        operator fun get(attributes: Attributes): DataPoint<Resolved, Attributes> {
            return object : DataPoint<Resolved, Attributes> {
                override val attributes: Attributes = attributes
                override val baseHistogram: Resolved = this@Resolved
                override fun observe(value: Double) {
                    baseHistogram.observe(value, this.attributes)
                }

                override fun observe(value: Long) {
                    baseHistogram.observe(value, this.attributes)
                }

                override fun observePreferred(double: Double, long: Long) {
                    baseHistogram.observePreferred(double, long, this.attributes)
                }
            }
        }
    }

    interface Unresolved : IHistogramInstrument, IObservationRecorder.Unresolved.Sourceless {
        operator fun get(attributes: IAttributeValueStore): DataPoint<Unresolved, IAttributeValueStore> {
            return object : DataPoint<Unresolved, IAttributeValueStore> {
                override val baseHistogram: Unresolved = this@Unresolved
                override val attributes: IAttributeValueStore = AttributeKeyMapAttributeStore(
                    attributes.entries().toList()
                )

                override fun observe(value: Double) {
                    context(this.attributes) {
                        baseHistogram.observe(value)
                    }
                }

                override fun observe(value: Long) {
                    context(this.attributes) {
                        baseHistogram.observe(value)
                    }
                }

                override fun observePreferred(double: Double, long: Long) {
                    context(this.attributes) {
                        baseHistogram.observePreferred(double, long)
                    }
                }
            }
        }
    }


    interface DataPoint<T : IHistogramInstrument, A : Any> {
        val baseHistogram: T
        val attributes: A
        fun observe(value: Double)
        fun observe(value: Long)
        fun observePreferred(double: Double, long: Long) {
            if (baseHistogram.supportsFloating)
                observe(double)
            else
                observe(long)
        }
    }
}
