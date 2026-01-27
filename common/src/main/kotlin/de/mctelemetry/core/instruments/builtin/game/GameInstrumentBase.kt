package de.mctelemetry.core.instruments.builtin.game

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.instruments.gauge.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.ILongInstrumentRegistration
import de.mctelemetry.core.api.instruments.gauge.builder.IGaugeInstrumentBuilder
import de.mctelemetry.core.api.instruments.manager.IGameInstrumentManager
import de.mctelemetry.core.api.instruments.manager.gaugeInstrument
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.instruments.builtin.StaticInstrumentBase

abstract class GameInstrumentBase<T : IInstrumentRegistration?>(name: String) :
    IGameInstrumentManager.Events.Ready,
    StaticInstrumentBase(name) {

    override fun gameMetricsManagerReady(manager: IGameInstrumentManager) {
        manager.gaugeInstrument(name) {
            importInstrument(this@GameInstrumentBase)
        }.register()
    }

    protected abstract fun IGaugeInstrumentBuilder<*>.register()

    context(attributeStore: IAttributeValueStore.Mutable, registration: T)
    abstract fun observeGame(recorder: IObservationRecorder.Unresolved.Sourceless)

    abstract class Simple(name: String, override val supportsFloating: Boolean) : GameInstrumentBase<Nothing?>(name) {

        override fun IGaugeInstrumentBuilder<*>.register() {
            if (supportsFloating) {
                registerWithCallbackOfDouble { recorder ->
                    context(null) {
                        withIdentityResolver(recorder) {
                            observeGame(it)
                        }
                    }
                }
            } else {
                registerWithCallbackOfLong { recorder ->
                    context(null) {
                        withIdentityResolver(recorder) {
                            observeGame(it)
                        }
                    }
                }
            }
        }

        context(attributeStore: IAttributeValueStore.Mutable, registration: Nothing?)
        final override fun observeGame(recorder: IObservationRecorder.Unresolved.Sourceless) {
            observeGameSimple(recorder)
        }
        context(attributeStore: IAttributeValueStore.Mutable)
        abstract fun observeGameSimple(recorder: IObservationRecorder.Unresolved.Sourceless)
    }

    abstract class OfDouble(name: String) : GameInstrumentBase<IDoubleInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = true

        override fun IGaugeInstrumentBuilder<*>.register() {
            registerWithCallbackOfDouble { registration, recorder ->
                context(registration) {
                    withIdentityResolver(recorder) {
                        observeGame(it)
                    }
                }
            }
        }
    }

    abstract class OfLong(name: String) : GameInstrumentBase<ILongInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = false

        override fun IGaugeInstrumentBuilder<*>.register() {
            registerWithCallbackOfLong { registration, recorder ->
                context(registration) {
                    withIdentityResolver(recorder) {
                        observeGame(it)
                    }
                }
            }
        }
    }
}
