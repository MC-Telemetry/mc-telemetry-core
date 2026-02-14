package de.mctelemetry.core.mixin.api

import net.fabricmc.fabric.impl.lookup.block.BlockApiLookupImpl
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

interface BlockApiLookupWrapperMixinInterface<A, C> {
    fun `mcotelcore$registerWrapper`(wrapper: Function6<A, Level, BlockPos, BlockState, BlockEntity?, C, A>)
}

inline fun <A, C> BlockApiLookupImpl<A, C>.registerWrapper(crossinline wrapper: (A, Level, BlockPos, BlockState, BlockEntity?, C) -> A) {
    @Suppress("CAST_NEVER_SUCCEEDS")
    (this as BlockApiLookupWrapperMixinInterface<A, C>).`mcotelcore$registerWrapper` { previous, level, pos, state, entity, context ->
        wrapper(previous, level, pos, state, entity, context)
    }
}
