package de.mctelemetry.core.api.attributes

interface IMappedAttributeKeySet {
    val attributeKeys: Set<MappedAttributeKeyInfo<*, *>>

    companion object {

        private val empty = Default(emptySet())

        fun empty(): IMappedAttributeKeySet = empty

        private class Default(override val attributeKeys: Set<MappedAttributeKeyInfo<*, *>>) : IMappedAttributeKeySet

        operator fun invoke(attributeKeys: Collection<MappedAttributeKeyInfo<*, *>>): IMappedAttributeKeySet =
            if(attributeKeys.isEmpty()) empty
            else Default(attributeKeys.toSet())

        operator fun invoke(vararg attributeKeys: MappedAttributeKeyInfo<*, *>): IMappedAttributeKeySet =
            if(attributeKeys.isEmpty()) empty
            else Default(attributeKeys.toSet())
    }
}
