package de.mctelemetry.core.api.instruments.manager.client

import de.mctelemetry.core.api.instruments.definition.IWorldInstrumentDefinition

interface IClientWorldInstrumentDefinition :
        IClientInstrumentManager.IClientInstrumentDefinition,
        IWorldInstrumentDefinition
