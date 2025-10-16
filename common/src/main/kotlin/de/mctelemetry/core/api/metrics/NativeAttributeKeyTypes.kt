package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation

object NativeAttributeKeyTypes {

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

    operator fun invoke(keyType: AttributeType): IMappedAttributeKeyType<*, *> {
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

    operator fun <T : Any> invoke(attributeKey: AttributeKey<T>): IMappedAttributeKeyType<T, T> {
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

        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "string")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<String, String> {
            return MappedAttributeKeyInfo(AttributeKey.stringKey(name), this)
        }

        override fun canConvertDirectlyFrom(subtype: IMappedAttributeKeyType<*, *>): Boolean {
            return true
        }

        override fun <R : Any> convertDirectlyFrom(subtype: IMappedAttributeKeyType<R, *>, value: R): String {
            return value.toString()
        }

        override fun format(value: String): String = value
    }

    object BooleanType : IMappedAttributeKeyType<Boolean, Boolean> {

        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "boolean")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<Boolean, Boolean> {
            return MappedAttributeKeyInfo(AttributeKey.booleanKey(name), this)
        }

        override fun format(value: Boolean): Boolean = value
    }

    object LongType : IMappedAttributeKeyType<Long, Long> {

        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "long")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<Long, Long> {
            return MappedAttributeKeyInfo(AttributeKey.longKey(name), this)
        }

        override fun format(value: Long): Long = value
    }

    object DoubleType : IMappedAttributeKeyType<Double, Double> {

        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "double")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<Double, Double> {
            return MappedAttributeKeyInfo(AttributeKey.doubleKey(name), this)
        }

        override fun format(value: Double): Double = value
    }

    object StringArrayType : IMappedAttributeKeyType<List<String>, List<String>> {

        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "string_array")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<List<String>, List<String>> {
            return MappedAttributeKeyInfo(AttributeKey.stringArrayKey(name), this)
        }

        override fun format(value: List<String>): List<String> = value
    }

    object BooleanArrayType : IMappedAttributeKeyType<List<Boolean>, List<Boolean>> {

        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "boolean_array")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<List<Boolean>, List<Boolean>> {
            return MappedAttributeKeyInfo(AttributeKey.booleanArrayKey(name), this)
        }

        override fun format(value: List<Boolean>): List<Boolean> = value
    }

    object LongArrayType : IMappedAttributeKeyType<List<Long>, List<Long>> {

        override val id: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "long_array")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<List<Long>, List<Long>> {
            return MappedAttributeKeyInfo(AttributeKey.longArrayKey(name), this)
        }

        override fun format(value: List<Long>): List<Long> = value
    }

    object DoubleArrayType : IMappedAttributeKeyType<List<Double>, List<Double>> {

        override val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, "double_array")

        override fun create(
            name: String,
            savedData: CompoundTag?,
        ): MappedAttributeKeyInfo<List<Double>, List<Double>> {
            return MappedAttributeKeyInfo(AttributeKey.doubleArrayKey(name), this)
        }

        override fun format(value: List<Double>): List<Double> = value
    }
}
