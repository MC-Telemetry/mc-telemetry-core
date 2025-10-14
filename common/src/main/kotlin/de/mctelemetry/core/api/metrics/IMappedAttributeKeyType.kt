package de.mctelemetry.core.api.metrics

import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation

interface IMappedAttributeKeyType<in T, B> {

    val id: ResourceLocation
    fun format(value: T): B
    fun create(name: String, savedData: Tag?): MappedAttributeKeyInfo<T, B>
}
