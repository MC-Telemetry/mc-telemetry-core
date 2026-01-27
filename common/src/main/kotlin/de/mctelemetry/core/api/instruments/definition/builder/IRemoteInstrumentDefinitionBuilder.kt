package de.mctelemetry.core.api.instruments.definition.builder

import org.jetbrains.annotations.Contract

interface IRemoteInstrumentDefinitionBuilder<out B : IRemoteInstrumentDefinitionBuilder<B>> :
        IInstrumentDefinitionBuilder<B> {

    var supportsFloating: Boolean

    @Contract("_ -> this", mutates = "this")
    fun withSupportsFloating(supportsFloating: Boolean = true): B {
        this.supportsFloating = supportsFloating
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    fun sendToServer()
}
