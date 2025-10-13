package de.mctelemetry.core.api.metrics.builder

import org.jetbrains.annotations.Contract

interface IWorldGaugeInstrumentBuilder<B : IWorldGaugeInstrumentBuilder<B>> : IGaugeInstrumentBuilder<B> {

    var persistent: Boolean

    @Contract("_ -> this", mutates = "this")
    fun withPersistent(persistent: Boolean = true): IWorldGaugeInstrumentBuilder<B> {
        this.persistent = persistent
        return this
    }
}
