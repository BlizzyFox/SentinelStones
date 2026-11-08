package com.blizzy.sentinelstones.mixin.create;

import com.blizzy.sentinelstones.compat.create.CreateProtectionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour")
public abstract class BlockBreakingMovementBehaviourMixin {

    @Inject(
        method = "canBreak",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sentinelstones$preventProtectedBreaking(
            Level world,
            BlockPos breakingPos,
            BlockState state,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (world instanceof ServerLevel serverLevel
                && CreateProtectionManager.isProtected(serverLevel, breakingPos)) {

            cir.setReturnValue(false);
        }
    }
}