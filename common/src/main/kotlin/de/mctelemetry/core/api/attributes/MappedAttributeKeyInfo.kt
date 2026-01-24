package de.mctelemetry.core.api.attributes

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.BooleanArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.BooleanType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.DoubleArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.DoubleType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.LongArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.LongType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.StringArrayType
import de.mctelemetry.core.api.attributes.NativeAttributeKeyTypes.StringType
import de.mctelemetry.core.utils.addErrorTo
import de.mctelemetry.core.utils.get
import de.mctelemetry.core.utils.getParsedValue
import de.mctelemetry.core.utils.getStringValue
import de.mctelemetry.core.utils.mergeErrorMessages
import de.mctelemetry.core.utils.resultOrElse
import de.mctelemetry.core.utils.resultOrPartialOrElse
import de.mctelemetry.core.utils.withEncodedEntry
import de.mctelemetry.core.utils.withEntry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import java.util.function.Supplier

open class MappedAttributeKeyInfo<T : Any, B : Any, I : IAttributeKeyTypeInstance<T, B, I>>(
    val baseKey: AttributeKey<B>,
    val typeInstance: I,
) {

    val templateType: IAttributeKeyTypeTemplate<T, B, I>
        get() = typeInstance.templateType

    companion object {

        val CODEC: Codec<MappedAttributeKeyInfo<*, *, *>> = object : Codec<MappedAttributeKeyInfo<*, *, *>> {
            override fun <T> encode(
                input: MappedAttributeKeyInfo<*, *, *>,
                ops: DynamicOps<T>,
                prefix: T
            ): DataResult<T> {
                var result = prefix
                var errors: MutableList<Supplier<String>>? = null
                if (input.baseKey.key.length > OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_NAME_MAX_LENGTH) {
                    return DataResult.error { "Instrument attribute name is too long" }
                } else if (input.baseKey.key.isEmpty()) {
                    return DataResult.error { "Instrument attribute name is empty" }
                }
                context(ops) {
                    result = result.withEntry("name", ops.createString(input.baseKey.key))
                    val typeInstance = input.typeInstance
                    result = result.withEncodedEntry("type", typeInstance.templateType, IAttributeKeyTypeTemplate.CODEC)
                        .resultOrPartialOrElse(
                            onError = { errors = it.addErrorTo(errors) },
                            fallback = { return DataResult.error(it.messageSupplier) }
                        )

                    result = result.withEntry(
                        "data",
                        typeInstance.encodeInstanceDetails(ops.empty()).resultOrPartialOrElse(
                            onError = { errors = it.addErrorTo(errors) },
                            fallback = { return DataResult.error(mergeErrorMessages(errors!!)) }
                        ),
                        allowEmpty = false,
                    )
                    return if (errors != null) {
                        DataResult.error(mergeErrorMessages(errors), result)
                    } else {
                        DataResult.success(result)
                    }
                }
            }

            override fun <T> decode(
                ops: DynamicOps<T>,
                input: T
            ): DataResult<Pair<MappedAttributeKeyInfo<*, *, *>, T>> {
                val result: MappedAttributeKeyInfo<*, *, *>
                var errors: MutableList<Supplier<String>>? = null

                context(ops) {
                    val name: String = input.getStringValue("name").resultOrElse {
                        return DataResult.error(it.messageSupplier)
                    }
                    if (name.length > OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_NAME_MAX_LENGTH) {
                        return DataResult.error { "Instrument attribute name is too long" }
                    }
                    if (name.isEmpty()) {
                        return DataResult.error { "Instrument attribute name is empty" }
                    }
                    val type = input.getParsedValue("type", IAttributeKeyTypeTemplate.CODEC).resultOrElse {
                        return DataResult.error(it.messageSupplier)
                    }
                    val typeInstance = input["data"].resultOrElse { ops.empty() }
                        .let { dataElement -> type.typeInstanceCodec.parse(ops, dataElement) }
                        .resultOrPartialOrElse(
                            onError = { errors = it.addErrorTo(errors) },
                            fallback = { return DataResult.error(mergeErrorMessages(errors!!)) }
                        )
                    result = typeInstance.create(name)
                }
                return if (errors == null) {
                    DataResult.success(Pair(result, input))
                } else {
                    DataResult.error(mergeErrorMessages(errors), Pair(result, input))
                }
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MappedAttributeKeyInfo<*, *, *>> =
            object : StreamCodec<RegistryFriendlyByteBuf, MappedAttributeKeyInfo<*, *, *>> {
                override fun decode(`object`: RegistryFriendlyByteBuf): MappedAttributeKeyInfo<*, *, *> {
                    val name = `object`.readUtf(OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_NAME_MAX_LENGTH)
                    require(name.isNotEmpty()) { "Instrument attribute name is empty" }
                    val type = IAttributeKeyTypeTemplate.STREAM_CODEC.decode(`object`)
                    val instance = type.typeInstanceStreamCodec.decode(`object`)
                    return instance.create(name)
                }

                override fun encode(
                    `object`: RegistryFriendlyByteBuf,
                    object2: MappedAttributeKeyInfo<*, *, *>
                ) {
                    val name = object2.baseKey.key
                    val typeInstance = object2.typeInstance
                    require(name.isNotEmpty()) { "Instrument attribute name is empty" }
                    `object`.writeUtf(name, OTelCoreModAPI.Limits.INSTRUMENT_ATTRIBUTES_NAME_MAX_LENGTH)
                    IAttributeKeyTypeTemplate.STREAM_CODEC.encode(`object`, typeInstance.templateType)
                    typeInstance.encodeInstanceDetails(`object`)
                }
            }

        // cast must use a concrete type (which is undone before returning) because type wildcards cannot be passed to
        // constructors and the correct type is recursive
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> fromNative(key: AttributeKey<T>): MappedAttributeKeyInfo<T, T, *> {
            val instanceType: IAttributeKeyTypeInstance.InstanceType<T, T, *> = NativeAttributeKeyTypes[key]
            return instanceType.create(key)
        }

        operator fun invoke(name: String, keyType: AttributeType): MappedAttributeKeyInfo<*, *, *> {
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
        operator fun invoke(baseKey: AttributeKey<String>): MappedAttributeKeyInfo<String, String, StringType> =
            MappedAttributeKeyInfo(baseKey, StringType)

        @JvmName("createForBooleanKey")
        operator fun invoke(baseKey: AttributeKey<Boolean>): MappedAttributeKeyInfo<Boolean, Boolean, BooleanType> =
            MappedAttributeKeyInfo(baseKey, BooleanType)

        @JvmName("createForLongKey")
        operator fun invoke(baseKey: AttributeKey<Long>): MappedAttributeKeyInfo<Long, Long, LongType> =
            MappedAttributeKeyInfo(baseKey, LongType)

        @JvmName("createForDoubleKey")
        operator fun invoke(baseKey: AttributeKey<Double>): MappedAttributeKeyInfo<Double, Double, DoubleType> =
            MappedAttributeKeyInfo(baseKey, DoubleType)

        @JvmName("createForStringArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<String>>): MappedAttributeKeyInfo<List<String>, List<String>, StringArrayType> =
            MappedAttributeKeyInfo(baseKey, StringArrayType)

        @JvmName("createForBooleanArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<Boolean>>): MappedAttributeKeyInfo<List<Boolean>, List<Boolean>, BooleanArrayType> =
            MappedAttributeKeyInfo(baseKey, BooleanArrayType)

        @JvmName("createForLongArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<Long>>): MappedAttributeKeyInfo<List<Long>, List<Long>, LongArrayType> =
            MappedAttributeKeyInfo(baseKey, LongArrayType)

        @JvmName("createForDoubleArrayKey")
        operator fun invoke(baseKey: AttributeKey<List<Double>>): MappedAttributeKeyInfo<List<Double>, List<Double>, DoubleArrayType> =
            MappedAttributeKeyInfo(baseKey, DoubleArrayType)


        fun forString(name: String): MappedAttributeKeyInfo<String, String, StringType> =
            MappedAttributeKeyInfo(AttributeKey.stringKey(name))

        fun forBoolean(name: String): MappedAttributeKeyInfo<Boolean, Boolean, BooleanType> =
            MappedAttributeKeyInfo(AttributeKey.booleanKey(name))

        fun forLong(name: String): MappedAttributeKeyInfo<Long, Long, LongType> =
            MappedAttributeKeyInfo(AttributeKey.longKey(name))

        fun forDouble(name: String): MappedAttributeKeyInfo<Double, Double, DoubleType> =
            MappedAttributeKeyInfo(AttributeKey.doubleKey(name))

        fun forStringArray(name: String): MappedAttributeKeyInfo<List<String>, List<String>, StringArrayType> =
            MappedAttributeKeyInfo(AttributeKey.stringArrayKey(name))

        fun forBooleanArray(name: String): MappedAttributeKeyInfo<List<Boolean>, List<Boolean>, BooleanArrayType> =
            MappedAttributeKeyInfo(AttributeKey.booleanArrayKey(name))

        fun forLongArray(name: String): MappedAttributeKeyInfo<List<Long>, List<Long>, LongArrayType> =
            MappedAttributeKeyInfo(AttributeKey.longArrayKey(name))

        fun forDoubleArray(name: String): MappedAttributeKeyInfo<List<Double>, List<Double>, DoubleArrayType> =
            MappedAttributeKeyInfo(AttributeKey.doubleArrayKey(name))
    }

    override fun toString(): String {
        val typeId: String = typeInstance.templateType.id.location().let {
            if (it.namespace == ResourceLocation.DEFAULT_NAMESPACE) it.path else it.toString()
        }
        return "${baseKey.key}#$typeId"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MappedAttributeKeyInfo<*, *, *>

        if (baseKey != other.baseKey) return false
        if (typeInstance != other.typeInstance) return false

        return true
    }

    override fun hashCode(): Int {
        return baseKey.hashCode()
    }
}
