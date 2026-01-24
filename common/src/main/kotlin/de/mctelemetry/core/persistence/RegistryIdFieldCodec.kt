package de.mctelemetry.core.persistence

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import de.mctelemetry.core.utils.component1
import de.mctelemetry.core.utils.component2
import de.mctelemetry.core.utils.findInDelegationChain
import net.minecraft.core.Registry
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

class RegistryIdFieldCodec<A>(val registry: ResourceKey<Registry<A>>, val idGetter: (A) -> ResourceLocation) :
    Codec<A> {

    companion object {
        inline operator fun <A> invoke(
            registry: ResourceKey<Registry<A>>,
            crossinline idGetter: (A) -> ResourceKey<A>
        ): RegistryIdFieldCodec<A> {
            return RegistryIdFieldCodec(registry) { idGetter(it).location() }
        }
    }

    override fun <T> decode(
        ops: DynamicOps<T>,
        input: T
    ): DataResult<Pair<A, T>> {
        val registryOps = ops.findInDelegationChain { it as? RegistryOps<*> }
        if (registryOps != null) {
            val getter = registryOps.getter(registry).getOrNull()
            if (getter != null) {
                return ResourceLocation.CODEC.decode(ops, input).flatMap { (id, node) ->
                    val reference = getter
                        .get(ResourceKey.create(registry, id))
                        .getOrElse { return@flatMap DataResult.error { "Failed to get element $id" } }
                    DataResult.success(Pair(reference.value(), node))
                }
            }
        }
        return DataResult.error { "Can't access registry $registry" }
    }

    override fun <T> encode(
        input: A,
        ops: DynamicOps<T>,
        prefix: T
    ): DataResult<T> {
        return ResourceLocation.CODEC.encode(idGetter(input), ops, prefix)
    }
}
