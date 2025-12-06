package de.mctelemetry.core.api.instruments.manager.client

import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition

interface IClientWorldInstrumentDefinition :
        IClientInstrumentManager.IClientInstrumentDefinition,
        IWorldInstrumentDefinition
