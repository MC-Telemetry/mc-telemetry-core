package de.mctelemetry.core.api.attributes

import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.AttributeDataSource.Reference.ObservationSourceAttributeReference
import de.mctelemetry.core.api.attributes.AttributeDataSource.Reference.ObservationSourceAttributeReference.Companion.find
import de.mctelemetry.core.api.observations.IObservationSource
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.IdDispatchCodec
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

sealed interface AttributeDataSource<T : Any> {

    val type: IAttributeKeyTypeInstance<T, *>

    val additionalTypeData: CompoundTag?
        get() = null

    context(_: IMappedAttributeValueLookup)
    val value: T?

    sealed interface Reference<T : Any> : AttributeDataSource<T> {

        val info: MappedAttributeKeyInfo<T, *>

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

        class ObservationSourceAttributeReference<T : Any>(
            val source: IObservationSource<*, *>,
            val attributeName: String,
            override val type: IAttributeKeyTypeInstance<T, *>,
        ) : Reference<T> {

            constructor(source: IObservationSource<*, *>, info: MappedAttributeKeyInfo<T, *>) : this(
                source,
                info.baseKey.key,
                info,
            )

            override val info: MappedAttributeKeyInfo<T, *> by lazy {
                if (type is MappedAttributeKeyInfo<T, *> && type.baseKey.key == attributeName) {
                    type
                } else {
                    type.create(attributeName)
                }
            }

            companion object {

                fun find(
                    source: IObservationSource<*, *>,
                    attributeName: String,
                ): ObservationSourceAttributeReference<*> {
                    return source.attributes.findObservationSourceAttributeReference(attributeName)
                        ?: throw NoSuchElementException("Could not find ObservationSourceAttributeReference named $attributeName in $source")
                }

                val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ObservationSourceAttributeReference<*>>
                    get() = OBSERVATION_ATTRIBUTE_REFERENCE_STREAM_CODEC
            }
        }

        @JvmInline
        value class TypedSlot<T : Any>(override val info: MappedAttributeKeyInfo<T, *>) : Reference<T> {

            override val type: IAttributeKeyTypeInstance<T, *>
                get() = info

            companion object {

                val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TypedSlot<*>>
                    get() = TYPED_SLOT_STREAM_CODEC
            }
        }
    }

