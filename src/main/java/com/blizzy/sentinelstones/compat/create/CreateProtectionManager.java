package com.blizzy.sentinelstones.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.blizzy.sentinelstones.protection.ProtectionManager;
import com.blizzy.sentinelstones.protection.BeaconProtectionManager;

public class CreateProtectionManager {

    public static boolean isProtected(
            ServerLevel level,
            BlockPos pos
    ) {
        // Personal Sentinel Stone claim.
        if (ProtectionManager.isCreateProtected(level, pos)) {
            return true;
        }

        // Bound beacon's immediate 3x3x3 area.
        return BeaconProtectionManager.isCreateProtected(level, pos);
    }
}