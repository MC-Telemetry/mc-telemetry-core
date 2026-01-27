package de.mctelemetry.core.api.instruments.gauge

import de.mctelemetry.core.api.instruments.definition.IWorldInstrumentDefinition

interface IWorldInstrumentRegistration :
        IInstrumentRegistration,
        IWorldInstrumentDefinition
