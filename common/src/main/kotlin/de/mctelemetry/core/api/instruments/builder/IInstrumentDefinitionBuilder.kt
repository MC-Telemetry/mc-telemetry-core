package de.mctelemetry.core.api.instruments.builder

import de.mctelemetry.core.api.attributes.MappedAttributeKeyInfo
import io.opentelemetry.api.common.AttributeKey
import org.jetbrains.annotations.Contract

interface IInstrumentDefinitionBuilder<out B : IInstrumentDefinitionBuilder<B>> {

    val name: String
    var attributes: List<MappedAttributeKeyInfo<*, *>>
    var description: String
    var unit: String

    @Contract("_ -> this", mutates = "this")
    fun addAttribute(attributeKey: MappedAttributeKeyInfo<*, *>): B {
        attributes += attributeKey
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun addAttribute(attributeKey: AttributeKey<*>): B {
        return addAttribute(MappedAttributeKeyInfo.fromNative(attributeKey))
    }

    @Contract("_ -> this", mutates = "this")
    fun addAttributes(attributeKeys: Collection<MappedAttributeKeyInfo<*, *>>): B {
        attributes += attributeKeys
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun addNativeAttributes(attributeKeys: Collection<AttributeKey<*>>): B {
        return addAttributes(attributeKeys.map { MappedAttributeKeyInfo.fromNative(it) })
    }

    @Contract("_ -> this", mutates = "this")
    fun withDescription(description: String): B {
        this.description = description
        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    @Contract("_ -> this", mutates = "this")
    fun withUnit(unit: String): B {
        this.unit = unit
        @Suppress("UNCHECKED_CAST")
        return this as B
    }
}
