package de.mctelemetry.core.api.metrics

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation

interface IMappedAttributeKeyType<T, B> {

    val id: ResourceLocation
    fun format(value: T): B
    fun create(name: String, savedData: CompoundTag?): MappedAttributeKeyInfo<T, B>
    fun canConvertDirectlyFrom(subtype: IMappedAttributeKeyType<*, *>): Boolean = false
    fun canConvertDirectlyTo(supertype: IMappedAttributeKeyType<*, *>): Boolean = false
    fun <R : Any> convertDirectlyFrom(subtype: IMappedAttributeKeyType<R, *>, value: R): T? = null
    fun <R : Any> convertDirectlyTo(supertype: IMappedAttributeKeyType<R, *>, value: T): R? = null
}

infix fun IMappedAttributeKeyType<*, *>.canConvertTo(supertype: IMappedAttributeKeyType<*, *>): Boolean {
    return this === supertype || canConvertDirectlyTo(supertype) || supertype.canConvertDirectlyFrom(this)
}

infix fun IMappedAttributeKeyType<*, *>.canConvertFrom(subtype: IMappedAttributeKeyType<*, *>): Boolean {
    return subtype canConvertTo this
}

fun <T : Any, R : Any> IMappedAttributeKeyType<T, *>.convertTo(supertype: IMappedAttributeKeyType<R, *>, value: T): R? {
    @Suppress("UNCHECKED_CAST")
    if (this === supertype)
        return value as R
    return convertDirectlyTo(supertype, value) ?: supertype.convertDirectlyFrom(this, value)
}

fun <T : Any, R : Any> IMappedAttributeKeyType<R, *>.convertFrom(subtype: IMappedAttributeKeyType<T, *>, value: T): R? {
    return subtype.convertTo(this, value)
}
