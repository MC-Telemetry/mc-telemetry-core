package de.mctelemetry.core.instruments.builtin

import de.mctelemetry.core.api.attributes.AttributeDataSource
import de.mctelemetry.core.api.attributes.IAttributeKeyTypeInstance
import de.mctelemetry.core.api.attributes.IMappedAttributeValueLookup
import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import de.mctelemetry.core.api.instruments.IInstrumentDefinition
import de.mctelemetry.core.api.observations.IObservationRecorder
import de.mctelemetry.core.observations.model.ObservationIdentityResolver
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class StaticInstrumentBase(final override val name: String) : IInstrumentDefinition {

    override val description: String = ""
    override val unit: String = ""


    final override val attributes: Map<String, MappedAttributeKeyInfo<*,*>> by lazy {
        slots.mapValues { it.value.info }
    }

    val slots: Map<String, AttributeDataSource.Reference.TypedSlot<*>>  by lazy {
        val slots = pendingAttributeSlots ?: throw IllegalStateException("Internal pending attribute storage broken")
        pendingAttributeSlots = null
        slots
    }

    private var pendingAttributeSlots: MutableMap<String, AttributeDataSource.Reference.TypedSlot<*>>? =
        mutableMapOf()

    protected open fun createAttributeStore(): IMappedAttributeValueLookup.Mutable {
        return IMappedAttributeValueLookup.MapLookup(
            slots.values,
        )
    }

    protected inline fun <T> withIdentityResolver(
        recorder: IObservationRecorder.Resolved,
        block: context(IMappedAttributeValueLookup.Mutable) (recorder: IObservationRecorder.Unresolved.Sourceless) -> T
    ): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val store = createAttributeStore()
        return context(store) {
            block(ObservationIdentityResolver(recorder))
        }
    }

    protected fun <T : Any> IAttributeKeyTypeInstance<T, *>.createAttributeSlot(name: String): AttributeDataSource.Reference.TypedSlot<T> {
        val slots = pendingAttributeSlots
            ?: throw IllegalStateException("Cannot create attribute slot references after attributes have already been accessed")
        val newValue = AttributeDataSource.Reference.TypedSlot(
            this.create(name)
        )
        val oldValue = slots.putIfAbsent(name, newValue)
        if (oldValue != null) {
            throw IllegalArgumentException("Attribute slot reference with name $name already exists on ${this@StaticInstrumentBase}: $oldValue.")
        }
        return newValue
    }
}