    class ConstantAttributeData<T : Any>(
        override val type: IAttributeKeyTypeInstance<T, *>,
        value: T,
    ) : AttributeDataSource<T> {

        @Suppress("UNCHECKED_CAST")
        constructor(value: MappedAttributeKeyValue<T, *>) : this(
            value.info as MappedAttributeKeyInfo<T, *>,
            value.value
        )

        private val _value: T = value

        internal val valueTag: Tag
            get() = type.templateType.toNbt(_value)

        private fun writeValue(bb: RegistryFriendlyByteBuf) {
            type.templateType.valueStreamCodec.encode(bb, _value)
        }

        context(_: IMappedAttributeValueLookup)
        override val value: T
            get() = _value

        val value: T
            get() = _value

        companion object {
            private object StreamCodecImpl : StreamCodec<RegistryFriendlyByteBuf, ConstantAttributeData<*>> {

                override fun encode(`object`: RegistryFriendlyByteBuf, object2: ConstantAttributeData<*>) {
                    `object`.writeResourceKey(object2.type.templateType.id)
                    `object`.writeNbt(object2.additionalTypeData)
                    object2.writeValue(`object`)
                }

                override fun decode(`object`: RegistryFriendlyByteBuf): ConstantAttributeData<*> {
                    val key = `object`.readResourceKey(OTelCoreModAPI.AttributeTypeMappings)
                    val additionalData = `object`.readNbt()
                    val type: IAttributeKeyTypeTemplate<*, *> = `object`.registryAccess()
                        .registryOrThrow(OTelCoreModAPI.AttributeTypeMappings)
                        .getOrThrow(key)
                    val typeInstance: IAttributeKeyTypeInstance<*, *> = type.create(additionalData)
                    val value = type.valueStreamCodec.decode(`object`)
                    @Suppress("UNCHECKED_CAST") // type information is retained in `type`
                    return ConstantAttributeData(typeInstance as IAttributeKeyTypeInstance<Any, *>, value)
                }
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ConstantAttributeData<*>> = StreamCodecImpl
        }
    }

    companion object {


        private val TYPED_SLOT_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Reference.TypedSlot<*>> =
            MappedAttributeKeyInfo.STREAM_CODEC.map(
                { Reference.TypedSlot(it) },
                Reference.TypedSlot<*>::info
            )

        private val OBSERVATION_ATTRIBUTE_REFERENCE_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Reference.ObservationSourceAttributeReference<*>> =
            StreamCodec.composite(
                ByteBufCodecs.registry(OTelCoreModAPI.ObservationSources),
                ObservationSourceAttributeReference<*>::source,
                ByteBufCodecs.STRING_UTF8,
                ObservationSourceAttributeReference<*>::attributeName,
                ::find
            )


        fun <T : Any> MappedAttributeKeyInfo<T, *>.asAttributeDataSlot(): Reference.TypedSlot<T> =
            Reference.TypedSlot(this)

        fun <T : Any> MappedAttributeKeyInfo<T, *>.asObservationDataReference(source: IObservationSource<*, *>): Reference.ObservationSourceAttributeReference<T> =
            Reference.ObservationSourceAttributeReference(
                source,
                this,
            )

        fun fromNbt(
            tag: CompoundTag,
            lookupProvider: HolderLookup.Provider,
            sourceContext: IObservationSource<*, *>,
        ): AttributeDataSource<*> {
            var typeString = tag.getString("type").lowercase()
            if (typeString.isEmpty()) {
                if (!tag.getCompound("slot").isEmpty)
                    typeString = "slot"
                else if (!tag.getString("reference").isEmpty())
                    typeString = "reference"
                else if (tag.get("value") != null)
                    typeString = "constant"
            }
            return when (typeString) {
                "reference" -> Reference.ObservationSourceAttributeReference.find(
                    sourceContext,
                    tag.getString("reference").also {
                        require(it.isNotEmpty()) { "Reference name must not be empty" }
                    }
                )

                "slot" -> Reference.TypedSlot(
                    MappedAttributeKeyInfo.load(
                        tag.getCompound("reference"),
                        lookupProvider.lookupOrThrow(
                            OTelCoreModAPI.AttributeTypeMappings
                        )
                    ),
                )

                "constant" -> {
                    val valueTypeResourceLocation = ResourceLocation.parse(tag.getString("value_type"))
                    val valueType: IAttributeKeyTypeTemplate<*, *> =
                        lookupProvider.lookupOrThrow(OTelCoreModAPI.AttributeTypeMappings).getOrThrow(
                            ResourceKey.create(OTelCoreModAPI.AttributeTypeMappings, valueTypeResourceLocation)
                        ).value()
                    ConstantAttributeData(
                        @Suppress("UNCHECKED_CAST")
                        (valueType as IAttributeKeyTypeTemplate<Any, *>).create(
                            tag.getCompound("type_data").takeUnless(CompoundTag::isEmpty)
                        ),
                        valueType.fromNbt(tag.get("value")!!, lookupProvider),
                    )
                }

                "" -> throw IllegalArgumentException("No AttributeDataSource type found")
                else -> throw IllegalArgumentException("Unknown AttributeDataSource type \"$typeString\"")
            }
        }

        fun toNbt(data: AttributeDataSource<*>, explicitType: Boolean = false): CompoundTag {
            return CompoundTag().also { tag ->
                when (data) {
                    is Reference.ObservationSourceAttributeReference -> {
                        if (explicitType) tag.putString("type", "reference")
                        tag.putString("reference", data.attributeName)
                    }

                    is Reference.TypedSlot<*> -> {
                        if (explicitType) tag.putString("type", "slot")
                        tag.put("slot", data.info.save())
                    }

                    is ConstantAttributeData<*> -> {
                        if (explicitType) tag.putString("type", "constant")
                        tag.putString("value_type", data.type.templateType.id.location().toString())
                        tag.put("value", data.valueTag)
                        val additionalTypeData = data.additionalTypeData
                        if (additionalTypeData != null) {
                            tag.put("type_data", additionalTypeData)
                        }
                    }
                }
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, AttributeDataSource<*>> =
            IdDispatchCodec.builder<RegistryFriendlyByteBuf, AttributeDataSource<*>, Class<*>> {
                it::class.java
            }.add(
                Reference.TypedSlot::class.java,
                TYPED_SLOT_STREAM_CODEC,
            ).add(
                ObservationSourceAttributeReference::class.java,
                OBSERVATION_ATTRIBUTE_REFERENCE_STREAM_CODEC,
            ).add(
                ConstantAttributeData::class.java,
                ConstantAttributeData.STREAM_CODEC,
            ).build()
    }
}
