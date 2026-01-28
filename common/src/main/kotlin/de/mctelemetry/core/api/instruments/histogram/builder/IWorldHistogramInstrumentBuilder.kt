package de.mctelemetry.core.api.instruments.histogram.builder

import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.definition.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.definition.builder.IInstrumentDefinitionBuilder
import de.mctelemetry.core.api.instruments.definition.builder.IWorldInstrumentDefinitionBuilder
import de.mctelemetry.core.api.instruments.histogram.IHistogramInstrument
import it.unimi.dsi.fastutil.doubles.DoubleCollection
import it.unimi.dsi.fastutil.doubles.DoubleSortedSet
import org.jetbrains.annotations.Contract


interface IWorldHistogramInstrumentBuilder<out B : IWorldHistogramInstrumentBuilder<B>> : IHistogramInstrumentBuilder<B>, IWorldInstrumentDefinitionBuilder<B> {
    override fun importInstrument(instrument: IInstrumentDefinition): B {
        return super<IHistogramInstrumentBuilder>.importInstrument(instrument).also {
            if(instrument is IWorldInstrumentDefinition){
                this.persistent = instrument.persistent
            }
        }
    }
}
