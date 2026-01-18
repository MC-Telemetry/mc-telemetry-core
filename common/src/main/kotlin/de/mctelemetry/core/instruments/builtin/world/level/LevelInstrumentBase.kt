package de.mctelemetry.core.instruments.builtin.world.level

import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes
import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration
import de.mctelemetry.core.api.instruments.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.instruments.builtin.world.WorldInstrumentBase
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel

abstract class LevelInstrumentBase<T : IInstrumentRegistration?>(name: String) :
    WorldInstrumentBase<T>(name) {

    protected val levelSlot = NativeAttributeKeyTypes.StringType.createAttributeSlot("dimension")

    context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, registration: T)
    override fun observeWorld(recorder: IObservationRecorder.Unresolved.Sourceless) {
        for (level in server.allLevels) {
            if (!server.isLevelEnabled(level)) continue
            levelSlot.set(level.dimension().location().toString())
            context(level) {
                observeLevel(recorder)
            }
        }
    }

    context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, level: ServerLevel, registration: T)
    abstract fun observeLevel(recorder: IObservationRecorder.Unresolved.Sourceless)

    abstract class Simple(name: String, override val supportsFloating: Boolean) : LevelInstrumentBase<Nothing?>(name) {

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterSimple()

        context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, level: ServerLevel, registration: Nothing?)
        final override fun observeLevel(recorder: IObservationRecorder.Unresolved.Sourceless) {
            observeLevelSimple(recorder)
        }

        context(attributeStore: IAttributeValueStore.Mutable, server: MinecraftServer, level: ServerLevel)
        abstract fun observeLevelSimple(recorder: IObservationRecorder.Unresolved.Sourceless)
    }

    abstract class OfDouble(name: String) : LevelInstrumentBase<IDoubleInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = true

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterDouble()
    }

    abstract class OfLong(name: String) : LevelInstrumentBase<ILongInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = false

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterLong()
    }
}
