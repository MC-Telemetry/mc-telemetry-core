package de.mctelemetry.core.api.attributes

interface IAttributeDateSourceReferenceSet {

    val references: Set<AttributeDataSource.ObservationSourceAttributeReference<*>>

    companion object {

        private val empty = Default(emptySet())

        fun empty(): IAttributeDateSourceReferenceSet = empty

        private class Default(override val references: Set<AttributeDataSource.ObservationSourceAttributeReference<*>>) :
                IAttributeDateSourceReferenceSet

        @JvmName("newFromReferenceCollection")
        operator fun invoke(attributeKeys: Collection<AttributeDataSource.ObservationSourceAttributeReference<*>>): IAttributeDateSourceReferenceSet =
            if (attributeKeys.isEmpty()) empty
            else Default(attributeKeys.toSet())

        @JvmName("newFromInfoCollection")
        operator fun invoke(attributeKeys: Collection<MappedAttributeKeyInfo<*, *>>): IAttributeDateSourceReferenceSet =
            if (attributeKeys.isEmpty()) empty
            else Default(attributeKeys.mapTo(mutableSetOf()) {
                AttributeDataSource.ObservationSourceAttributeReference(it)
            })

        @JvmName("newFromVararg")
        operator fun invoke(vararg attributeKeys: MappedAttributeKeyInfo<*, *>): IAttributeDateSourceReferenceSet =
            if (attributeKeys.isEmpty()) empty
            else Default(attributeKeys.mapTo(mutableSetOf()) {
                AttributeDataSource.ObservationSourceAttributeReference(it)
            })
    }
}
