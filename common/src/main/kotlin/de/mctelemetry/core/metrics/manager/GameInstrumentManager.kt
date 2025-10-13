package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.api.metrics.managar.IGameInstrumentManager
import io.opentelemetry.api.metrics.Meter

internal class GameInstrumentManager(
    meter: Meter,
): InstrumentManagerBase<InstrumentManagerBase.GaugeInstrumentBuilder<*>>(
    meter,
    null,
), IGameInstrumentManager
