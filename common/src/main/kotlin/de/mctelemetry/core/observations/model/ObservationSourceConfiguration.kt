package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.metrics.IInstrumentDefinition
import de.mctelemetry.core.api.metrics.managar.IInstrumentManager
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

data class ObservationSourceConfiguration(
    val instrument: IInstrumentDefinition,
    val mapping: ObservationAttributeMapping,
) {

    fun validate(): MutableComponent? {
        return mapping.validate(instrument.attributes.values)
    }

    data class Template(
        val instrumentName: String,
        val mapping: ObservationAttributeMapping,
    ) {

        fun tryResolve(manager: IInstrumentManager): ObservationSourceConfiguration? {
            val instrument = manager.findLocalMutable(instrumentName) ?: return null
            return ObservationSourceConfiguration(
                instrument,
                mapping,
            )
        }

        companion object {

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Template> = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(256),
                Template::instrumentName,
                ObservationAttributeMapping.STREAM_CODEC,
                Template::mapping,
                ::Template
            )
        }
    }

    companion object {

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationSourceConfiguration> = StreamCodec.composite(
            IInstrumentDefinition.Record.INTERFACE_STREAM_CODEC,
            ObservationSourceConfiguration::instrument,
            ObservationAttributeMapping.STREAM_CODEC,
            ObservationSourceConfiguration::mapping,
            ::ObservationSourceConfiguration
        )
    }
}
