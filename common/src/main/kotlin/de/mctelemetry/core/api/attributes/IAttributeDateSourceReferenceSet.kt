package de.mctelemetry.core.api.attributes

interface IAttributeDateSourceReferenceSet {

    val references: Set<AttributeDataSource.Reference<*>>

    fun findObservationSourceAttributeReference(name: String): AttributeDataSource.Reference.ObservationSourceAttributeReference<*>? {
        return references.firstOrNull {
            it is AttributeDataSource.Reference.ObservationSourceAttributeReference<*> && it.attributeName == name
        } as AttributeDataSource.Reference.ObservationSourceAttributeReference<*>?
    }

    companion object {

        private val empty = Default(emptySet())

        fun empty(): IAttributeDateSourceReferenceSet = empty

        private class Default(override val references: Set<AttributeDataSource.Reference<*>>) :
                IAttributeDateSourceReferenceSet

        @JvmName("newFromReferenceCollection")
        operator fun invoke(attributeKeys: Collection<AttributeDataSource.Reference<*>>): IAttributeDateSourceReferenceSet =
            if (attributeKeys.isEmpty()) empty
            else Default(attributeKeys.toSet())

        @JvmName("newFromInfoCollection")
        operator fun invoke(attributeKeys: Collection<MappedAttributeKeyInfo<*, *>>): IAttributeDateSourceReferenceSet =
            if (attributeKeys.isEmpty()) empty
            else Default(attributeKeys.mapTo(mutableSetOf()) {
                AttributeDataSource.Reference.TypedSlot(it)
            })

        @JvmName("newFromVararg")
        operator fun invoke(vararg attributeKeys: MappedAttributeKeyInfo<*, *>): IAttributeDateSourceReferenceSet =
            if (attributeKeys.isEmpty()) empty
            else Default(attributeKeys.mapTo(mutableSetOf()) {
                AttributeDataSource.Reference.TypedSlot(it)
            })
    }
}
