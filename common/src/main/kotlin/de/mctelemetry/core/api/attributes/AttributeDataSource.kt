package de.mctelemetry.core.api.attributes

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.observations.IObservationSource
import de.mctelemetry.core.persistence.SerializationAttributes
import de.mctelemetry.core.utils.addErrorTo
import de.mctelemetry.core.utils.asStringDynamic
import de.mctelemetry.core.utils.get
import de.mctelemetry.core.utils.getAttributeFromContext
import de.mctelemetry.core.utils.getParsedValue
import de.mctelemetry.core.utils.getStringValue
import de.mctelemetry.core.utils.mergeErrorMessages
import de.mctelemetry.core.utils.resultOrElse
import de.mctelemetry.core.utils.resultOrNull
import de.mctelemetry.core.utils.resultOrPartialOrElse
import de.mctelemetry.core.utils.withEncodedEntry
import de.mctelemetry.core.utils.withEntry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.IdDispatchCodec
import net.minecraft.network.codec.StreamCodec
import java.util.function.Supplier

sealed interface AttributeDataSource<T : Any> {

    val type: IAttributeKeyTypeInstance<T, *, *>

    context(_: IAttributeValueStore)
    val value: T?

    sealed interface Reference<T : Any> : AttributeDataSource<T> {

        val info: MappedAttributeKeyInfo<T, *, *>

        context(attributeStore: IAttributeValueStore)
        override val value: T?
            get() = attributeStore[this]

        context(attributeStore: IAttributeValueStore.Mutable)
        fun set(value: T) {
            attributeStore[this] = value
        }

        context(attributeStore: IAttributeValueStore.Mutable)
        fun unset() {
            attributeStore[this] = null
        }

        class ObservationSourceAttributeReference<T : Any> private constructor(
            val source: IObservationSource<*, *>,
            val attributeName: String,
            override val type: IAttributeKeyTypeInstance<T, *, *>,
            infoLazy: Lazy<MappedAttributeKeyInfo<T, *, *>>,
        ) : Reference<T> {

            constructor(
                source: IObservationSource<*, *>,
                attributeName: String,
                type: IAttributeKeyTypeInstance<T, *, *>
            ) : this(
                source,
                attributeName,
                type,
                lazy { type.create(attributeName) }
            )

            constructor(source: IObservationSource<*, *>, info: MappedAttributeKeyInfo<T, *, *>) : this(
                source,
                info.baseKey.key,
                info.typeInstance,
                lazy { info },
            )

            override val info: MappedAttributeKeyInfo<T, *, *> by infoLazy

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
        value class TypedSlot<T : Any>(override val info: MappedAttributeKeyInfo<T, *, *>) : Reference<T> {

            override val type: IAttributeKeyTypeInstance<T, *, *>
                get() = info.typeInstance

            companion object {

                val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TypedSlot<*>>
                    get() = TYPED_SLOT_STREAM_CODEC
            }
        }
    }

