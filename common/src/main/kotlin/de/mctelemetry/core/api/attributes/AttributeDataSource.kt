package de.mctelemetry.core.api.attributes

import de.mctelemetry.core.api.OTelCoreModAPI
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.IdDispatchCodec
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

sealed interface AttributeDataSource<T : Any> {

    val type: IAttributeKeyTypeTemplate<T, *>

    val additionalTypeData: CompoundTag?
        get() = null

    context(_: IMappedAttributeValueLookup)
    val value: T?

    @JvmInline
    value class ObservationSourceAttributeReference<T : Any>(val info: MappedAttributeKeyInfo<T, *>) :
            AttributeDataSource<T> {

        override val type: IAttributeKeyTypeTemplate<T, *>
            get() = info.templateType

        companion object {

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationSourceAttributeReference<*>>
                get() = REFERENCE_STREAM_CODEC
        }

        context(attributeStore: IMappedAttributeValueLookup)
        override val value: T?
            get() = attributeStore[this]

        context(attributeStore: IMappedAttributeValueLookup.Mutable)
        fun set(value: T) {
            attributeStore[this] = value
        }

        context(attributeStore: IMappedAttributeValueLookup.Mutable)
        fun unset() {
            attributeStore[this] = null
        }
    }

    class ConstantAttributeData<T : Any>(
        override val type: IAttributeKeyTypeTemplate<T, *>,
        value: T,
        override val additionalTypeData: CompoundTag? = null,
    ) : AttributeDataSource<T> {

        constructor(info: MappedAttributeKeyInfo<T, *>, value: T) : this(info.templateType, value, info.saveTemplateData())

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

        context(attributeStore: IMappedAttributeValueLookup)
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
                    return ConstantAttributeData(type as IAttributeKeyTypeTemplate<Any, *>, value, additionalData)
                }
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ConstantAttributeData<*>> = StreamCodecImpl
        }
    }

    companion object {


        private val REFERENCE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationSourceAttributeReference<*>> =
            MappedAttributeKeyInfo.STREAM_CODEC.map(
                { ObservationSourceAttributeReference(it) },
                ObservationSourceAttributeReference<*>::info
            )

        fun <T : Any> MappedAttributeKeyInfo<T, *>.asReference(): ObservationSourceAttributeReference<T> =
            ObservationSourceAttributeReference(this)

        fun fromNbt(tag: CompoundTag, lookupProvider: HolderLookup.Provider): AttributeDataSource<*> {
            var typeString = tag.getString("type").lowercase()
            if (typeString.isEmpty()) {
                if (!tag.getCompound("reference").isEmpty)
                    typeString = "reference"
                else if (tag.get("value") != null)
                    typeString = "constant"
            }
            return when (typeString) {
                "reference" -> ObservationSourceAttributeReference(
                    MappedAttributeKeyInfo.load(
                        tag.getCompound("reference"),
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
                        (valueType as IAttributeKeyTypeTemplate<Any, *>),
                        valueType.fromNbt(tag.get("value")!!, lookupProvider),
                        tag.getCompound("type_data").takeUnless(CompoundTag::isEmpty)
                    )
                }
                else -> throw IllegalArgumentException("Unknown AttributeDataSource type $typeString")
            }
        }

        fun toNbt(data: AttributeDataSource<*>, explicitType: Boolean = false): CompoundTag {
            return CompoundTag().also { tag ->
                when (data) {
                    is ObservationSourceAttributeReference<*> -> {
                        if (explicitType) tag.putString("type", "reference")
                        tag.put("reference", data.info.save())
                    }
                    is ConstantAttributeData<*> -> {
                        if (explicitType) tag.putString("type", "constant")
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
                REFERENCE_STREAM_CODEC,
            ).add(
                ConstantAttributeData::class.java,
                ConstantAttributeData.STREAM_CODEC,
            ).build()
    }
}
