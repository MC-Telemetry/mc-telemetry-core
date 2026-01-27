package de.mctelemetry.core.api.instruments.gauge.builder

import de.mctelemetry.core.api.instruments.definition.builder.IWorldInstrumentDefinitionBuilder

interface IWorldGaugeInstrumentBuilder<B : IWorldGaugeInstrumentBuilder<B>> :
        IGaugeInstrumentBuilder<B>,
        IWorldInstrumentDefinitionBuilder<B>
