package de.mctelemetry.core.api.observations

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeInstance
import de.mctelemetry.core.api.attributes.stores.IAttributeValueStore
import de.mctelemetry.core.api.attributes.stores.MapAttributeStore
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.nbt.Tag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

abstract class ObservationSourceBase<SC, I : IObservationSourceInstance<SC, MapAttributeStore, I>> :
    IObservationSource<SC, I> {

    final override val attributes: IAttributeDateSourceReferenceSet by lazy {
        val attributes =
            pendingAttributeReferences ?: throw IllegalStateException("Internal pending attribute storage broken")
        runWithExceptionCleanup({ pendingAttributeReferences = attributes }) {
            pendingAttributeReferences = null
            IAttributeDateSourceReferenceSet.Companion(attributes.values)
        }
    }

    private var pendingAttributeReferences: MutableMap<String, AttributeDataSource.Reference.ObservationSourceAttributeReference<*>>? =
        mutableMapOf()

    protected fun <T : Any> IAttributeKeyTypeInstance<T, *>.createObservationAttributeReference(name: String): AttributeDataSource.Reference.ObservationSourceAttributeReference<T> {
        val references = pendingAttributeReferences
            ?: throw IllegalStateException("Cannot create attribute references after attributes have already been accessed")
        val newValue = AttributeDataSource.Reference.ObservationSourceAttributeReference(
            this@ObservationSourceBase,
            name,
            this
        )
        val oldValue = references.putIfAbsent(name, newValue)
        if (oldValue != null) {
            throw IllegalArgumentException("Attribute reference with name $name already exists on ${this@ObservationSourceBase}: $oldValue.")
        }
        return newValue
    }

    abstract class InstanceBase<SC, out I : InstanceBase<SC, I>>(
        override val source: ObservationSourceBase<SC, out I>
    ) : IObservationSourceInstance<SC, MapAttributeStore, I> {

        context(sourceContext: SC)
        override fun createAttributeStore(parent: IAttributeValueStore): MapAttributeStore {
            return MapAttributeStore(attributes.references, parent)
        }
    }

    abstract class SingletonBase<SC, I : SingletonBase<SC, I>> : ObservationSourceBase<SC, SingletonBase<SC, I>>(),
        IObservationSourceSingleton<SC, MapAttributeStore, SingletonBase<SC, I>> {

        override val streamCodec: StreamCodec<RegistryFriendlyByteBuf, SingletonBase<SC, I>> = StreamCodec.unit(this)
        override fun fromNbt(tag: Tag?): SingletonBase<SC, I> = this
        override fun toNbt(instance: SingletonBase<SC, I>): Tag? = null

        context(sourceContext: SC)
        override fun createAttributeStore(parent: IAttributeValueStore): MapAttributeStore {
            return MapAttributeStore(attributes.references, parent)
        }
    }
}
