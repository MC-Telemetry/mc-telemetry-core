package de.mctelemetry.core.instruments.manager

import de.mctelemetry.core.api.instruments.manager.IGameInstrumentManager
import io.opentelemetry.api.metrics.Meter

internal class GameInstrumentManager(
    meter: Meter,
): InstrumentManagerBase.Root<InstrumentManagerBase.GaugeInstrumentBuilder<*>>(
    meter,
), IGameInstrumentManager
