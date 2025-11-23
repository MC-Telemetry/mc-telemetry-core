package de.mctelemetry.core.api.attributes

import de.mctelemetry.core.api.OTelCoreModAPI
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey

interface IMappedAttributeKeyType<T : Any, B : Any> {

    val id: ResourceKey<IMappedAttributeKeyType<*, *>>
    val valueType: Class<T>
    val valueStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>
    val baseType: GenericAttributeType<B>
    fun format(value: T): B
    fun create(name: String, savedData: CompoundTag?): MappedAttributeKeyInfo<T, B> {
        return MappedAttributeKeyInfo(
            baseKey = NativeAttributeKeyTypes.attributeKeyForType(baseType, name),
            type = this,
        )
    }

    fun canConvertDirectlyFrom(subtype: IMappedAttributeKeyType<*, *>): Boolean = false
    fun canConvertDirectlyTo(supertype: IMappedAttributeKeyType<*, *>): Boolean = false
    fun <R : Any> convertDirectlyFrom(subtype: IMappedAttributeKeyType<R, *>, value: R): T? = null
    fun <R : Any> convertDirectlyTo(supertype: IMappedAttributeKeyType<R, *>, value: T): R? = null

    companion object {

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IMappedAttributeKeyType<*, *>> = ByteBufCodecs.registry(
            OTelCoreModAPI.AttributeTypeMappings
        )
    }
}

infix fun IMappedAttributeKeyType<*, *>.canConvertTo(supertype: IMappedAttributeKeyType<*, *>): Boolean {

    return this === supertype ||
            canConvertDirectlyTo(supertype) ||
            canConvertFormatTo(supertype) ||
            supertype.canConvertDirectlyFrom(this)
}

@Suppress("unused")
infix fun IMappedAttributeKeyType<*, *>.canConvertFrom(subtype: IMappedAttributeKeyType<*, *>): Boolean {
    return subtype canConvertTo this
}

fun <T : Any, R : Any> IMappedAttributeKeyType<T, *>.convertTo(supertype: IMappedAttributeKeyType<R, *>, value: T): R? {
    @Suppress("UNCHECKED_CAST")
    if (this === supertype)
        return value as R
    return convertDirectlyTo(supertype, value)
        ?: convertFormatTo(supertype, value)
        ?: supertype.convertDirectlyFrom(this, value)
}

fun <T : Any, R : Any> IMappedAttributeKeyType<R, *>.convertFrom(subtype: IMappedAttributeKeyType<T, *>, value: T): R? {
    return subtype.convertTo(this, value)
}

private fun IMappedAttributeKeyType<*, *>.canConvertFormatTo(supertype: IMappedAttributeKeyType<*, *>): Boolean {
    return baseType.mappedType === supertype
}

private fun <T : Any, R : Any> IMappedAttributeKeyType<T, *>.convertFormatTo(
    supertype: IMappedAttributeKeyType<R, *>,
    value: T,
): R? {
    return if (baseType.mappedType === supertype)
        @Suppress("UNCHECKED_CAST") // known generic type from ref-equality check
        (format(value) as R)
    else
        null
}

operator fun <T : Any, B : Any> IMappedAttributeKeyType<T, B>.invoke(name: String, savedData: CompoundTag?=null): MappedAttributeKeyInfo<T, B> {
    return create(name, savedData)
}
