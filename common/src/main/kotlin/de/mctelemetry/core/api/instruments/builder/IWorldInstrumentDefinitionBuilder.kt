package de.mctelemetry.core.api.instruments.builder

import org.jetbrains.annotations.Contract

interface IWorldInstrumentDefinitionBuilder<out B : IWorldInstrumentDefinitionBuilder<B>> :
        IInstrumentDefinitionBuilder<B> {

    var persistent: Boolean

    @Contract("_ -> this", mutates = "this")
    fun withPersistent(persistent: Boolean = true): B {
        this.persistent = persistent
        @Suppress("UNCHECKED_CAST")
        return this as B
    }
}
