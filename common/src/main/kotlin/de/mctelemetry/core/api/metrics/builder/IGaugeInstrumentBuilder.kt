package de.mctelemetry.core.api.metrics.builder

import de.mctelemetry.core.api.metrics.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.ILongInstrumentRegistration
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import org.jetbrains.annotations.Contract

interface IGaugeInstrumentBuilder<out B: IGaugeInstrumentBuilder<B>> {

    val name: String
    var attributes: List<AttributeKey<*>>
    var description: String
    var unit: String

    @Contract("_ -> this", mutates = "this")
    fun addAttribute(attributeKey: AttributeKey<*>): B {
        attributes += attributeKey
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun addAttributes(attributeKeys: Collection<AttributeKey<*>>): B {
        attributes += attributeKeys
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun withDescription(description: String): B {
        this.description = description
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun withUnit(unit: String): B {
        this.unit = unit
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    fun registerWithCallbackOfLong(callback: IInstrumentRegistration.Callback<ObservableLongMeasurement>): ILongInstrumentRegistration
    fun registerWithCallbackOfDouble(callback: IInstrumentRegistration.Callback<ObservableDoubleMeasurement>): IDoubleInstrumentRegistration
    fun registerMutableOfLong(): ILongInstrumentRegistration.Mutable
    fun registerMutableOfDouble(): IDoubleInstrumentRegistration.Mutable
}
