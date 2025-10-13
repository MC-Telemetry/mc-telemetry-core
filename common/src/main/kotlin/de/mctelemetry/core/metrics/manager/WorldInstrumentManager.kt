package de.mctelemetry.core.metrics.manager

import de.mctelemetry.core.api.metrics.IInstrumentRegistration
import de.mctelemetry.core.api.metrics.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.api.metrics.managar.IGameInstrumentManager
import de.mctelemetry.core.api.metrics.managar.IWorldInstrumentManager
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import io.opentelemetry.api.metrics.ObservableMeasurement
import net.minecraft.server.MinecraftServer
import java.lang.AutoCloseable

internal class WorldInstrumentManager(
    meter: Meter,
    override val gameInstruments: IGameInstrumentManager,
    val server: MinecraftServer,
) : InstrumentManagerBase<WorldInstrumentManager.WorldGaugeInstrumentBuilder>(
    meter,
    gameInstruments,
), IWorldInstrumentManager, AutoCloseable {

    fun start() {
        allowRegistration.set(true)
    }

    fun stop() {
        allowRegistration.set(false)
        unregisterAllLocal()
    }

    override fun close() {
        stop()
    }

    override fun gaugeInstrument(name: String): WorldGaugeInstrumentBuilder {
        return WorldGaugeInstrumentBuilder(name, this)
    }

    override fun createImmutableDoubleRegistration(
        builder: WorldGaugeInstrumentBuilder,
        callback: IInstrumentRegistration.Callback<ObservableDoubleMeasurement>,
    ): InstrumentManagerBase<WorldGaugeInstrumentBuilder>.ImmutableGaugeInstrumentRegistration<ObservableDoubleMeasurement> {
        if (builder.persistent) throw IllegalArgumentException("Cannot create persistent immutable instrument registrations")
        return super.createImmutableDoubleRegistration(builder, callback)
    }

    override fun createImmutableLongRegistration(
        builder: WorldGaugeInstrumentBuilder,
        callback: IInstrumentRegistration.Callback<ObservableLongMeasurement>,
    ): InstrumentManagerBase<WorldGaugeInstrumentBuilder>.ImmutableGaugeInstrumentRegistration<ObservableLongMeasurement> {
        if (builder.persistent) throw IllegalArgumentException("Cannot create persistent immutable instrument registrations")
        return super.createImmutableLongRegistration(builder, callback)
    }

    override fun createMutableDoubleRegistration(builder: WorldGaugeInstrumentBuilder): WorldMutableDoubleGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return WorldMutableDoubleGaugeInstrumentRegistration(builder)
    }

    override fun createMutableLongRegistration(builder: WorldGaugeInstrumentBuilder): InstrumentManagerBase<WorldGaugeInstrumentBuilder>.MutableLongGaugeInstrumentRegistration {
        assertAllowsRegistration()
        return WorldMutableLongGaugeInstrumentRegistration(builder)
    }

    internal class WorldGaugeInstrumentBuilder(
        name: String,
        manager: WorldInstrumentManager,
    ) : GaugeInstrumentBuilder<WorldGaugeInstrumentBuilder>(
        name,
        @Suppress("UNCHECKED_CAST")
        (manager as InstrumentManagerBase<GaugeInstrumentBuilder<WorldGaugeInstrumentBuilder>>),
    ), IWorldGaugeInstrumentBuilder<WorldGaugeInstrumentBuilder> {

        override var persistent: Boolean = false
    }

    internal interface IWorldMutableInstrumentRegistration<R : ObservableMeasurement> : IInstrumentRegistration.Mutable<R> {

        val persistent: Boolean
    }

    internal inner class WorldMutableLongGaugeInstrumentRegistration(
        builder: WorldGaugeInstrumentBuilder,
    ) : MutableLongGaugeInstrumentRegistration(
        builder
    ), IWorldMutableInstrumentRegistration<ObservableLongMeasurement> {

        override val persistent = builder.persistent
    }

    internal inner class WorldMutableDoubleGaugeInstrumentRegistration(
        builder: WorldGaugeInstrumentBuilder,
    ) : MutableDoubleGaugeInstrumentRegistration(
        builder
    ), IWorldMutableInstrumentRegistration<ObservableDoubleMeasurement> {

        override val persistent = builder.persistent
    }
}
