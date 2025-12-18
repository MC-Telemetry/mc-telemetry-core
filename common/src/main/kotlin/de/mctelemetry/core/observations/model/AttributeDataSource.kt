package de.mctelemetry.core.observations.model

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.ObservationContext
import de.mctelemetry.core.api.attributes.IMappedAttributeKeyType
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.IdDispatchCodec
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

sealed interface AttributeDataSource<T : Any> {

    val type: IMappedAttributeKeyType<T, *>
    context(observationContext: ObservationContext<*>)
    val value: T?

    @JvmInline
    value class ObservationSourceAttributeReference<T : Any>(val info: MappedAttributeKeyInfo<T, *>) :
            AttributeDataSource<T> {

        override val type: IMappedAttributeKeyType<T, *>
            get() = info.type

        companion object {

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationSourceAttributeReference<*>> =
                MappedAttributeKeyInfo.STREAM_CODEC.map(
                    { ObservationSourceAttributeReference(it) },
                    ObservationSourceAttributeReference<*>::info
                )
        }

        context(observationContext: ObservationContext<*>)
        override val value: T?
            get() = observationContext.attributeValueLookup[info]
    }

    class ConstantAttributeData<T : Any>(
        override val type: IMappedAttributeKeyType<T, *>,
        value: T,
        val additionalTypeData: CompoundTag? = null,
    ) : AttributeDataSource<T> {

        constructor(info: MappedAttributeKeyInfo<T, *>, value: T) : this(info.type, value, info.saveAdditional())

        @Suppress("UNCHECKED_CAST")
        constructor(value: MappedAttributeKeyValue<T, *>) : this(
            value.info as MappedAttributeKeyInfo<T, *>,
            value.value
        )

        private val _value: T = value

        internal val valueTag: Tag
            get() = type.toNbt(_value)

        private fun writeValue(bb: RegistryFriendlyByteBuf) {
            type.valueStreamCodec.encode(bb, _value)
        }

        context(observationContext: ObservationContext<*>)
        override val value: T
            get() = _value

        companion object {
            private object StreamCodecImpl : StreamCodec<RegistryFriendlyByteBuf, ConstantAttributeData<*>> {

                override fun encode(`object`: RegistryFriendlyByteBuf, object2: ConstantAttributeData<*>) {
                    `object`.writeResourceKey(object2.type.id)
                    `object`.writeNbt(object2.additionalTypeData)
                    object2.writeValue(`object`)
                }

                override fun decode(`object`: RegistryFriendlyByteBuf): ConstantAttributeData<*> {
                    val key = `object`.readResourceKey(OTelCoreModAPI.AttributeTypeMappings)
                    val additionalData = `object`.readNbt()
                    val type = `object`.registryAccess()
                        .registryOrThrow(OTelCoreModAPI.AttributeTypeMappings)
                        .getOrThrow(key)
                    val value = type.valueStreamCodec.decode(`object`)
                    @Suppress("UNCHECKED_CAST") // type information is retained in `type`
                    return ConstantAttributeData(type as IMappedAttributeKeyType<Any, *>, value, additionalData)
                }
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ConstantAttributeData<*>> = StreamCodecImpl
        }
    }

    companion object {

        fun fromNbt(tag: CompoundTag, lookupProvider: HolderLookup.Provider): AttributeDataSource<*> {
            return when (val typeString = tag.getString("type").lowercase()) {
                "reference" -> ObservationSourceAttributeReference(
                    MappedAttributeKeyInfo.load(
                        tag.getCompound("data"),
                        lookupProvider.lookupOrThrow(
                            OTelCoreModAPI.AttributeTypeMappings
                        )
                    ),
                )
                "constant" -> {
                    val valueTypeResourceLocation = ResourceLocation.parse(tag.getString("value_type"))
                    val valueType = lookupProvider.lookupOrThrow(OTelCoreModAPI.AttributeTypeMappings).getOrThrow(
                        ResourceKey.create(OTelCoreModAPI.AttributeTypeMappings, valueTypeResourceLocation)
                    )
                    ConstantAttributeData(
                        @Suppress("UNCHECKED_CAST")
                        (valueType as IMappedAttributeKeyType<Any, *>),
                        valueType.fromNbt(tag.get("value")!!, lookupProvider),
                        tag.getCompound("type_data").takeUnless(CompoundTag::isEmpty)
                    )
                }
                else -> throw IllegalArgumentException("Unknown AttributeDataSource type $typeString")
            }
        }

        fun toNbt(data: AttributeDataSource<*>): CompoundTag {
            return CompoundTag().also { tag ->
                when (data) {
                    is ObservationSourceAttributeReference<*> -> {
                        tag.putString("type", "reference")
                        tag.put("reference", data.info.save())
                    }
                    is ConstantAttributeData<*> -> {
                        tag.putString("type", "constant")
                        tag.putString("value_type", data.type.id.location().toString())
                        tag.put("value", data.valueTag)
                        if (data.additionalTypeData != null) {
                            tag.put("type_data", data.additionalTypeData)
                        }
                    }
                }
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, AttributeDataSource<*>> =
            IdDispatchCodec.builder<RegistryFriendlyByteBuf, AttributeDataSource<*>, Class<*>> {
                it::class.java
            }.add(
                ObservationSourceAttributeReference::class.java,
                ObservationSourceAttributeReference.STREAM_CODEC,
            ).add(
                ConstantAttributeData::class.java,
                ConstantAttributeData.STREAM_CODEC,
            ).build()
    }
}
