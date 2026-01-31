package de.mctelemetry.core.instruments.manager

import de.mctelemetry.core.api.instruments.gauge.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.IGameInstrumentManager
import io.opentelemetry.api.metrics.Meter

internal class GameInstrumentManager(
    meter: Meter,
): InstrumentManagerBase.Root<GameInstrumentManager.GameGaugeInstrumentBuilder>(
    meter,
), IGameInstrumentManager {
    internal class GameGaugeInstrumentBuilder(name: String, manager: InstrumentManagerBase<GameGaugeInstrumentBuilder>) :
        GaugeInstrumentBuilder<GameGaugeInstrumentBuilder>(name, manager)

    override fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*> {
        return GameGaugeInstrumentBuilder(name, this)
    }
}
