package de.mctelemetry.core.api.metrics

import net.minecraft.network.RegistryFriendlyByteBuf

@JvmInline
value class MappedAttributeKeyValue<out T : Any, I : MappedAttributeKeyInfo<out T, *>>(val pair: Pair<I, T>): Map.Entry<I,T> {

    constructor(info: I, value: T): this(info to value)

    init {
        require(info.type.valueType.isAssignableFrom(value::class.java)) {"$value cannot be used as a value for $info"}
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
        (info as MappedAttributeKeyInfo<T,*>).type.valueStreamCodec.encode(buf, value)
    }
    companion object {
        fun <T: Any> MappedAttributeKeyInfo<T,*>.decodeToValue(buf: RegistryFriendlyByteBuf): MappedAttributeKeyValue<T, *> {
            return MappedAttributeKeyValue(this, this.type.valueStreamCodec.decode(buf))
        }
    }
}
