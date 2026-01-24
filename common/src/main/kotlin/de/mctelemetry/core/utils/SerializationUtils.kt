package de.mctelemetry.core.utils

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.DataResult
import com.mojang.serialization.Decoder
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.Encoder
import com.mojang.serialization.MapLike
import io.wispforest.endec.SerializationAttribute
import io.wispforest.endec.SerializationContext
import io.wispforest.owo.mixin.ForwardingDynamicOpsAccessor
import io.wispforest.owo.serialization.format.ContextHolder
import io.wispforest.owo.serialization.format.DynamicOpsWithContext
import net.minecraft.resources.DelegatingOps
import net.minecraft.resources.RegistryOps
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

operator fun <T> Pair<T, *>.component1(): T = first
operator fun <T> Pair<*, T>.component2(): T = second

@Suppress("UNCHECKED_CAST")
val <T> DelegatingOps<T>.delegate: DynamicOps<T>
    get() = (this as ForwardingDynamicOpsAccessor<T>).`owo$delegate`()

inline fun <T, R : Any> DynamicOps<T>.findInDelegationChain(block: (DelegatingOps<T>) -> R?): R? {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    var current = this
    while (current is DelegatingOps<T>) {
        val result = block(current)
        if (result != null) {
            return result
        } else {
            current = current.delegate
        }
    }
    return null
}

fun <T> DynamicOps<T>.injectAttributeIntoContext(
    attributeInstance: SerializationAttribute.Instance,
): DynamicOps<T> {
    val existingContext = findInDelegationChain { (it as? ContextHolder)?.capturedContext() }
    val newContext = if (existingContext == null) {
        SerializationContext.attributes(attributeInstance)
    } else {
        existingContext.withAttributes(attributeInstance)
    }
    return if (this is RegistryOps) {
        this.withParent(DynamicOpsWithContext.of(newContext, this.delegate))
    } else {
        DynamicOpsWithContext.of(newContext, this)
    }
}


fun <T, R> DynamicOps<T>.injectAttributeIntoContext(
    attribute: SerializationAttribute.WithValue<R>,
    value: R
): DynamicOps<T> = injectAttributeIntoContext(attribute.instance(value))

inline fun <R> DynamicOps<*>.getAttributeFromContext(
    attribute: SerializationAttribute.WithValue<R>,
    default: () -> R
): R {
    val context = findInDelegationChain {
        (it as? ContextHolder)?.capturedContext()
    } ?: return default()
    return if (context.hasAttribute(attribute)) {
        context.getAttributeValue(attribute)
    } else {
        default()
    }
}

@Suppress("UNCHECKED_CAST") // type is out-variance safe but not marked as such
fun <R : Any> DynamicOps<*>.getAttributeFromContext(
    attribute: SerializationAttribute.WithValue<R>
): R? = getAttributeFromContext(attribute as SerializationAttribute.WithValue<R?>) { null }

operator fun <T> SerializationAttribute.WithValue<T>.invoke(value: T): SerializationAttribute.Instance = instance(value)

@JvmName("addErrorToNullable")
fun DataResult<*>.addErrorTo(errors: MutableList<Supplier<String>>?): MutableList<Supplier<String>>? {
    return if (!isError) errors
    else (this as DataResult.Error<*>).addErrorTo(errors)
}

fun DataResult<*>.addErrorTo(errors: MutableList<Supplier<String>>): MutableList<Supplier<String>>? {
    return if (!isError) errors
    else (this as DataResult.Error<*>).addErrorTo(errors)
}

@JvmName("addErrorToNullable")
fun DataResult.Error<*>.addErrorTo(errors: MutableList<Supplier<String>>?): MutableList<Supplier<String>> {
    return if (errors == null)
        mutableListOf(messageSupplier)
    else {
        errors.add(messageSupplier)
        errors
    }
}


fun DataResult.Error<*>.addErrorTo(errors: MutableList<Supplier<String>>): MutableList<Supplier<String>> {
    errors.add(messageSupplier)
    return errors
}

fun mergeErrorMessages(errors: List<Supplier<String>>): Supplier<String> {
    if (errors.isEmpty())
        return Supplier { "<No errors provided>" }
    if (errors.size == 1)
        return errors[0]
    return Supplier {
        errors.joinToString(separator = "; ", transform = Supplier<String>::get)
    }
}

inline fun <T : R, R> DataResult<T>.resultOrPartialOrElse(
    onError: (DataResult.Error<T>) -> Unit = {},
    fallback: (DataResult.Error<T>) -> R
): R {
    contract {
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
        callsInPlace(fallback, InvocationKind.AT_MOST_ONCE)
    }
    if (isError) {
        this as DataResult.Error<T>
        onError(this)
        if (!hasResultOrPartial())
            return fallback(this)
    }
    return partialOrThrow
}

inline fun <T : R, R> DataResult<T>.resultOrElse(
    fallback: (DataResult.Error<T>) -> R
): R {
    contract {
        callsInPlace(fallback, InvocationKind.AT_MOST_ONCE)
    }
    if (isError) {
        this as DataResult.Error<T>
        return fallback(this)
    }
    return orThrow
}

