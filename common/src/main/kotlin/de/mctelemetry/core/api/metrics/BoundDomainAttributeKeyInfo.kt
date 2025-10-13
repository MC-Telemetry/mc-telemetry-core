package de.mctelemetry.core.api.metrics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType

sealed class BoundDomainAttributeKeyInfo<T>(
    val attributeKey: AttributeKey<T>,
) {

    companion object {
        fun <T> ofBuiltin(key: AttributeKey<T>): BoundDomainAttributeKeyInfo<T> {
            @Suppress("UNCHECKED_CAST")
            return when(key.type) {
                AttributeType.STRING -> StringKeyInfo(key as AttributeKey<String>)
                AttributeType.BOOLEAN -> BooleanKeyInfo(key as AttributeKey<Boolean>)
                AttributeType.LONG -> LongKeyInfo(key as AttributeKey<Long>)
                AttributeType.DOUBLE -> DoubleKeyInfo(key as AttributeKey<Double>)
                AttributeType.STRING_ARRAY -> StringArrayKeyInfo(key as AttributeKey<List<String>>)
                AttributeType.BOOLEAN_ARRAY -> BooleanArrayKeyInfo(key as AttributeKey<List<Boolean>>)
                AttributeType.LONG_ARRAY -> LongArrayKeyInfo(key as AttributeKey<List<Long>>)
                AttributeType.DOUBLE_ARRAY -> DoubleArrayKeyInfo(key as AttributeKey<List<Double>>)
            } as BoundDomainAttributeKeyInfo<T>
        }
    }

    class StringKeyInfo(attributeKey: AttributeKey<String>) :
            BoundDomainAttributeKeyInfo<String>(attributeKey) {

        constructor(name: String) : this(AttributeKey.stringKey(name))
    }

    class StringArrayKeyInfo(attributeKey: AttributeKey<List<String>>) :
            BoundDomainAttributeKeyInfo<List<String>>(attributeKey) {

        constructor(name: String) : this(AttributeKey.stringArrayKey(name))
    }

    class LongKeyInfo(attributeKey: AttributeKey<Long>) :
            BoundDomainAttributeKeyInfo<Long>(attributeKey) {

        constructor(name: String) : this(AttributeKey.longKey(name))
    }

    class LongArrayKeyInfo(attributeKey: AttributeKey<List<Long>>) :
            BoundDomainAttributeKeyInfo<List<Long>>(attributeKey) {

        constructor(name: String) : this(AttributeKey.longArrayKey(name))
    }

    class DoubleKeyInfo(attributeKey: AttributeKey<Double>) :
            BoundDomainAttributeKeyInfo<Double>(attributeKey) {

        constructor(name: String) : this(AttributeKey.doubleKey(name))
    }

    class DoubleArrayKeyInfo(attributeKey: AttributeKey<List<Double>>) :
            BoundDomainAttributeKeyInfo<List<Double>>(attributeKey) {

        constructor(name: String) : this(AttributeKey.doubleArrayKey(name))
    }

    class BooleanKeyInfo(attributeKey: AttributeKey<Boolean>) :
            BoundDomainAttributeKeyInfo<Boolean>(attributeKey) {

        constructor(name: String) : this(AttributeKey.booleanKey(name))
    }

    class BooleanArrayKeyInfo(attributeKey: AttributeKey<List<Boolean>>) :
            BoundDomainAttributeKeyInfo<List<Boolean>>(attributeKey) {

        constructor(name: String) : this(AttributeKey.booleanArrayKey(name))
    }

    class DomainKeyInfo<T>(attributeKey: AttributeKey<T>, val type: Class<T>) : BoundDomainAttributeKeyInfo<T>(attributeKey) {
        //TODO: implement DomainAttributeKey
        //constructor(name: String, type: Class<T>): this(AttributeKey.stringKey(name), type)
    }
}
