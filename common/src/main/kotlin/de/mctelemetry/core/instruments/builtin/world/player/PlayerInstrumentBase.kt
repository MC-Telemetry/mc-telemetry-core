package de.mctelemetry.core.instruments.builtin.world.player

import de.mctelemetry.core.api.attributes.BuiltinAttributeKeyTypes
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.instruments.IDoubleInstrumentRegistration
import de.mctelemetry.core.api.instruments.IInstrumentRegistration
import de.mctelemetry.core.api.instruments.ILongInstrumentRegistration
import de.mctelemetry.core.api.instruments.builder.IWorldGaugeInstrumentBuilder
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.instruments.builtin.world.WorldInstrumentBase
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

abstract class PlayerInstrumentBase<T : IInstrumentRegistration?>(name: String) :
    WorldInstrumentBase<T>(name) {

    protected val playerSlot = BuiltinAttributeKeyTypes.UUIDType.createAttributeSlot("player.id")

    context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, registration: T)
    override fun observeWorld(recorder: IObservationRecorder.Unresolved.Sourceless) {
        for (player in server.playerList.players) {
            if (!player.allowsListing()) continue
            playerSlot.set(player.uuid)
            context(player) {
                observePlayer(recorder)
            }
        }
    }

    context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer, registration: T)
    abstract fun observePlayer(recorder: IObservationRecorder.Unresolved.Sourceless)

    abstract class Simple(name: String, override val supportsFloating: Boolean) : PlayerInstrumentBase<Nothing?>(name) {

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterSimple()

        context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer, registration: Nothing?)
        final override fun observePlayer(recorder: IObservationRecorder.Unresolved.Sourceless) {
            observePlayerSimple(recorder)
        }

        context(attributeStore: IMappedAttributeValueLookup.Mutable, server: MinecraftServer, player: ServerPlayer)
        abstract fun observePlayerSimple(recorder: IObservationRecorder.Unresolved.Sourceless)
    }

    abstract class OfDouble(name: String) : PlayerInstrumentBase<IDoubleInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = true

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterDouble()
    }

    abstract class OfLong(name: String) : PlayerInstrumentBase<ILongInstrumentRegistration>(name) {

        final override val supportsFloating: Boolean = false

        context(server: MinecraftServer)
        override fun IWorldGaugeInstrumentBuilder<*>.register() = defaultRegisterLong()
    }
}
