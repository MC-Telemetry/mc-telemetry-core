package de.mctelemetry.core.api.instruments.manager.server

import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.IWorldInstrumentRegistration

interface IWorldMutableInstrumentRegistration<out R : IWorldMutableInstrumentRegistration<R>> :
        IInstrumentRegistration.Mutable<R>,
        IWorldInstrumentRegistration
