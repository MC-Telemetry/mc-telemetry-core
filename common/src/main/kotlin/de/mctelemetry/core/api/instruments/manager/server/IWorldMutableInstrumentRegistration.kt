package de.mctelemetry.core.api.instruments.manager.server

import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.IWorldInstrumentRegistration

interface IWorldMutableInstrumentRegistration<out R : IWorldMutableInstrumentRegistration<R>> :
        IInstrumentRegistration.Mutable<R>,
        IWorldInstrumentRegistration
