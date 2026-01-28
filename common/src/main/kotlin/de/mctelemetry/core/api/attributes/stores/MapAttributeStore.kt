package de.mctelemetry.core.api.attributes.stores

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.attributes.MappedAttributeKeyValue
import de.mctelemetry.core.api.attributes.stores.MapAttributeStore.Companion.asMappedAttributeKeyValue

class MapAttributeStore private constructor(
    private val data: MutableMap<AttributeDataSource.Reference<*>, Any?>,
    private val parent: IAttributeValueStore,
) : IAttributeValueStore.Mutable {

    constructor(
        data: MapAttributeStore,
        parent: IAttributeValueStore = IAttributeValueStore.Companion.empty(),
    ) : this(data.data.toMutableMap(), parent)

    companion object {

        @JvmName("newFromReferences")
        operator fun invoke(
            data: Collection<AttributeDataSource.Reference<*>>,
            parent: IAttributeValueStore = IAttributeValueStore.Companion.empty(),
        ) = MapAttributeStore(data.associateWithTo(mutableMapOf()) {
            null
        }, parent)

        @JvmName("newFromReferences")
        operator fun invoke(
            data: Map<AttributeDataSource.Reference<*>, Any?>,
            parent: IAttributeValueStore = IAttributeValueStore.Companion.empty(),
        ) = MapAttributeStore(data.also {
            data.forEach { (key, value) ->
                if (value == null) return@forEach
                require(key.type.templateType.valueType.isInstance(value)) {
                    "Incompatible key and value: $key to $value"
                }
            }
        }.toMutableMap(), parent)

        @JvmName("newFromAttributeKeyInfos")
        operator fun invoke(
            data: Collection<MappedAttributeKeyInfo<*, *>>,
            parent: IAttributeValueStore = IAttributeValueStore.Companion.empty(),
        ) = MapAttributeStore(data.associateTo(mutableMapOf()) {
            AttributeDataSource.Reference.TypedSlot(it) to null
        }, parent)

        @JvmName("newFromAttributeKeyInfos")
        operator fun invoke(
            data: Map<MappedAttributeKeyInfo<*, *>, Any?>,
            parent: IAttributeValueStore = IAttributeValueStore.Companion.empty(),
        ) = MapAttributeStore(data.mapKeysTo(mutableMapOf()) { (key, value) ->
            if (value != null) {
                require(key.templateType.valueType.isInstance(value)) {
                    "Incompatible key and value: $key to $value"
                }
            }
            AttributeDataSource.Reference.TypedSlot(key)
        }, parent)

        private fun <T : Any> Map.Entry<AttributeDataSource.Reference<T>, T?>.asMappedAttributeKeyValue(): MappedAttributeKeyValue<T, *>? {
            val value = value ?: return null
            return MappedAttributeKeyValue(key.info, value)
        }
    }

    override val references: Set<AttributeDataSource.Reference<*>> by lazy {
        if (parent === IAttributeValueStore.empty()) data.keys
        else data.keys.union(parent.references)
    }

    override fun <T : Any> get(reference: AttributeDataSource.Reference<T>): T? {
        return if (reference in data) {
            @Suppress("UNCHECKED_CAST")
            data.getValue(reference) as T?
                ?: throw java.util.NoSuchElementException("Key $reference is stored locally but has no value")
        } else {
            parent[reference]
        }
    }

    private fun <T : Any> getValue(info: AttributeDataSource.Reference<T>): T {
        @Suppress("UNCHECKED_CAST")
        return data.getValue(info) as T?
            ?: throw NoSuchElementException("Key $info is stored locally but has no value")
    }

    override fun <T : Any, R : AttributeDataSource.Reference<T>> prepareLookup(reference: R): ((R) -> T)? {
        return if (reference in data) ::getValue
        else parent.prepareLookup(reference)
    }

    fun <T : Any> update(key: AttributeDataSource.Reference<T>, value: T?) {
        require(key in data) {
            "Cannot add additional values after construction (tried to add $key=$value)"
        }
        data[key] = value
    }

    override fun <T : Any> set(reference: AttributeDataSource.Reference<T>, value: T?) {
        update(reference, value)
    }

    override fun entries(): Sequence<MappedAttributeKeyValue<*, *>> {
        if (data.isEmpty()) return parent.entries()
        return parent.entries() + data.asSequence().mapNotNull {
            @Suppress("UNCHECKED_CAST")
            (it as Map.Entry<AttributeDataSource.Reference<Any>, Any?>).asMappedAttributeKeyValue()
        }
    }
}
