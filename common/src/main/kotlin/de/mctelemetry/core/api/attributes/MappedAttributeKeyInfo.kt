package de.mctelemetry.core.api.attributes

import de.mctelemetry.core.OTelCoreMod
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.BooleanArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.BooleanType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.DoubleArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.DoubleType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.LongArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.LongType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.StringArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.StringType
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import net.minecraft.core.HolderGetter
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

open class MappedAttributeKeyInfo<T : Any, B : Any>(
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
            { Optional.ofNullable(it.saveAdditional()) },
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

        fun load(
            tag: CompoundTag,
            attributeKeyTypeHolderGetter: HolderGetter<IMappedAttributeKeyType<*, *>>,
        ): MappedAttributeKeyInfo<*, *> {
            val name = tag.getString("name")
            if (name.isNullOrEmpty()) throw NoSuchElementException("Could not find key 'name'")
            val type = tag.getString("type")
            if (type.isNullOrEmpty()) throw NoSuchElementException("Could not find key 'type'")
            val typeResourceLocation = ResourceLocation.parse(type)
            val mappingType: IMappedAttributeKeyType<*, *> = try {
                attributeKeyTypeHolderGetter.getOrThrow(
                    ResourceKey.create(
                        OTelCoreModAPI.AttributeTypeMappings,
                        ResourceLocation.parse(type)
                    )
                ).value()
            } catch (ex: Exception) {
                val otherResourceLocation: ResourceLocation = when (typeResourceLocation.namespace) {
                    ResourceLocation.DEFAULT_NAMESPACE -> ResourceLocation.fromNamespaceAndPath(
                        OTelCoreModAPI.MOD_ID,
                        typeResourceLocation.path
                    )
                    OTelCoreModAPI.MOD_ID -> ResourceLocation.fromNamespaceAndPath(
                        ResourceLocation.DEFAULT_NAMESPACE,
                        typeResourceLocation.path
                    )
                    else -> throw ex
                }
                try {
                    attributeKeyTypeHolderGetter.getOrThrow(
                        ResourceKey.create(OTelCoreModAPI.AttributeTypeMappings, otherResourceLocation)
                    ).value().also {
                        OTelCoreMod.logger.warn("Converted attribute resource location from $typeResourceLocation to $otherResourceLocation during loading")
                    }
                } catch (ex2: Exception) {
                    ex.addSuppressed(ex2)
                    throw ex
                }
            }
            return mappingType.create(name, tag.getCompound("data"))
        }
    }

    @Suppress("SameReturnValue")
    open fun saveAdditional(): CompoundTag? {
        return null
    }

    fun save(): CompoundTag {
        return CompoundTag().also { saveTag ->
            saveTag.putString("name", baseKey.key)
            saveTag.putString("type", type.id.location().toString())
            val data = saveAdditional()
            if (data != null) {
                saveTag.put("data", data)
            }
        }
    }

    override fun toString(): String {
        val additional = saveAdditional()
        val typeId: String = type.id.location().let {
            if (it.namespace == ResourceLocation.DEFAULT_NAMESPACE) it.path else it.toString()
        }
        return if (additional != null) {
            "${baseKey.key}#$typeId$additional"
        } else {
            "${baseKey.key}#$typeId"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MappedAttributeKeyInfo<*, *>

        if (baseKey != other.baseKey) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return baseKey.hashCode()
    }
}
