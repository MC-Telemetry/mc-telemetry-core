package de.mctelemetry.core.api.instruments.builder

interface IWorldGaugeInstrumentBuilder<B : IWorldGaugeInstrumentBuilder<B>> :
        IGaugeInstrumentBuilder<B>,
        IWorldInstrumentDefinitionBuilder<B>
