package de.mctelemetry.core.api.attributes

import de.mctelemetry.core.api.OTelCoreModAPI
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey

interface IAttributeKeyTypeTemplate<T : Any, B : Any> {

    val id: ResourceKey<IAttributeKeyTypeTemplate<*, *>>
    val valueType: Class<T>
    val valueStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>
    val baseType: GenericAttributeType<B>
    fun format(value: T): B
    fun create(name: String, savedData: CompoundTag?): MappedAttributeKeyInfo<T, B> {
        return create(savedData).create(name)
    }

    fun create(savedData: CompoundTag?): IAttributeKeyTypeInstance<T, B> {
        return object : IAttributeKeyTypeInstance<T, B> {
            override val templateType: IAttributeKeyTypeTemplate<T, B>
                get() = this@IAttributeKeyTypeTemplate
        }
    }

    fun canConvertDirectlyFrom(subtype: IAttributeKeyTypeTemplate<*, *>): Boolean = false
    fun canConvertDirectlyTo(supertype: IAttributeKeyTypeTemplate<*, *>): Boolean = false
    fun <R : Any> convertDirectlyFrom(subtype: IAttributeKeyTypeTemplate<R, *>, value: R): T? = null
    fun <R : Any> convertDirectlyTo(supertype: IAttributeKeyTypeTemplate<R, *>, value: T): R? = null

    fun fromNbt(tag: Tag, lookupProvider: HolderLookup.Provider): T
    fun toNbt(value: T): Tag

    companion object {

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IAttributeKeyTypeTemplate<*, *>> =
            ByteBufCodecs.registry(
                OTelCoreModAPI.AttributeTypeMappings
            )
    }
}

infix fun IAttributeKeyTypeTemplate<*, *>.canConvertTo(supertype: IAttributeKeyTypeTemplate<*, *>): Boolean {

    return this === supertype ||
            canConvertDirectlyTo(supertype) ||
            canConvertFormatTo(supertype) ||
            supertype.canConvertDirectlyFrom(this)
}

@Suppress("unused")
infix fun IAttributeKeyTypeTemplate<*, *>.canConvertFrom(subtype: IAttributeKeyTypeTemplate<*, *>): Boolean {
    return subtype canConvertTo this
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<T, *>.convertTo(
    supertype: IAttributeKeyTypeTemplate<R, *>,
    value: T,
): R? {
    @Suppress("UNCHECKED_CAST")
    if (this === supertype)
        return value as R
    return convertDirectlyTo(supertype, value)
        ?: convertFormatTo(supertype, value)
        ?: supertype.convertDirectlyFrom(this, value)
}


fun <T : Any, R : Any> MappedAttributeKeyValue<T, *>.convertTo(supertype: IAttributeKeyTypeTemplate<R, *>): R? {
    @Suppress("UNCHECKED_CAST") // relevant part of cast (T) is still retained, only wildcard is cascaded
    return (info.templateType as IAttributeKeyTypeTemplate<T, *>).convertTo(supertype, value)
}

fun <T : Any, R : Any> AttributeDataSource.ConstantAttributeData<T>.convertValueTo(supertype: IAttributeKeyTypeTemplate<R, *>): R? {
    return type.convertTo(supertype, value)
}

fun <T : Any, R : Any> AttributeDataSource.ConstantAttributeData<T>.convertTo(supertype: IAttributeKeyTypeTemplate<R, *>): AttributeDataSource.ConstantAttributeData<R>? {
    return AttributeDataSource.ConstantAttributeData(supertype,type.convertTo(supertype, value) ?: return null)
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<R, *>.convertFrom(
    subtype: IAttributeKeyTypeTemplate<T, *>,
    value: T,
): R? {
    return subtype.convertTo(this, value)
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<R, *>.convertFrom(subtypeValue: MappedAttributeKeyValue<T, *>): R? {
    return subtypeValue.convertTo(this)
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<R, *>.convertValueFrom(subtypeValue: AttributeDataSource.ConstantAttributeData<T>): R? {
    return subtypeValue.convertValueTo(this)
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<R, *>.convertFrom(subtypeValue: AttributeDataSource.ConstantAttributeData<T>): AttributeDataSource.ConstantAttributeData<R>? {
    return subtypeValue.convertTo(this)
}

private fun IAttributeKeyTypeTemplate<*, *>.canConvertFormatTo(supertype: IAttributeKeyTypeTemplate<*, *>): Boolean {
    return baseType.mappedType === supertype
}

private fun <T : Any, R : Any> IAttributeKeyTypeTemplate<T, *>.convertFormatTo(
    supertype: IAttributeKeyTypeTemplate<R, *>,
    value: T,
): R? {
    return if (baseType.mappedType === supertype)
        @Suppress("UNCHECKED_CAST") // known generic type from ref-equality check
        (format(value) as R)
    else
        null
}

operator fun <T : Any, B : Any> IAttributeKeyTypeTemplate<T, B>.invoke(
    name: String,
    savedData: CompoundTag? = null,
): MappedAttributeKeyInfo<T, B> {
    return create(name, savedData)
}

operator fun <T : Any, B : Any> IAttributeKeyTypeTemplate<T, B>.invoke(
    savedData: CompoundTag? = null,
): IAttributeKeyTypeInstance<T, B> {
    return create(savedData)
}
