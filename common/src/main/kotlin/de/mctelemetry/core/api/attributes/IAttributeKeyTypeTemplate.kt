package de.mctelemetry.core.api.attributes

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.OTelCoreModAPI
import de.mctelemetry.core.persistence.RegistryIdFieldCodec
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.util.NullOps

interface IAttributeKeyTypeTemplate<T : Any, B : Any, I : IAttributeKeyTypeInstance<T, B, *>> {

    val id: ResourceKey<IAttributeKeyTypeTemplate<*, *, *>>
    val valueType: Class<T>
    val valueCodec: Codec<T>
    val valueStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>
    val typeInstanceCodec: Codec<I>
    val typeInstanceStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, I>

    val baseType: GenericAttributeType<B>
    fun format(value: T): B

    fun canConvertDirectlyFrom(subtype: IAttributeKeyTypeTemplate<*, *, *>): Boolean = false
    fun canConvertDirectlyTo(supertype: IAttributeKeyTypeTemplate<*, *, *>): Boolean = false
    fun <R : Any> convertDirectlyFrom(subtype: IAttributeKeyTypeTemplate<R, *, *>, value: R): T? = null
    fun <R : Any> convertDirectlyTo(supertype: IAttributeKeyTypeTemplate<R, *, *>, value: T): R? = null

    companion object {

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, IAttributeKeyTypeTemplate<*, *, *>> =
            ByteBufCodecs.registry(
                OTelCoreModAPI.AttributeTypeMappings
            )

        val CODEC: Codec<IAttributeKeyTypeTemplate<*, *, *>> = RegistryIdFieldCodec(
            OTelCoreModAPI.AttributeTypeMappings,
            IAttributeKeyTypeTemplate<*, *, *>::id,
        )
    }
}

fun <T : Any, B : Any, I : IAttributeKeyTypeInstance<T, B, I>> IAttributeKeyTypeTemplate<T, B, I>.create(
    name: String,
    data: CompoundTag? = null
): MappedAttributeKeyInfo<T, B, I> {
    return create(data).create(name)
}

fun <T : Any, B : Any, I : IAttributeKeyTypeInstance<T, B, I>> IAttributeKeyTypeTemplate<T, B, I>.create(
    data: CompoundTag?
): I {
    return if (data == null) {
        typeInstanceCodec.decode(NullOps.INSTANCE, net.minecraft.util.Unit.INSTANCE)
    } else {
        typeInstanceCodec.decode(NbtOps.INSTANCE, data)
    }.orThrow.first
}

infix fun IAttributeKeyTypeTemplate<*, *, *>.canConvertTo(
    supertype: IAttributeKeyTypeTemplate<*, *, *>,
): Boolean {
    return this === supertype ||
            canConvertDirectlyTo(supertype) ||
            canConvertFormatTo(supertype) ||
            supertype.canConvertDirectlyFrom(this)
}

@Suppress("unused")
infix fun IAttributeKeyTypeTemplate<*, *, *>.canConvertFrom(
    subtype: IAttributeKeyTypeTemplate<*, *, *>,
): Boolean {
    return subtype canConvertTo this
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<T, *, *>.convertTo(
    supertype: IAttributeKeyTypeTemplate<R, *, *>,
    value: T,
): R? {
    @Suppress("UNCHECKED_CAST")
    if (this === supertype)
        return value as R
    return convertDirectlyTo(supertype, value)
        ?: convertFormatTo(supertype, value)
        ?: supertype.convertDirectlyFrom(this, value)
}


fun <T : Any, R : Any> MappedAttributeKeyValue<T, *>.convertTo(
    supertype: IAttributeKeyTypeTemplate<R, *, *>,
): R? {
    @Suppress("UNCHECKED_CAST") // relevant part of cast (T) is still retained, only wildcard is cascaded
    return (info.templateType as IAttributeKeyTypeTemplate<T, *, *>).convertTo(supertype, value)
}

fun <T : Any, R : Any> AttributeDataSource.ConstantAttributeData<T>.convertValueTo(
    supertype: IAttributeKeyTypeTemplate<R, *, *>,
): R? {
    return type.templateType.convertTo(supertype, value)
}

fun <T : Any, R : Any> AttributeDataSource.ConstantAttributeData<T>.convertTo(
    supertype: IAttributeKeyTypeInstance<R, *, *>,
): AttributeDataSource.ConstantAttributeData<R>? {
    return AttributeDataSource.ConstantAttributeData(
        supertype, type.templateType.convertTo(supertype.templateType, value) ?: return null
    )
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<R, *, *>.convertFrom(
    subtype: IAttributeKeyTypeTemplate<T, *, *>,
    value: T,
): R? {
    return subtype.convertTo(this, value)
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<R, *, *>.convertFrom(
    subtypeValue: MappedAttributeKeyValue<T, *>,
): R? {
    return subtypeValue.convertTo(this)
}

fun <T : Any, R : Any> IAttributeKeyTypeTemplate<R, *, *>.convertValueFrom(
    subtypeValue: AttributeDataSource.ConstantAttributeData<T>,
): R? {
    return subtypeValue.convertValueTo(this)
}

fun <T : Any, R : Any> IAttributeKeyTypeInstance<R, *, *>.convertFrom(
    subtypeValue: AttributeDataSource.ConstantAttributeData<T>,
): AttributeDataSource.ConstantAttributeData<R>? {
    return subtypeValue.convertTo(this)
}

private fun IAttributeKeyTypeTemplate<*, *, *>.canConvertFormatTo(
    supertype: IAttributeKeyTypeTemplate<*, *, *>,
): Boolean {
    return baseType.mappedType === supertype
}

private fun <T : Any, R : Any> IAttributeKeyTypeTemplate<T, *, *>.convertFormatTo(
    supertype: IAttributeKeyTypeTemplate<R, *, *>,
    value: T,
): R? {
    return if (baseType.mappedType === supertype)
        @Suppress("UNCHECKED_CAST") // known generic type from ref-equality check
        (format(value) as R)
    else
        null
}
