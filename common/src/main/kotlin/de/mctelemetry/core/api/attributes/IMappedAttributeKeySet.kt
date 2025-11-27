package de.mctelemetry.core.api.attributes

interface IMappedAttributeKeySet {
    val attributeKeys: Set<MappedAttributeKeyInfo<*, *>>

    companion object {
        operator fun invoke(attributeKeys: Collection<MappedAttributeKeyInfo<*,*>>): IMappedAttributeKeySet = object : IMappedAttributeKeySet {
            override val attributeKeys: Set<MappedAttributeKeyInfo<*, *>> = attributeKeys.toSet()
        }

        operator fun invoke(vararg attributeKeys: MappedAttributeKeyInfo<*,*>): IMappedAttributeKeySet = object : IMappedAttributeKeySet {
            override val attributeKeys: Set<MappedAttributeKeyInfo<*, *>> = attributeKeys.toSet()
        }
    }
}
