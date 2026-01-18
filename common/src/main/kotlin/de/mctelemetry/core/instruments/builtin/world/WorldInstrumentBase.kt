package de.mctelemetry.core.instruments.builtin.world

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration
import de.mctelemetry.core.api.instruments.IWorldInstrumentDefinition
import de.mctelemetry.core.api.instruments.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.server.IServerWorldInstrumentManager
import de.mctelemetry.core.api.instruments.manager.server.gaugeWorldInstrument
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.instruments.builtin.StaticInstrumentBase
import net.minecraft.server.MinecraftServer

abstract class WorldInstrumentBase<T : IInstrumentRegistration?>(name: String) :
    IServerWorldInstrumentManager.Events.Loading,
    StaticInstrumentBase(name),
    IWorldInstrumentDefinition {

    final override val persistent: Boolean = false

    override fun worldInstrumentManagerLoading(manager: IServerWorldInstrumentManager, server: MinecraftServer) {
        manager.gaugeWorldInstrument(name) {
            importWorldInstrument(this@WorldInstrumentBase)
        }.apply {
            context(server) {
                register()
            }
        }
    }

    context(server: MinecraftServer)
    protected abstract fun IWorldGaugeInstrumentBuilder<*>.register()

    context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, registration: T)
    abstract fun observeWorld(recorder: IObservationRecorder.Unresolved.Sourceless)

    abstract class Simple(name: String, override val supportsFloating: Boolean) : WorldInstrumentBase<Nothing?>(name) {

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterSimple()

        context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, registration: Nothing?)
        final override fun observeWorld(recorder: IObservationRecorder.Unresolved.Sourceless) {
            observeWorldSimple(recorder)
        }

        context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer)
        abstract fun observeWorldSimple(recorder: IObservationRecorder.Unresolved.Sourceless)
    }

    abstract class OfDouble(name: String) : WorldInstrumentBase<IDoubleInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = true

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterDouble()
    }

    abstract class OfLong(name: String) : WorldInstrumentBase<ILongInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = false

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterLong()
    }

    companion object {
        context(server: MinecraftServer, instrument: WorldInstrumentBase<Nothing?>)
        fun IWorldGaugeInstrumentBuilder<*>.defaultRegisterSimple() {
            if (instrument.supportsFloating) {
                registerWithCallbackOfDouble { recorder ->
                    context(null) {
                        instrument.withIdentityResolver(recorder) {
                            instrument.observeWorld(it)
                        }
                    }
                }
            } else {
                registerWithCallbackOfLong { recorder ->
                    context(null) {
                        instrument.withIdentityResolver(recorder) {
                            instrument.observeWorld(it)
                        }
                    }
                }
            }
        }

        context(server: MinecraftServer, instrument: WorldInstrumentBase<IDoubleInstrumentRegistration>)
        fun IWorldGaugeInstrumentBuilder<*>.defaultRegisterDouble() {
            registerWithCallbackOfDouble { registration, recorder ->
                context(registration) {
                    instrument.withIdentityResolver(recorder) {
                        instrument.observeWorld(it)
                    }
                }
            }
        }

        context(server: MinecraftServer, instrument: WorldInstrumentBase<ILongInstrumentRegistration>)
        fun IWorldGaugeInstrumentBuilder<*>.defaultRegisterLong() {
            registerWithCallbackOfLong { registration, recorder ->
                context(registration) {
                    instrument.withIdentityResolver(recorder) {
                        instrument.observeWorld(it)
                    }
                }
            }
        }
    }
}
