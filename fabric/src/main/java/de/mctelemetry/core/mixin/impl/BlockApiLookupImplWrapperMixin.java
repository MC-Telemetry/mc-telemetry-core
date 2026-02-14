package de.mctelemetry.core.mixin.impl;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.mctelemetry.core.mixin.api.BlockApiLookupWrapperMixinInterface;
import kotlin.jvm.functions.Function6;
import net.fabricmc.fabric.impl.lookup.block.BlockApiLookupImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(BlockApiLookupImpl.class)
public abstract class BlockApiLookupImplWrapperMixin<A, C> implements BlockApiLookupWrapperMixinInterface<A, C> {

    @Unique
    private List<Function6<? super A, ? super @org.jetbrains.annotations.NotNull Level, ? super @org.jetbrains.annotations.NotNull BlockPos, ? super @org.jetbrains.annotations.NotNull BlockState, ? super @org.jetbrains.annotations.Nullable BlockEntity, ? super C, ? extends A>> wrappers;

    @ModifyReturnValue(method = "find", at = @At("RETURN"))
    private A wrapWithWrappers(A original,
                               @Local(argsOnly = true, type = Level.class) Level level,
                               @Local(argsOnly = true, type = BlockPos.class) BlockPos pos,
                               @Local(argsOnly = true, type = BlockState.class) BlockState state,
                               @Local(argsOnly = true, type = BlockEntity.class) BlockEntity blockEntity,
                               @Local(argsOnly = true, type = Object.class) C context
    ) {
        if (original == null) {
            return null;
        }
        A result = original;
        for (var wrapper : wrappers) {
            result = wrapper.invoke(result, level, pos, state, blockEntity, context);
        }
        return result;
    }

    public void mcotelcore$registerWrapper(
            @org.jetbrains.annotations.NotNull Function6<
                    ? super A,
                    ? super @org.jetbrains.annotations.NotNull Level,
                    ? super @org.jetbrains.annotations.NotNull BlockPos,
                    ? super @org.jetbrains.annotations.NotNull BlockState,
                    ? super @org.jetbrains.annotations.Nullable BlockEntity,
                    ? super C,
                    ? extends A> wrapper) {
        if (wrappers == null) {
            wrappers = new ArrayList<>();
        }
        wrappers.add(wrapper);
    }
}
