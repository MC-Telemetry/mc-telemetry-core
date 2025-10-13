package de.mctelemetry.core.metrics.manager

import io.opentelemetry.api.metrics.Meter

internal class GameInstrumentManager(
    meter: Meter,
): InstrumentManagerBase<InstrumentManagerBase.GaugeInstrumentBuilder<*>>(
    meter,
    null,
)
