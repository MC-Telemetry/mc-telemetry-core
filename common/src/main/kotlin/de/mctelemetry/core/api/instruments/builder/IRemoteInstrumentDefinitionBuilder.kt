package de.mctelemetry.core.api.instruments.builder

interface IRemoteInstrumentDefinitionBuilder<out B : IRemoteInstrumentDefinitionBuilder<B>> :
        IInstrumentDefinitionBuilder<B> {
    fun submit()
}
