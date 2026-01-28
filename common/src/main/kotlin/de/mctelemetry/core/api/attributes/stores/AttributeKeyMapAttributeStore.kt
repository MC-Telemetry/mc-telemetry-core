package de.mctelemetry.core.api.attributes.stores

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.MappedAttributeKeyMap
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue

class AttributeKeyMapAttributeStore(val attributeMap: MappedAttributeKeyMap<*>) : IAttributeValueStore {

    constructor(attributes: Collection<MappedAttributeKeyValue<*, *>>) : this(MappedAttributeKeyMap(attributes))

    override fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T? {
        val info = reference.info
        return if (info in attributeMap.attributeKeys) {
            attributeMap[info]
                ?: throw java.util.NoSuchElementException("Key $reference is stored locally but has no value")
        } else null
    }

    override fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(
        reference: R
    ): ((R) -> T)? {
        return if (reference.info in attributeMap.attributeKeys) {
            {
                attributeMap[it.info]
                    ?: throw java.util.NoSuchElementException("Key $it is stored locally but has no value")
            }
        } else null
    }

    override fun entries(): Sequence<MappedAttributeKeyValue<*, *>> = attributeMap.asSequence()

    override val references: Set<AttributeDataSource.Reference<*>> by lazy {
        attributeMap.attributeKeys.mapTo(mutableSetOf()) {
            AttributeDataSource.Reference.TypedSlot(it)
        }.toSet()
    }
}
