package de.mctelemetry.core.api.metrics

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation

interface IMappedAttributeKeyType<T : Any, B> {

    val id: ResourceLocation
    fun format(value: T): B
    fun create(name: String, savedData: CompoundTag?): MappedAttributeKeyInfo<T, B>
}
