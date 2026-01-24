package de.mctelemetry.core.api.attributes

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import io.netty.buffer.ByteBuf
import io.opentelemetry.api.common.AttributeKey
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

interface IAttributeKeyTypeInstance<T : Any, B : Any, I : IAttributeKeyTypeInstance<T, B, I>> {

    val templateType: IAttributeKeyTypeTemplate<T, B, I>

    fun create(key: AttributeKey<B>): MappedAttributeKeyInfo<T, B, I> {
        return MappedAttributeKeyInfo(
            baseKey = key,
            typeInstance = @Suppress("UNCHECKED_CAST") (this as I),
        )
    }

    fun create(name: String): MappedAttributeKeyInfo<T, B, I> {
        return create(
            NativeAttributeKeyTypes.attributeKeyForType(
                templateType.baseType,
                name
            )
        )
    }

    interface InstanceType<T : Any, B : Any, I : InstanceType<T, B, I>> : IAttributeKeyTypeInstance<T, B, I>,
        IAttributeKeyTypeTemplate<T, B, I>, Codec<I>, StreamCodec<ByteBuf, I> {

        override val templateType: InstanceType<T, B, I>
            get() = this

        override val typeInstanceCodec: Codec<I>
            get() = this

        override val typeInstanceStreamCodec: StreamCodec<in RegistryFriendlyByteBuf, I>
            get() = this

        override fun <T> decode(ops: DynamicOps<T>, input: T): DataResult<Pair<I, T>> {
            @Suppress("UNCHECKED_CAST")
            return DataResult.success(Pair(this as I, input))
        }

        override fun decode(`object`: ByteBuf): I {
            @Suppress("UNCHECKED_CAST")
            return this as I
        }

        override fun <T> encode(input: I, ops: DynamicOps<T>, prefix: T): DataResult<T> {
            return DataResult.success(ops.empty())
        }

        override fun encode(`object`: ByteBuf, object2: I) {}
    }
}

internal fun <I : IAttributeKeyTypeInstance<*, *, I>> IAttributeKeyTypeInstance<*, *, I>.encodeInstanceDetails(`object`: RegistryFriendlyByteBuf) {
    @Suppress("UNCHECKED_CAST")
    templateType.typeInstanceStreamCodec.encode(`object`, this as I)
}

context(ops: DynamicOps<T>)
internal fun <I : IAttributeKeyTypeInstance<*, *, I>, T> IAttributeKeyTypeInstance<*, *, I>.encodeInstanceDetails(prefix: T): DataResult<T> {
    @Suppress("UNCHECKED_CAST")
    return templateType.typeInstanceCodec.encode(this as I, ops, prefix)
}