    class ConstantAttributeData<T : Any>(
        override val type: IAttributeKeyTypeInstance<T, *, *>,
        value: T,
    ) : AttributeDataSource<T> {

        @Suppress("UNCHECKED_CAST")
        constructor(value: MappedAttributeKeyValue<T, *>) : this(
            (value.info as MappedAttributeKeyInfo<T, *, *>).typeInstance,
            value.value
        )

        private val _value: T = value

        private fun writeValue(bb: RegistryFriendlyByteBuf) {
            type.templateType.valueStreamCodec.encode(bb, _value)
        }

        context(ops: DynamicOps<T>)
        internal fun <T> encode(prefix: T = ops.empty()): DataResult<T> {
            return type.templateType.valueCodec.encode(_value, ops, prefix)
        }

        context(_: IAttributeValueStore)
        override val value: T
            get() = _value

        val value: T
            get() = _value

        companion object {
            private object StreamCodecImpl : StreamCodec<RegistryFriendlyByteBuf, ConstantAttributeData<*>> {

                override fun encode(`object`: RegistryFriendlyByteBuf, object2: ConstantAttributeData<*>) {
                    `object`.writeResourceKey(object2.type.templateType.id)
                    object2.type.encodeInstanceDetails(`object`)
                    object2.writeValue(`object`)
                }

                override fun decode(`object`: RegistryFriendlyByteBuf): ConstantAttributeData<*> {
                    val key = `object`.readResourceKey(OTelCoreModAPI.AttributeTypeMappings)
                    val type: IAttributeKeyTypeTemplate<*, *, *> = `object`.registryAccess()
                        .registryOrThrow(OTelCoreModAPI.AttributeTypeMappings)
                        .getOrThrow(key)
                    val typeInstance: IAttributeKeyTypeInstance<*, *, *> = type.typeInstanceStreamCodec.decode(`object`)
                    val value = type.valueStreamCodec.decode(`object`)
                    @Suppress("UNCHECKED_CAST") // type information is retained in `type`
                    return ConstantAttributeData(typeInstance as IAttributeKeyTypeInstance<Any, *, *>, value)
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
                Reference.ObservationSourceAttributeReference<*>::source,
                ByteBufCodecs.STRING_UTF8,
                Reference.ObservationSourceAttributeReference<*>::attributeName,
            ) { source, name ->
                Reference.ObservationSourceAttributeReference.find(source, name)
            }


        fun <T : Any> MappedAttributeKeyInfo<T, *, *>.asAttributeDataSlot() =
            Reference.TypedSlot(this)

        fun <T : Any> MappedAttributeKeyInfo<T, *, *>.asObservationDataReference(source: IObservationSource<*, *>) =
            Reference.ObservationSourceAttributeReference(
                source,
                this,
            )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, AttributeDataSource<*>> =
            IdDispatchCodec.builder<RegistryFriendlyByteBuf, AttributeDataSource<*>, Class<*>> {
                it::class.java
            }.add(
                Reference.TypedSlot::class.java,
                TYPED_SLOT_STREAM_CODEC,
            ).add(
                Reference.ObservationSourceAttributeReference::class.java,
                OBSERVATION_ATTRIBUTE_REFERENCE_STREAM_CODEC,
            ).add(
                ConstantAttributeData::class.java,
                ConstantAttributeData.STREAM_CODEC,
            ).build()

        val CODEC: Codec<AttributeDataSource<*>> = object : Codec<AttributeDataSource<*>> {
            override fun <T> encode(
                input: AttributeDataSource<*>,
                ops: DynamicOps<T>,
                prefix: T
            ): DataResult<T> {
                var result = prefix
                context(ops) {
                    return when (input) {
                        is Reference.ObservationSourceAttributeReference -> {
                            result = result.withEntry("type", "reference".asStringDynamic())
                            result = result.withEntry("reference", input.attributeName.asStringDynamic())
                            DataResult.success(result)
                        }

                        is Reference.TypedSlot<*> -> {
                            result = result.withEntry("type", "slot".asStringDynamic())
                            result.withEncodedEntry("slot", input.info, MappedAttributeKeyInfo.CODEC)
                        }

                        is ConstantAttributeData<*> -> {
                            var errors: MutableList<Supplier<String>>? = null
                            result = result.withEntry("type", "constant".asStringDynamic())
                            result = result.withEncodedEntry(
                                "value_type",
                                input.type.templateType,
                                IAttributeKeyTypeTemplate.CODEC
                            ).resultOrPartialOrElse(
                                onError = { errors = it.addErrorTo(errors) },
                                fallback = { return DataResult.error(it.messageSupplier) }
                            )
                            result = result.withEntry(
                                "type_data",
                                input.type.encodeInstanceDetails(ops.empty())
                                    .resultOrPartialOrElse(
                                        onError = { errors = it.addErrorTo(errors) },
                                        fallback = { return DataResult.error(mergeErrorMessages(errors!!)) }
                                    ),
                                allowEmpty = false,
                            )
                            result = result.withEntry(
                                "value",
                                input.encode()
                                    .resultOrPartialOrElse(
                                        onError = { errors = it.addErrorTo(errors) },
                                        fallback = { return DataResult.error(mergeErrorMessages(errors!!)) }
                                    ),
                                allowEmpty = false,
                            )
                            if (errors != null) {
                                DataResult.error(mergeErrorMessages(errors), result)
                            } else {
                                DataResult.success(result)
                            }
                        }
                    }
                }
            }

            override fun <T> decode(
                ops: DynamicOps<T>,
                input: T
            ): DataResult<Pair<AttributeDataSource<*>, T>> {
                context(ops) {
                    var typeString =
                        input.getStringValue("type").resultOrNull()?.lowercase()?.takeUnless { it.isBlank() }

                    if (typeString == null) {
                        typeString = if (input["slot"].isSuccess)
                            "slot"
                        else if (input["reference"].isSuccess)
                            "reference"
                        else if (input["value"].isSuccess)
                            "constant"
                        else
                            return DataResult.error { "No AttributeDataSource type found" }
                    }

                    return when (typeString) {
                        "reference" -> Reference.ObservationSourceAttributeReference.find(
                            ops.getAttributeFromContext(
                                SerializationAttributes.ObservationSourceSerializationAttribute,
                                default = {
                                    return DataResult.error { "No ObservationSource provided in deserialization context" }
                                }
                            ),
                            input.getStringValue("reference").resultOrElse {
                                return DataResult.error { "No reference field found for AttributeDataSource reference" }
                            }.also {
                                if (it.isEmpty()) return DataResult.error { "Reference name must not be empty" }
                            }
                        ).let {
                            DataResult.success(Pair(it, input))
                        }

                        "slot" -> {
                            input.getParsedValue("reference", MappedAttributeKeyInfo.CODEC).map {
                                Pair(Reference.TypedSlot(it), input)
                            }
                        }

                        "constant" -> {
                            var errors: MutableList<Supplier<String>>? = null
                            val valueTypeTemplate: IAttributeKeyTypeTemplate<*, *, *> = input.getParsedValue(
                                "value_type",
                                IAttributeKeyTypeTemplate.CODEC
                            ).resultOrPartialOrElse(
                                onError = { errors = it.addErrorTo(errors) },
                                fallback = { return DataResult.error(it.messageSupplier) }
                            )
                            val typeDataResult = input["type_data"].resultOrElse { ops.empty() }
                            val valueTypeInstance: IAttributeKeyTypeInstance<*, *, *> =
                                valueTypeTemplate.typeInstanceCodec.parse(ops, typeDataResult).resultOrPartialOrElse(
                                    onError = { errors = it.addErrorTo(errors) },
                                    fallback = { return DataResult.error(mergeErrorMessages(errors!!)) }
                                )

                            val result = createConstantFromType("value", valueTypeInstance, input)
                                .resultOrPartialOrElse(
                                    onError = { errors = it.addErrorTo(errors) },
                                    fallback = { return DataResult.error(mergeErrorMessages(errors!!)) }
                                )
                            if (errors == null)
                                DataResult.success(Pair(result, input))
                            else
                                DataResult.error(mergeErrorMessages(errors), Pair(result, input))
                        }

                        else -> throw IllegalArgumentException("Unknown AttributeDataSource type \"$typeString\"")
                    }
                }
            }

            context(ops: DynamicOps<A>)
            private fun <T:Any,A> createConstantFromType(field: String, typeInstance: IAttributeKeyTypeInstance<T,*,*>, input: A): DataResult<ConstantAttributeData<T>>{
                return input.getParsedValue(field, typeInstance.templateType.valueCodec).map {
                    ConstantAttributeData(typeInstance, it)
                }
            }
        }

    }
}
