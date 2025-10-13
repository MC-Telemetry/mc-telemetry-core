package de.mctelemetry.core.api.metrics.managar

import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.IMetricDefinition
import de.mctelemetry.core.api.metrics.builder.IGaugeInstrumentBuilder

interface IInstrumentManager {

    fun findGlobal(pattern: Regex): Sequence<IMetricDefinition>
    fun findGlobal(name: String): IMetricDefinition? {
        return findGlobal(Regex.fromLiteral(name)).firstOrNull()
    }

    fun findLocal(pattern: Regex): Sequence<IInstrumentRegistration>
    fun findLocal(name: String): IInstrumentRegistration? {
        return findLocal(Regex.fromLiteral(name)).firstOrNull()
    }

    fun findLocalMutable(pattern: Regex): Sequence<IInstrumentRegistration.Mutable<*>> {
        return findLocal(pattern).filterIsInstance<IInstrumentRegistration.Mutable<*>>()
    }

    fun findLocalMutable(name: String): IInstrumentRegistration.Mutable<*>? {
        return findLocalMutable(Regex.fromLiteral(name)).firstOrNull()
    }

    fun nameAvailable(name: String): Boolean {
        return findGlobal(name) == null
    }

    fun gaugeInstrument(name: String): IGaugeInstrumentBuilder<*>
}

inline fun IInstrumentManager.gaugeInstrument(
    name: String,
    block: IGaugeInstrumentBuilder<*>.() -> Unit,
): IGaugeInstrumentBuilder<*> = gaugeInstrument(name).apply(block)
