package de.mctelemetry.core.api.metrics

import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.BooleanArrayType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.BooleanType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.DoubleArrayType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.DoubleType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.LongArrayType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.LongType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.StringArrayType
import de.mctelemetry.core.api.metrics.NativeAttributeKeyTypes.StringType
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

open class MappedAttributeKeyInfo<T : Any, B: Any>(
    val baseKey: AttributeKey<B>,
    val type: IMappedAttributeKeyType<T, B>,
) {

    companion object {


        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MappedAttributeKeyInfo<*, *>> = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256),
            { it.baseKey.key },
            IMappedAttributeKeyType.STREAM_CODEC,
            MappedAttributeKeyInfo<*, *>::type,
            ByteBufCodecs.OPTIONAL_COMPOUND_TAG,
            { Optional.ofNullable(it.save()) },
        ) { name, type, data ->
            type.create(name, data.getOrNull())
        }

        fun <T : Any> fromNative(key: AttributeKey<T>): MappedAttributeKeyInfo<T, T> {
            return MappedAttributeKeyInfo(key, NativeAttributeKeyTypes[key])
        }

        operator fun invoke(name: String, keyType: AttributeType): MappedAttributeKeyInfo<*, *> {
            return when (keyType) {
                AttributeType.STRING -> StringType.create(name, null)
                AttributeType.BOOLEAN -> BooleanType.create(name, null)
                AttributeType.LONG -> LongType.create(name, null)
                AttributeType.DOUBLE -> DoubleType.create(name, null)
                AttributeType.STRING_ARRAY -> StringArrayType.create(name, null)
                AttributeType.BOOLEAN_ARRAY -> BooleanArrayType.create(name, null)
                AttributeType.LONG_ARRAY -> LongArrayType.create(name, null)
                AttributeType.DOUBLE_ARRAY -> DoubleArrayType.create(name, null)
            }
        }

        @JvmName("createForStringKey")
        operator fun invoke(baseKey: AttributeKey<String>): MappedAttributeKeyInfo<String, String> =
            MappedAttributeKeyInfo(baseKey, StringType)

        @JvmName("createForBooleanKey")
        operator fun invoke(baseKey: AttributeKey<Boolean>): MappedAttributeKeyInfo<Boolean, Boolean> =
            MappedAttributeKeyInfo(baseKey, BooleanType)

        @JvmName("createForLongKey")
        operator fun invoke(baseKey: AttributeKey<Long>): MappedAttributeKeyInfo<Long, Long> =
            MappedAttributeKeyInfo(baseKey, LongType)

        @JvmName("createForDoubleKey")
        operator fun invoke(baseKey: AttributeKey<Double>): MappedAttributeKeyInfo<Double, Double> =
            MappedAttributeKeyInfo(baseKey, DoubleType)

        @JvmName("createForStringArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<String>>): MappedAttributeKeyInfo<List<String>, List<String>> =
            MappedAttributeKeyInfo(baseKey, StringArrayType)

        @JvmName("createForBooleanArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<Boolean>>): MappedAttributeKeyInfo<List<Boolean>, List<Boolean>> =
            MappedAttributeKeyInfo(baseKey, BooleanArrayType)

        @JvmName("createForLongArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<Long>>): MappedAttributeKeyInfo<List<Long>, List<Long>> =
            MappedAttributeKeyInfo(baseKey, LongArrayType)

        @JvmName("createForDoubleArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<Double>>): MappedAttributeKeyInfo<List<Double>, List<Double>> =
            MappedAttributeKeyInfo(baseKey, DoubleArrayType)


        fun forString(name: String): MappedAttributeKeyInfo<String, String> =
            MappedAttributeKeyInfo(AttributeKey.stringKey(name))

        fun forBoolean(name: String): MappedAttributeKeyInfo<Boolean, Boolean> =
            MappedAttributeKeyInfo(AttributeKey.booleanKey(name))

        fun forLong(name: String): MappedAttributeKeyInfo<Long, Long> =
            MappedAttributeKeyInfo(AttributeKey.longKey(name))

        fun forDouble(name: String): MappedAttributeKeyInfo<Double, Double> =
            MappedAttributeKeyInfo(AttributeKey.doubleKey(name))

        fun forStringArray(name: String): MappedAttributeKeyInfo<List<String>, List<String>> =
            MappedAttributeKeyInfo(AttributeKey.stringArrayKey(name))

        fun forBooleanArray(name: String): MappedAttributeKeyInfo<List<Boolean>, List<Boolean>> =
            MappedAttributeKeyInfo(AttributeKey.booleanArrayKey(name))

        fun forLongArray(name: String): MappedAttributeKeyInfo<List<Long>, List<Long>> =
            MappedAttributeKeyInfo(AttributeKey.longArrayKey(name))

        fun forDoubleArray(name: String): MappedAttributeKeyInfo<List<Double>, List<Double>> =
            MappedAttributeKeyInfo(AttributeKey.doubleArrayKey(name))
    }

    @Suppress("SameReturnValue")
    open fun save(): CompoundTag? {
        return null
    }
}
