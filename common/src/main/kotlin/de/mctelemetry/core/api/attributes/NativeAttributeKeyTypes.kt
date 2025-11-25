package de.mctelemetry.core.api.attributes

import de.mctelemetry.core.api.OTelCoreModAPI
import io.netty.buffer.ByteBuf
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import it.unimi.dsi.fastutil.booleans.BooleanArrayList
import it.unimi.dsi.fastutil.booleans.BooleanList
import it.unimi.dsi.fastutil.booleans.BooleanLists
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.longs.LongArrayList
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
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
        override val valueStreamCodec: StreamCodec<ByteBuf, String> = ByteBufCodecs.STRING_UTF8
        override val valueType: Class<String> = String::class.java

        override fun canConvertDirectlyFrom(subtype: IMappedAttributeKeyType<*, *>): Boolean {
            return true
        }

        override fun <R : Any> convertDirectlyFrom(subtype: IMappedAttributeKeyType<R, *>, value: R): String {
            return value.toString()
        }

        override fun format(value: String): String = value

        fun MappedAttributeKeyValue<*,*>.convertValueToString(): String {
            return convertTo(StringType)!!
        }
    }

    object BooleanType : IMappedAttributeKeyType<Boolean, Boolean> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "boolean")
        )

        override val baseType: GenericAttributeType<Boolean> = GenericAttributeType.BOOLEAN
        override val valueStreamCodec: StreamCodec<ByteBuf, Boolean> = ByteBufCodecs.BOOL
        override val valueType: Class<Boolean> = Boolean::class.java

        override fun format(value: Boolean): Boolean = value
    }

    object LongType : IMappedAttributeKeyType<Long, Long> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "long")
        )

        override val baseType: GenericAttributeType<Long> = GenericAttributeType.LONG
        override val valueStreamCodec: StreamCodec<ByteBuf, Long> = ByteBufCodecs.VAR_LONG
        override val valueType: Class<Long> = Long::class.java

        override fun format(value: Long): Long = value
    }

    object DoubleType : IMappedAttributeKeyType<Double, Double> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "double")
        )

        override val baseType: GenericAttributeType<Double> = GenericAttributeType.DOUBLE
        override val valueStreamCodec: StreamCodec<ByteBuf, Double> = ByteBufCodecs.DOUBLE
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
        override val valueStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, List<String>> =
            ByteBufCodecs.collection({ if (it == 0) emptyList() else ArrayList(it) }, ByteBufCodecs.STRING_UTF8)

        override fun format(value: List<String>): List<String> = value
    }

    object BooleanArrayType : IMappedAttributeKeyType<List<Boolean>, List<Boolean>>,
            StreamCodec<FriendlyByteBuf, List<Boolean>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "boolean_array")
        )

        override val baseType: GenericAttributeType<List<Boolean>> = GenericAttributeType.BOOLEAN_ARRAY

        @Suppress("UNCHECKED_CAST")
        override val valueType: Class<List<Boolean>> = List::class.java as Class<List<Boolean>>

        override fun format(value: List<Boolean>): List<Boolean> = value
        override val valueStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, List<Boolean>>
            get() = this

        override fun decode(`object`: FriendlyByteBuf): BooleanList {
            val size = `object`.readVarInt()
            if (size == 0) return BooleanLists.emptyList()
            val list: BooleanList = BooleanArrayList(size)
            repeat(size.floorDiv(8)) { byteIndex ->
                var data = `object`.readByte().toInt()
                repeat(8) { i ->
                    list.add(byteIndex * 8 + 7 - i, (data and 0b1) == 0b1)
                    data = data ushr 1
                }
                assert(data == 0)
            }
            val partialDataSize = size % 8
            if (partialDataSize == 0) return list
            var data: Int = `object`.readByte().toInt()
            val partialBaseIndex = size - partialDataSize
            repeat(partialDataSize) { i ->
                list.add(partialBaseIndex + partialDataSize - i, (data and 0b1) == 0b1)
                data = data ushr 1
            }
            assert(data == 0)
            return list
        }

        override fun encode(`object`: FriendlyByteBuf, object2: List<Boolean>) {
            `object`.writeVarInt(object2.size)
            var currentByteIndex = 0
            var currentByte = 0
            for (entry in object2) {
                currentByte = (currentByte shl 1) or if (entry) 1 else 0
                currentByteIndex = (currentByteIndex + 1) % 8
                if (currentByteIndex == 0) {
                    `object`.writeByte(currentByte)
                    currentByte = 0
                }
            }
            if (currentByteIndex != 0) {
                `object`.writeByte(currentByte)
            }
        }
    }

    object LongArrayType : IMappedAttributeKeyType<List<Long>, List<Long>> {

        override val id: ResourceKey<IMappedAttributeKeyType<*, *>> = ResourceKey.create(
            OTelCoreModAPI.AttributeTypeMappings,
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "long_array")
        )

        override val baseType: GenericAttributeType<List<Long>> = GenericAttributeType.LONG_ARRAY

        @Suppress("UNCHECKED_CAST")
        override val valueType: Class<List<Long>> = List::class.java as Class<List<Long>>
        override val valueStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, List<Long>> =
            ByteBufCodecs.collection({ if (it == 0) emptyList() else LongArrayList(it) }, ByteBufCodecs.VAR_LONG)

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
        override val valueStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, List<Double>> =
            ByteBufCodecs.collection({ if (it == 0) emptyList() else DoubleArrayList(it) }, ByteBufCodecs.DOUBLE)

        override fun format(value: List<Double>): List<Double> = value
    }
}
