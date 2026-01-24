package de.mctelemetry.core.api.attributes

import net.minecraft.network.RegistryFriendlyByteBuf

@JvmInline
value class MappedAttributeKeyValue<out T : Any, I : MappedAttributeKeyInfo<out T, *, *>>(val pair: Pair<I, T>) :
    Map.Entry<I, T> {

    constructor(info: I, value: T) : this(info to value)

    init {
        require(info.templateType.valueType.isAssignableFrom(value::class.java)) { "$value cannot be used as a value for $info" }
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
    }
}
