package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.instruments.manager.IInstrumentManager
import de.mctelemetry.core.api.instruments.manager.IMutableInstrumentManager
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
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
                attributes = mapOf()
            )
            return ObservationSourceConfiguration(
                instrument,
                mapping,
            )
        }

        fun saveToTag(): CompoundTag {
            return CompoundTag().also { tag ->
                tag.putString("name", instrumentName)
                tag.put("mapping", mapping.saveToTag())
            }
        }

        companion object {

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Template> = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(256),
                Template::instrumentName,
                ObservationAttributeMapping.STREAM_CODEC,
                Template::mapping,
                ::Template
            )

            fun loadFromTag(tag: CompoundTag, holderLookupProvider: HolderLookup.Provider): Template? {
                val name = tag.getString("name")
                if (name.isBlank()) return null
                val mappingTag = tag.get("mapping") ?: return Template(name, ObservationAttributeMapping.empty())
                return Template(
                    instrumentName = name,
                    mapping = ObservationAttributeMapping.loadFromTag(mappingTag, holderLookupProvider),
                )
            }
        }
    }

    fun saveToTag(): CompoundTag {
        return Template(instrument.name, mapping).saveToTag()
    }

    companion object {

        fun loadFromTag(
            tag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
            instrumentManager: IMutableInstrumentManager?,
        ): ObservationSourceConfiguration? {
            if (tag.isEmpty) return null
            return Template.loadFromTag(tag, holderLookupProvider)?.resolveOrMock(instrumentManager)
        }

        fun loadDelayedFromTag(
            tag: CompoundTag,
            holderLookupProvider: HolderLookup.Provider,
        ): (IMutableInstrumentManager?) -> (ObservationSourceConfiguration?) {
            if (tag.isEmpty) return { _ -> null }
            val template = Template.loadFromTag(tag, holderLookupProvider) ?: return { _ -> null }
            return template::resolveOrMock
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationSourceConfiguration> = StreamCodec.composite(
            IInstrumentDefinition.Record.INTERFACE_STREAM_CODEC,
            ObservationSourceConfiguration::instrument,
            ObservationAttributeMapping.STREAM_CODEC,
            ObservationSourceConfiguration::mapping,
            ::ObservationSourceConfiguration
        )
    }
}
