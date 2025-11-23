package de.mctelemetry.core.api.instruments.builder

interface IRemoteWorldInstrumentDefinitionBuilder<out B: IRemoteWorldInstrumentDefinitionBuilder<B>> :
        IRemoteInstrumentDefinitionBuilder<B>,
        IWorldInstrumentDefinitionBuilder<B>
