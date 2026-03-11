package de.mctelemetry.core.api.observations

import com.mojang.serialization.Codec
import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeDateSourceReferenceSet
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeInstance
import de.mctelemetry.core.observations.model.ObservationAttributeMapping
import de.mctelemetry.core.persistence.DirectUnitCodec
import de.mctelemetry.core.utils.EmptyAutoCloseable
import de.mctelemetry.core.utils.runWithExceptionCleanup
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.block.entity.BlockEntity

abstract class ObservationSourceBase<SO, I : IObservationSourceInstance<SO, *, I>> :
    IObservationSource<SO, I> {

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

    abstract class Simple<I : InstanceBase<BlockEntity, *, I>> : ObservationSourceBase<BlockEntity, I>() {
        final override val sourceOwnerType: Class<BlockEntity>
            get() = BlockEntity::class.java
    }

    abstract class InstanceBase<SO, OC : AutoCloseable, out I : InstanceBase<SO, OC, I>>(
        override val source: ObservationSourceBase<SO, out I>
    ) : IObservationSourceInstance<SO, OC, I> {

        abstract class Simple<out I : Simple<I>>(
            source: ObservationSourceBase<BlockEntity, out I>
        ) : InstanceBase<BlockEntity, EmptyAutoCloseable, I>(source) {
            context(sourceOwner: BlockEntity, mapping: ObservationAttributeMapping)
            final override fun createObservationContext(): EmptyAutoCloseable = EmptyAutoCloseable
        }
    }

    abstract class SingletonBase<SO, OC : AutoCloseable, I : SingletonBase<SO, OC, I>> : ObservationSourceBase<SO, I>(),
        IObservationSourceSingleton<SO, OC, I> {

        @Suppress("UNCHECKED_CAST")
        private val typedThis: I
            get() = this as I

        override val streamCodec: StreamCodec<in RegistryFriendlyByteBuf, I> = StreamCodec.unit(typedThis)
        override val codec: Codec<I> = DirectUnitCodec(typedThis)

        abstract class Simple<I : Simple<I>> :
            SingletonBase<BlockEntity, EmptyAutoCloseable, I>() {
            final override val sourceOwnerType: Class<BlockEntity>
                get() = BlockEntity::class.java

            context(sourceOwner: BlockEntity, mapping: ObservationAttributeMapping)
            final override fun createObservationContext(): EmptyAutoCloseable = EmptyAutoCloseable
        }
    }
}
