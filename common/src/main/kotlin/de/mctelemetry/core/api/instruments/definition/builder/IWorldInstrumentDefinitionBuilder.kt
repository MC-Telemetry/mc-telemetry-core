package de.mctelemetry.core.api.instruments.definition.builder

import de.mctelemetry.core.api.instruments.definition.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.definition.IWorldInstrumentDefinition
import org.jetbrains.annotations.Contract

interface IWorldInstrumentDefinitionBuilder<out B : IWorldInstrumentDefinitionBuilder<B>> :
        IInstrumentDefinitionBuilder<B> {

    var persistent: Boolean

    @Contract("_ -> this", mutates= "this")
    override fun importInstrument(instrument: IInstrumentDefinition): B {
        return super.importInstrument(instrument).also {
            if(instrument is IWorldInstrumentDefinition){
                persistent = instrument.persistent
            }
        }
    }

    @Contract("_ -> this", mutates = "this")
    fun withPersistent(persistent: Boolean = true): B {
        this.persistent = persistent
        @Suppress("UNCHECKED_CAST")
        return this as B
    }
}