inline fun <T> DataResult<T>.resultOrPartialOrNull(
    onError: (DataResult.Error<T>) -> Unit = {}
): T? {
    contract {
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return resultOrPartialOrElse { null }
}

fun <T> DataResult<T>.resultOrNull(): T? {
    return resultOrElse { null }
}


context(ops: DynamicOps<T>)
fun <T> T.withEntry(key: String, value: T, allowEmpty: Boolean = true): T =
    if (allowEmpty || value != ops.empty())
        ops.set(this, key, value)
    else
        this

context(ops: DynamicOps<T>)
fun <T : Any> T.withEntry(key: String, @Suppress("unused") value: Nothing?): T = ops.remove(this, key)

@JvmName("withEntryNullable")
context(ops: DynamicOps<T>)
fun <T : Any> T.withEntry(key: String, value: T?, allowEmpty: Boolean = true): T =
    if (value == null)
        ops.remove(this, key)
    else if (allowEmpty || value != ops.empty())
        ops.set(this, key, value)
    else
        this

context(ops: DynamicOps<T>)
fun <T> T.withEntry(key: T, value: T, allowEmpty: Boolean = true): T =
    if (allowEmpty || value != ops.empty())
        ops.mergeToMap(this, key, value).result().orElse(this)
    else
        this

context(ops: DynamicOps<T>)
fun <T, A> T.withEncodedEntry(key: String, value: A, encoder: Encoder<A>, allowEmpty: Boolean=true): DataResult<T> =
    if(allowEmpty){
        encoder.encodeStart(ops, value).flatMap { ops.mergeToMap(this, ops.createString(key), it) }
    } else {
        encoder.encodeStart(ops, value).flatMap {
            if(it != ops.empty())
                ops.mergeToMap(this, ops.createString(key), it)
            else
                DataResult.success(this)
        }
    }

context(ops: DynamicOps<T>)
fun <T : Any> T.withEncodedEntry(
    key: String,
    @Suppress("unused") value: Nothing?,
    @Suppress("unused") encoder: Encoder<*>
): DataResult<T> = DataResult.success(ops.remove(this, key))


@JvmName("withEncodedEntryNullable")
context(ops: DynamicOps<T>)
fun <T, A> T.withEncodedEntry(key: String, value: A?, encoder: Encoder<A>, allowEmpty: Boolean = true): DataResult<T> =
    if (value == null)
        DataResult.success(ops.remove(this, key))
    else
        withEncodedEntry(key, value, encoder, allowEmpty)


context(ops: DynamicOps<T>)
operator fun <T> T.get(key: String): DataResult<T> = ops.get(this, key)

context(ops: DynamicOps<T>)
operator fun <T> T.get(key: T): DataResult<T> = ops.getGeneric(this, key)

context(ops: DynamicOps<T>)
fun <T> T.getNumberValue(): DataResult<Number> = ops.getNumberValue(this)

context(ops: DynamicOps<T>)
fun <T> T.getNumberValue(defaultValue: Number): Number = ops.getNumberValue(this, defaultValue)

context(ops: DynamicOps<T>)
fun <T> T.getNumberValue(key: String): DataResult<Number> = this[key].flatMap { it.getNumberValue() }

context(ops: DynamicOps<T>)
fun <T> T.getNumberValue(key: String, defaultValue: Number): Number {
    val node = this[key]
    if (node.isError)
        return defaultValue
    return node.orThrow.getNumberValue(defaultValue)
}

context(ops: DynamicOps<T>)
fun <T, A> T.getParsedValue(decoder: Decoder<A>): DataResult<A> = decoder.parse(ops, this)

context(ops: DynamicOps<T>)
fun <T, A> T.getParsedValue(key: String, decoder: Decoder<A>): DataResult<A> =
    this[key].flatMap { decoder.parse(ops, it) }

context(ops: DynamicOps<T>)
fun <T> Number.asNumericDynamic(): T = ops.createNumeric(this)

context(ops: DynamicOps<T>)
fun <T> Byte.asByteDynamic(): T = ops.createByte(this)

context(ops: DynamicOps<T>)
fun <T> Short.asShortDynamic(): T = ops.createShort(this)

context(ops: DynamicOps<T>)
fun <T> Int.asIntDynamic(): T = ops.createInt(this)

context(ops: DynamicOps<T>)
fun <T> Long.asLongDynamic(): T = ops.createLong(this)

context(ops: DynamicOps<T>)
fun <T> Float.asFloatDynamic(): T = ops.createFloat(this)

context(ops: DynamicOps<T>)
fun <T> Double.asDoubleDynamic(): T = ops.createDouble(this)

context(ops: DynamicOps<T>)
fun <T> T.getBooleanValue(): DataResult<Boolean> = ops.getBooleanValue(this)

context(ops: DynamicOps<T>)
fun <T> T.getBooleanValue(key: String): DataResult<Boolean> = this[key].flatMap { it.getBooleanValue() }

context(ops: DynamicOps<T>)
fun <T> Boolean.asBooleanDynamic(): T = ops.createBoolean(this)

context(ops: DynamicOps<T>)
fun <T> T.getStringValue(): DataResult<String> = ops.getStringValue(this)

context(ops: DynamicOps<T>)
fun <T> T.getStringValue(key: String): DataResult<String> = this[key].flatMap { it.getStringValue() }

context(ops: DynamicOps<T>)
fun <T> String.asStringDynamic(): T = ops.createString(this)

context(ops: DynamicOps<T>)
infix fun <T> T.plusList(element: T): DataResult<T> = ops.mergeToList(this, element)

context(ops: DynamicOps<T>)
infix fun <T> T.plusList(list: List<T>): DataResult<T> = ops.mergeToList(this, list)

context(ops: DynamicOps<T>)
infix fun <T> T.plusMap(entry: Pair<T, T>): DataResult<T> = ops.mergeToMap(this, entry.first, entry.second)

context(ops: DynamicOps<T>)
infix fun <T> T.plusMap(entry: kotlin.Pair<T, T>): DataResult<T> = ops.mergeToMap(this, entry.first, entry.second)

context(ops: DynamicOps<T>)
fun <T> T.plusMap(key: T, value: T): DataResult<T> = ops.mergeToMap(this, key, value)

context(ops: DynamicOps<T>)
infix fun <T> T.plusMap(values: Map<T, T>): DataResult<T> = ops.mergeToMap(this, values)

context(ops: DynamicOps<T>)
infix fun <T> T.plusMap(values: MapLike<T>): DataResult<T> = ops.mergeToMap(this, values)

context(ops: DynamicOps<T>)
fun <T> T.getMapValues(): DataResult<Stream<Pair<T, T>>> = ops.getMapValues(this)

context(ops: DynamicOps<T>)
fun <T> T.getMapValues(key: String): DataResult<Stream<Pair<T, T>>> = this[key].flatMap { it.getMapValues() }

context(ops: DynamicOps<T>)
fun <T> T.getMapEntries(): DataResult<Consumer<BiConsumer<T, T>>> = ops.getMapEntries(this)

context(ops: DynamicOps<T>)
fun <T> T.getMapEntries(key: String): DataResult<Consumer<BiConsumer<T, T>>> = this[key].flatMap { it.getMapEntries() }

context(ops: DynamicOps<T>)
fun <T> Stream<Pair<T, T>>.asMapDynamic(): T = ops.createMap(this)

@JvmName("asMapDynamicFromKotlinPair")
context(ops: DynamicOps<T>)
fun <T> Stream<kotlin.Pair<T, T>>.asMapDynamic(): T = ops.createMap(this.map { Pair(it.first, it.second) })

context(ops: DynamicOps<T>)
fun <T> Map<T, T>.asMapDynamic(): T = ops.createMap(this)

context(ops: DynamicOps<T>)
fun <T> T.getMap(): DataResult<MapLike<T>> = ops.getMap(this)

context(ops: DynamicOps<T>)
fun <T> T.getMap(key: String): DataResult<MapLike<T>> = this[key].flatMap { it.getMap() }

context(ops: DynamicOps<T>)
fun <T> T.getStream(): DataResult<Stream<T>> = ops.getStream(this)

context(ops: DynamicOps<T>)
fun <T> T.getStream(key: String): DataResult<Stream<T>> = this[key].flatMap { it.getStream() }

context(ops: DynamicOps<T>)
inline fun <T> T.forEachStreamElement(
    onError: (DataResult.Error<Stream<T>>) -> Unit = {},
    partial: Boolean = true,
    block: (T) -> Unit
) {
    contract {
        callsInPlace(onError, InvocationKind.UNKNOWN)
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    val result = getStream()
    if (result.isError) {
        onError(result as DataResult.Error<Stream<T>>)
        if (!partial || !result.hasResultOrPartial()) return
    }
    for (element in result.resultOrPartial().get()) {
        block(element)
    }
}

context(ops: DynamicOps<T>)
inline fun <T> T.forEachMapEntry(
    onError: (DataResult.Error<Stream<Pair<T, T>>>) -> Unit = {},
    partial: Boolean = true,
    block: (T, T) -> Unit
) {
    contract {
        callsInPlace(onError, InvocationKind.UNKNOWN)
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    val result = getMapValues()
    if (result.isError) {
        onError(result as DataResult.Error<Stream<Pair<T, T>>>)
        if (!partial || !result.hasResultOrPartial()) return
    }
    for (element in result.resultOrPartial().get()) {
        block(element.first, element.second)
    }
}

context(ops: DynamicOps<T>)
fun <T> T.isEmptyMap(acceptPartial: Boolean = false): Boolean {
    val valuesDataResult = getMapValues()
    val stream = if (valuesDataResult.isSuccess || (acceptPartial && valuesDataResult.hasResultOrPartial())) {
        valuesDataResult.partialOrThrow
    } else {
        return false
    }
    return stream.findAny().isEmpty
}
