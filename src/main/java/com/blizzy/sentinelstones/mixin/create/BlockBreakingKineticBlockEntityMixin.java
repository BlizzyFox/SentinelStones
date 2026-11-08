package com.blizzy.sentinelstones.mixin.create;

import com.blizzy.sentinelstones.compat.create.CreateProtectionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity")
public abstract class BlockBreakingKineticBlockEntityMixin {

    @Shadow
    protected BlockPos breakingPos;

    @Shadow
    public Level level;

    @Inject(
        method = "canBreak",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sentinelstones$preventProtectedBreaking(
            BlockState state,
            float hardness,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (level instanceof ServerLevel serverLevel
                && breakingPos != null
                && CreateProtectionManager.isProtected(serverLevel, breakingPos)) {

            cir.setReturnValue(false);
        }
    }
}