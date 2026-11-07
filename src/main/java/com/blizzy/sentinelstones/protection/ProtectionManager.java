package com.blizzy.sentinelstones.protection;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class ProtectionManager {

    public static boolean canModify(
            ServerLevel level,
            BlockPos pos,
            UUID playerId
    ) {
        UUID owner = ProtectionSavedData.get(level)
                .getOwnerAt(level, pos);

        // No protection here.
        if (owner == null) {
            return true;
        }

        // Only the owner can modify this area.
        return owner.equals(playerId);
    }
}