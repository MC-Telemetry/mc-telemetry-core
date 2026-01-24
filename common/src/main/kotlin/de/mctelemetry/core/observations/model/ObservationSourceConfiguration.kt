package de.mctelemetry.core.observations.model

import com.mojang.serialization.Codec
import com.mojang.serialization.Encoder
import com.mojang.serialization.codecs.RecordCodecBuilder
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
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

    fun asTemplate(): Template = Template(instrument.name, mapping)

    data class Template(
        val instrumentName: String,
        val mapping: ObservationAttributeMapping,
    ) {

        fun tryResolve(manager: IMutableInstrumentManager?): ObservationSourceConfiguration? {
            val instrument = manager?.findLocalMutable(instrumentName) ?: return null
            return ObservationSourceConfiguration(
                instrument,
                mapping,
            )
        }

        fun resolveOrMock(manager: IMutableInstrumentManager?): ObservationSourceConfiguration {
            val instrument = manager?.findLocalMutable(instrumentName) ?: IInstrumentDefinition.Record(
                name = instrumentName,
                description = "",
                unit = "",
                attributes = mapOf(),
                supportsFloating = true,
            )
            return ObservationSourceConfiguration(
                instrument,
                mapping,
            )
        }

        companion object {

            val CODEC: Codec<Template> = RecordCodecBuilder.create {
                it.group(
                    Codec.string(1, OTelCoreModAPI.Limits.INSTRUMENT_NAME_MAX_LENGTH)
                        .fieldOf("name")
                        .forGetter(Template::instrumentName),
                    ObservationAttributeMapping.CODEC
                        .optionalFieldOf("mapping", ObservationAttributeMapping.empty())
                        .forGetter(Template::mapping)
                ).apply(it, ::Template)
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Template> = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(OTelCoreModAPI.Limits.INSTRUMENT_NAME_MAX_LENGTH),
                Template::instrumentName,
                ObservationAttributeMapping.STREAM_CODEC,
                Template::mapping,
                ::Template
            )
        }
    }

    companion object {

        val ENCODER: Encoder<ObservationSourceConfiguration> =
            Template.CODEC.comap(ObservationSourceConfiguration::asTemplate)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationSourceConfiguration> = StreamCodec.composite(
            IInstrumentDefinition.Record.INTERFACE_STREAM_CODEC,
            ObservationSourceConfiguration::instrument,
            ObservationAttributeMapping.STREAM_CODEC,
            ObservationSourceConfiguration::mapping,
            ::ObservationSourceConfiguration
        )
    }
}
