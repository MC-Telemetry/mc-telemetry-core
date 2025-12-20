package de.mctelemetry.core.api.attributes

import net.minecraft.nbt.CompoundTag

interface IAttributeKeyTypeInstance<T : Any, B : Any> {

    val templateType: IAttributeKeyTypeTemplate<T, B>

    fun saveTemplateData(): CompoundTag? {
        return null
    }

    fun create(name: String): MappedAttributeKeyInfo<T, B> {
        return MappedAttributeKeyInfo(
            baseKey = NativeAttributeKeyTypes.attributeKeyForType(
                templateType.baseType,
                name
            ),
            templateType,
        )
    }

    interface InstanceType<T : Any, B : Any> : IAttributeKeyTypeInstance<T, B>, IAttributeKeyTypeTemplate<T, B> {

        override val templateType: InstanceType<T, B>
            get() = this

        override fun create(savedData: CompoundTag?): IAttributeKeyTypeInstance<T, B> {
            return this
        }

        override fun saveTemplateData(): CompoundTag? {
            return null
        }
    }
}
