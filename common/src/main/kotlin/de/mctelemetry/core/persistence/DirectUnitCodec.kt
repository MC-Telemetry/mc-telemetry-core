package de.mctelemetry.core.persistence

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec

abstract class DirectUnitCodec<A> : Codec<A>, StreamCodec<ByteBuf, A> {
    abstract val instance: A

    override fun <T> encode(
        input: A,
        ops: DynamicOps<T>,
        prefix: T
    ): DataResult<T> {
        return DataResult.success(prefix)
    }

    override fun encode(`object`: ByteBuf, object2: A) {}

    override fun <T> decode(
        ops: DynamicOps<T>,
        input: T
    ): DataResult<Pair<A, T>> {
        return DataResult.success(Pair(instance, input))
    }

    override fun decode(`object`: ByteBuf): A {
        return instance
    }

    companion object {
        operator fun <A> invoke(instance: A): Constant<A> = Constant(instance)
        operator fun <A> invoke(block: () -> A): Lazy<A> = Lazy(block)
    }

    class Constant<A>(override val instance: A): DirectUnitCodec<A>()
    class Lazy<A>(val getter: ()->A): DirectUnitCodec<A>() {
        override val instance: A by lazy { getter() }
    }
}
