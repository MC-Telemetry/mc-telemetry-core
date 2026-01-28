package de.mctelemetry.core.api.instruments.histogram.builder

import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.definition.builder.IInstrumentDefinitionBuilder
import de.mctelemetry.core.api.instruments.histogram.IHistogramInstrument
import it.unimi.dsi.fastutil.doubles.DoubleCollection
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet
import org.jetbrains.annotations.Contract


interface IHistogramInstrumentBuilder<out B : IHistogramInstrumentBuilder<B>> : IInstrumentDefinitionBuilder<B> {

    val boundaries: DoubleSortedSet

    @Contract("_ -> this", mutates = "this")
    override fun importInstrument(instrument: IInstrumentDefinition): B {
        return super.importInstrument(instrument).also {
            if (instrument is IHistogramInstrument) {
                boundaries.clear()
                boundaries.addAll(instrument.boundaries)
            }
        }
    }

    @Contract("_ -> this", mutates = "this")
    fun withBoundariesLE(boundaries: DoubleCollection): B {
        this.boundaries.also { it.clear() }.addAll(boundaries)
        @Suppress("UNCHECKED_CAST")
        return this as B
    }
    @Contract("_ -> this", mutates = "this")
    fun withBoundariesLE(boundaries: Collection<Double>): B {
        this.boundaries.also { it.clear() }.addAll(boundaries)
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun withBoundariesLE(vararg boundaries: Double): B {
        this.boundaries.also { it.clear() }.addAll(boundaries.toTypedArray())
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun addBoundaryLE(le: Double): B {
        boundaries.add(le)
        @Suppress("UNCHECKED_CAST")
        return this as B
    }
}
