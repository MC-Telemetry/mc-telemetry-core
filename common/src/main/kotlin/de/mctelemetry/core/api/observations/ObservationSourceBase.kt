package de.mctelemetry.core.api.observations

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeInstance
import de.mctelemetry.core.api.attributes.IAttributeValueStore
import de.mctelemetry.core.persistence.DirectUnitCodec
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

abstract class ObservationSourceBase<SC, I : IObservationSourceInstance<SC, IAttributeValueStore.MapAttributeStore, I>> :
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

    protected fun <T : Any> IAttributeKeyTypeInstance<T, *, *>.createObservationAttributeReference(name: String): AttributeDataSource.Reference.ObservationSourceAttributeReference<T> {
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
    ) : IObservationSourceInstance<SC, IAttributeValueStore.MapAttributeStore, I> {

        context(sourceContext: SC)
        override fun createAttributeStore(parent: IAttributeValueStore): IAttributeValueStore.MapAttributeStore {
            return IAttributeValueStore.MapAttributeStore(attributes.references, parent)
        }
    }

    abstract class SingletonBase<SC, I : SingletonBase<SC, I>> : ObservationSourceBase<SC, I>(),
        IObservationSourceSingleton<SC, IAttributeValueStore.MapAttributeStore, I> {

        @Suppress("UNCHECKED_CAST")
        private val typedThis: I
            get() = this as I

        override val streamCodec: StreamCodec<RegistryFriendlyByteBuf, I> = StreamCodec.unit(typedThis)
        override val codec: Codec<I> = DirectUnitCodec(typedThis)

        context(sourceContext: SC)
        override fun createAttributeStore(parent: IAttributeValueStore): IAttributeValueStore.MapAttributeStore {
            return IAttributeValueStore.MapAttributeStore(attributes.references, parent)
        }
    }
}
