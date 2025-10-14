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
import net.minecraft.nbt.Tag

open class MappedAttributeKeyInfo<in T, B>(
    val baseKey: AttributeKey<B>,
    val type: IMappedAttributeKeyType<T, B>,
) {

    companion object {

        fun <T> fromNative(key: AttributeKey<T>): MappedAttributeKeyInfo<T,T> {
            return MappedAttributeKeyInfo(key, NativeAttributeKeyTypes(key))
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
    }

    @Suppress("SameReturnValue")
    open fun save(): Tag? {
        return null
    }
}
