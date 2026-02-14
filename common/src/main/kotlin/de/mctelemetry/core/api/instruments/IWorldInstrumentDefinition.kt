package de.mctelemetry.core.api.instruments

import de.mctelemetry.core.api.IMetricDefinition
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.manager.client.IClientWorldInstrumentDefinition
import net.fabricmc.api.EnvType
import net.fabricmc.api.EnvironmentInterface
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

interface IWorldInstrumentDefinition : IInstrumentDefinition {

    val persistent: Boolean


    @JvmRecord
    @EnvironmentInterface(
        value = EnvType.CLIENT,
        itf = IClientWorldInstrumentDefinition::class
    )
    data class Record(
        override val name: String,
        override val description: String = "",
        override val unit: String = "",
        override val attributes: Map<String, MappedAttributeKeyInfo<*, *, *>> = emptyMap(),
        override val supportsFloating: Boolean = false,
        override val persistent: Boolean = false,
    ) : IWorldInstrumentDefinition, IClientWorldInstrumentDefinition {


        init {
            attributes.forEach { (key, value) ->
                require(key == value.baseKey.key) {
                    "Illegal attributes mapping: key ${value.baseKey.key} is stored under $key"
                }
            }
        }

        constructor(
            name: String,
            description: String = "",
            unit: String = "",
            attributes: Collection<MappedAttributeKeyInfo<*, *, *>>,
            supportsFloating: Boolean = false,
            persistent: Boolean = false,
        ) : this(
            name,
            description,
            unit,
            attributes.associateBy { it.baseKey.key },
            supportsFloating,
            persistent,
        )

        companion object {

            operator fun invoke(definition: IWorldInstrumentDefinition): Record {
                if (definition is Record) return definition
                return Record(
                    name = definition.name,
                    description = definition.description,
                    unit = definition.unit,
                    attributes = definition.attributes,
                    supportsFloating = definition.supportsFloating,
                    persistent = definition.persistent,
                )
            }

            operator fun invoke(definition: IInstrumentDefinition, persistent: Boolean): Record {
                if (definition is Record) {
                    if (persistent == definition.persistent) {
                        return definition
                    }
                    return definition.copy(persistent = persistent)
                }
                return Record(
                    name = definition.name,
                    description = definition.description,
                    unit = definition.unit,
                    attributes = definition.attributes,
                    supportsFloating = definition.supportsFloating,
                    persistent = persistent,
                )
            }

            operator fun invoke(
                definition: IMetricDefinition,
                attributes: Collection<MappedAttributeKeyInfo<*, *, *>>,
                supportsFloating: Boolean,
                persistent: Boolean,
            ): Record {
                if (definition is Record) {
                    if (
                        persistent == definition.persistent &&
                        supportsFloating == definition.supportsFloating &&
                        definition.attributes.size == attributes.size &&
                        attributes.all {
                            definition.attributes[it.baseKey.key] == it
                        }
                    ) {
                        return definition
                    }
                }
                return Record(
                    name = definition.name,
                    description = definition.description,
                    unit = definition.unit,
                    attributes = attributes,
                    supportsFloating = supportsFloating,
                    persistent = persistent,
                )
            }

            internal fun encodeInterface(bb: RegistryFriendlyByteBuf, v: IWorldInstrumentDefinition) {
                encodeAsWorldRecord(bb, v)
            }

            internal fun encodeAsWorldRecord(bb: RegistryFriendlyByteBuf, v: IWorldInstrumentDefinition) {
                IInstrumentDefinition.Record.encodeAsSimpleRecord(bb, v)
                bb.writeBoolean(v.persistent)
            }

            internal fun decodeInterface(bb: RegistryFriendlyByteBuf): IWorldInstrumentDefinition {
                return decodeAsWorldRecord(bb)
            }

            internal fun decodeAsWorldRecord(bb: RegistryFriendlyByteBuf): IWorldInstrumentDefinition {
                val baseRecord = IInstrumentDefinition.Record.decodeAsSimpleRecord(bb)
                val persistent = bb.readBoolean()
                return Record(baseRecord, persistent)
            }

            val INTERFACE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IWorldInstrumentDefinition> =
                StreamCodec.of(
                    ::encodeInterface,
                    ::decodeInterface
                )
        }
    }
}
