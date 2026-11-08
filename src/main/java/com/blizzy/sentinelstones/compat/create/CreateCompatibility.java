package com.blizzy.sentinelstones.compat.create;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;

import net.minecraft.server.level.ServerLevel;

public class CreateCompatibility {

    public static void register() {

        BlockMovementChecks.registerMovementAllowedCheck(
            (state, world, pos) -> {

                if (!(world instanceof ServerLevel level)) {
                    return CheckResult.PASS;
                }

                if (CreateProtectionManager.isProtected(level, pos)) {
                    return CheckResult.FAIL;
                }

                return CheckResult.PASS;
            }
        );
    }
}