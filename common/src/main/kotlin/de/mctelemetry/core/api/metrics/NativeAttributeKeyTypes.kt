package de.mctelemetry.core.api.metrics

import io.netty.buffer.ByteBuf
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

object NativeAttributeKeyTypes {

    val NATIVE_ATTRIBUTE_TYPE_STREAM_CODED: StreamCodec<ByteBuf, AttributeType> = StreamCodec.of(
        { bb, v ->
            bb.writeByte(v.ordinal)
        },
        { bb ->
            AttributeType.entries[bb.readByte().toInt()]
        }
    )
    val NATIVE_ATTRIBUTE_KEY_STREAM_CODEC: StreamCodec<ByteBuf, AttributeKey<*>> = StreamCodec.composite(
        NATIVE_ATTRIBUTE_TYPE_STREAM_CODED,
        AttributeKey<*>::getType,
        ByteBufCodecs.stringUtf8(OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_NAME_MAX_LENGTH),
        AttributeKey<*>::getKey,
        ::attributeKeyForType,
    )

    val ALL: List<IMappedAttributeKeyType<*, *>> = listOf(
        StringType,
        BooleanType,
        LongType,
        DoubleType,
        StringArrayType,
        BooleanArrayType,
        LongArrayType,
        DoubleArrayType,
    )

    fun attributeKeyForType(keyType: AttributeType, name: String): AttributeKey<*> {
        return when (keyType) {
            AttributeType.STRING -> AttributeKey.stringKey(name)
            AttributeType.BOOLEAN -> AttributeKey.booleanKey(name)
            AttributeType.LONG -> AttributeKey.longKey(name)
            AttributeType.DOUBLE -> AttributeKey.doubleKey(name)
            AttributeType.STRING_ARRAY -> AttributeKey.stringArrayKey(name)
            AttributeType.BOOLEAN_ARRAY -> AttributeKey.booleanArrayKey(name)
            AttributeType.LONG_ARRAY -> AttributeKey.longArrayKey(name)
            AttributeType.DOUBLE_ARRAY -> AttributeKey.doubleArrayKey(name)
        }
    }

    fun <T : Any> attributeKeyForType(keyType: GenericAttributeType<T>, name: String): AttributeKey<T> {
        @Suppress("UNCHECKED_CAST") // known generic values
        return when (keyType) {
            GenericAttributeType.STRING -> AttributeKey.stringKey(name)
            GenericAttributeType.BOOLEAN -> AttributeKey.booleanKey(name)
            GenericAttributeType.LONG -> AttributeKey.longKey(name)
            GenericAttributeType.DOUBLE -> AttributeKey.doubleKey(name)
            GenericAttributeType.STRING_ARRAY -> AttributeKey.stringArrayKey(name)
            GenericAttributeType.BOOLEAN_ARRAY -> AttributeKey.booleanArrayKey(name)
            GenericAttributeType.LONG_ARRAY -> AttributeKey.longArrayKey(name)
            GenericAttributeType.DOUBLE_ARRAY -> AttributeKey.doubleArrayKey(name)
        } as AttributeKey<T>
    }

    operator fun get(keyType: AttributeType): IMappedAttributeKeyType<*, *> {
        return when (keyType) {
            AttributeType.STRING -> StringType
            AttributeType.BOOLEAN -> BooleanType
            AttributeType.LONG -> LongType
            AttributeType.DOUBLE -> DoubleType
            AttributeType.STRING_ARRAY -> StringArrayType
            AttributeType.BOOLEAN_ARRAY -> BooleanArrayType
            AttributeType.LONG_ARRAY -> LongArrayType
            AttributeType.DOUBLE_ARRAY -> DoubleArrayType
        }
    }

    operator fun <T : Any> get(attributeType: GenericAttributeType<T>): IMappedAttributeKeyType<T, T> =
        attributeType.mappedType

    operator fun <T : Any> get(attributeKey: AttributeKey<T>): IMappedAttributeKeyType<T, T> {
        @Suppress("UNCHECKED_CAST")
        return when (attributeKey.type) {
            AttributeType.STRING -> StringType
            AttributeType.BOOLEAN -> BooleanType
            AttributeType.LONG -> LongType
            AttributeType.DOUBLE -> DoubleType
            AttributeType.STRING_ARRAY -> StringArrayType
            AttributeType.BOOLEAN_ARRAY -> BooleanArrayType
            AttributeType.LONG_ARRAY -> LongArrayType
            AttributeType.DOUBLE_ARRAY -> DoubleArrayType
        } as IMappedAttributeKeyType<T, T>
    }

    object StringType : IMappedAttributeKeyType<String, String> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "string")
        )

        override val baseType: GenericAttributeType<String> = GenericAttributeType.STRING
        override val valueType: Class<String> = String::class.java

        override fun canConvertDirectlyFrom(subtype: IMappedAttributeKeyType<*, *>): Boolean {
            return true
        }

        override fun <R : Any> convertDirectlyFrom(subtype: IMappedAttributeKeyType<R, *>, value: R): String {
            return value.toString()
        }

        override fun format(value: String): String = value
    }

    object BooleanType : IMappedAttributeKeyType<Boolean, Boolean> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "boolean")
        )

        override val baseType: GenericAttributeType<Boolean> = GenericAttributeType.BOOLEAN
        override val valueType: Class<Boolean> = Boolean::class.java

        override fun format(value: Boolean): Boolean = value
    }

    object LongType : IMappedAttributeKeyType<Long, Long> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "long")
        )

        override val baseType: GenericAttributeType<Long> = GenericAttributeType.LONG
        override val valueType: Class<Long> = Long::class.java

        override fun format(value: Long): Long = value
    }

    object DoubleType : IMappedAttributeKeyType<Double, Double> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "double")
        )

        override val baseType: GenericAttributeType<Double> = GenericAttributeType.DOUBLE
        override val valueType: Class<Double> = Double::class.java

        override fun format(value: Double): Double = value
    }

    object StringArrayType : IMappedAttributeKeyType<List<String>, List<String>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "string_array")
        )

        override val baseType: GenericAttributeType<List<String>> = GenericAttributeType.STRING_ARRAY

        @Suppress("UNCHECKED_CAST")
        override val valueType: Class<List<String>> = List::class.java as Class<List<String>>

        override fun format(value: List<String>): List<String> = value
    }

    object BooleanArrayType : IMappedAttributeKeyType<List<Boolean>, List<Boolean>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "boolean_array")
        )

        override val baseType: GenericAttributeType<List<Boolean>> = GenericAttributeType.BOOLEAN_ARRAY

        @Suppress("UNCHECKED_CAST")
        override val valueType: Class<List<Boolean>> = List::class.java as Class<List<Boolean>>

        override fun format(value: List<Boolean>): List<Boolean> = value
    }

    object LongArrayType : IMappedAttributeKeyType<List<Long>, List<Long>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "long_array")
        )

        override val baseType: GenericAttributeType<List<Long>> = GenericAttributeType.LONG_ARRAY

        @Suppress("UNCHECKED_CAST")
        override val valueType: Class<List<Long>> = List::class.java as Class<List<Long>>

        override fun format(value: List<Long>): List<Long> = value
    }

    object DoubleArrayType : IMappedAttributeKeyType<List<Double>, List<Double>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "double_array")
        )

        override val baseType: GenericAttributeType<List<Double>> = GenericAttributeType.DOUBLE_ARRAY

        @Suppress("UNCHECKED_CAST")
        override val valueType: Class<List<Double>> = List::class.java as Class<List<Double>>

        override fun format(value: List<Double>): List<Double> = value
    }
}
