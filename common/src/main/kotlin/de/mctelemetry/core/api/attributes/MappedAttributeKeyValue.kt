package de.mctelemetry.core.api.attributes

import net.minecraft.network.RegistryFriendlyByteBuf

@JvmInline
value class MappedAttributeKeyValue<out T : Any, I : MappedAttributeKeyInfo<out T, *, *>>(val pair: Pair<I, T>) :
    Map.Entry<I, T> {

    constructor(info: I, value: T) : this(info to value)

    init {
        val valueType = info.templateType.valueType
        if (!valueType.isPrimitive)
            require(valueType.isAssignableFrom(value::class.java)) { "$value cannot be used as a value for $info" }
        else {
            val primitiveType = primitiveTypeMap[value::class.java]
            require(primitiveType != null && info.templateType.valueType.isAssignableFrom(primitiveType)) {
                "$value cannot be used as a value for $info"
            }
        }
    }

    operator fun component1(): I = pair.first
    operator fun component2(): T = pair.second

    val info: I
        get() = pair.first
    override val key: I
        get() = pair.first
    override val value: T
        get() = pair.second

    fun encodeValue(buf: RegistryFriendlyByteBuf) {
        @Suppress("UNCHECKED_CAST")
        (info as MappedAttributeKeyInfo<T, *, *>).templateType.valueStreamCodec.encode(buf, value)
    }

    companion object {
        fun <T : Any, I : IAttributeKeyTypeInstance<T, *, I>> MappedAttributeKeyInfo<T, *, I>.decodeToValue(buf: RegistryFriendlyByteBuf): MappedAttributeKeyValue<T, *> {
            val value = this.templateType.valueStreamCodec.decode(buf)

            return MappedAttributeKeyValue(this, value)
        }

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private val primitiveTypeMap: Map<Class<*>, Class<*>> = mapOf(
            java.lang.Boolean::class.java to Boolean::class.java,
            java.lang.Byte::class.java to Byte::class.java,
            java.lang.Short::class.java to Short::class.java,
            java.lang.Character::class.java to Char::class.java,
            java.lang.Integer::class.java to Int::class.java,
            java.lang.Long::class.java to Long::class.java,
            java.lang.Float::class.java to Float::class.java,
            java.lang.Double::class.java to Double::class.java,
        )
    }
}
