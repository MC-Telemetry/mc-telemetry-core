@file:Suppress("NOTHING_TO_INLINE", "WRONG_INVOCATION_KIND", "unused")
@file:OptIn(ExperimentalContracts::class)

package de.mctelemetry.core.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun GameTestHelper.assertBlockC(blockPos: BlockPos, message: String, noinline test: (Block)->Boolean) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
    }
    assertBlock(blockPos, test, message)
}

inline fun GameTestHelper.assertBlockC(blockPos: BlockPos, noinline message: ()->String, noinline test: (Block)->Boolean) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
        callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    assertBlock(blockPos, test, message)
}

inline fun <T : Comparable<T>> GameTestHelper.assertBlockPropertyC(
    blockPos: BlockPos,
    property: Property<T>,
    message: String,
    noinline test: (T) -> Boolean,
) {
    contract {
        callsInPlace(test, InvocationKind.AT_MOST_ONCE) // only invoked if block has property
    }
    assertBlockProperty(blockPos, property, test, message)
}

inline fun GameTestHelper.assertBlockStateC(
    blockPos: BlockPos,
    noinline message: () -> String,
    noinline test: (BlockState) -> Boolean,
) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
        callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    assertBlockState(blockPos, test, message)
}

inline fun GameTestHelper.assertBlockStateC(
    blockPos: BlockPos,
    message: String,
    noinline test: (BlockState) -> Boolean,
) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
    }
    assertBlockState(blockPos, test) { message }
}

inline fun <T : BlockEntity> GameTestHelper.assertBlockEntityDataC(
    blockPos: BlockPos,
    noinline message: () -> String,
    noinline test: (T) -> Boolean,
) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
        callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    assertBlockEntityData(blockPos, test, message)
}

inline fun <T : BlockEntity> GameTestHelper.assertBlockEntityDataC(
    blockPos: BlockPos,
    message: String,
    noinline test: (T) -> Boolean,
) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
    }
    assertBlockEntityData(blockPos, test) { message }
}

inline fun GameTestHelper.assertRedstoneSignalC(
    blockPos: BlockPos,
    direction: Direction,
    noinline message: () -> String,
    noinline test: (Int) -> Boolean,
) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
        callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    assertRedstoneSignal(blockPos, direction, test, message)
}

inline fun GameTestHelper.assertRedstoneSignalC(
    blockPos: BlockPos,
    direction: Direction,
    message: String,
    noinline test: (Int) -> Boolean,
) {
    contract {
        callsInPlace(test, InvocationKind.EXACTLY_ONCE)
    }
    assertRedstoneSignal(blockPos, direction, test) { message }
}

inline fun <E : Entity, T> GameTestHelper.assertEntityDataC(
    blockPos: BlockPos,
    entityType: EntityType<E>,
    expected: T,
    noinline function: (E) -> T,
) {
    contract {
        callsInPlace(function, InvocationKind.UNKNOWN)
    }
    assertEntityData(blockPos, entityType, function, expected)
}

inline fun <E : Entity> GameTestHelper.assertEntityPropertyC(
    entity: E,
    valueName: String,
    noinline function: (E) -> Boolean,
) {
    contract {
        callsInPlace(function, InvocationKind.EXACTLY_ONCE)
    }
    assertEntityProperty<E>(entity, function, valueName)
}

inline fun <E : Entity, T : Any> GameTestHelper.assertEntityPropertyC(
    entity: E,
    valueName: String,
    expected: T,
    noinline function: (E) -> T,
) {
    contract {
        callsInPlace(function, InvocationKind.EXACTLY_ONCE)
    }
    assertEntityProperty<E, T>(entity, function, valueName, expected)
}

inline fun GameTestHelper.failC(text: String, blockPos: BlockPos): Nothing =
    @Suppress("CAST_NEVER_SUCCEEDS")
    (fail(text, blockPos) as Nothing)

inline fun GameTestHelper.failC(text: String, entity: Entity): Nothing =
    @Suppress("CAST_NEVER_SUCCEEDS")
    (fail(text, entity) as Nothing)

inline fun GameTestHelper.failC(text: String): Nothing =
    @Suppress("CAST_NEVER_SUCCEEDS")
    (fail(text) as Nothing)

inline fun <N : Any> GameTestHelper.assertValueEqualC(expected: N, actual: N?, name: String) {
    contract {
        returns() implies (actual != null)
    }
    return assertValueEqual<N>(
        expected,
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        actual,
        name
    )
}

inline fun GameTestHelper.assertFalseC(value: Boolean, text: String) {
    contract {
        returns() implies !value
    }
    return assertFalse(value, text)
}
