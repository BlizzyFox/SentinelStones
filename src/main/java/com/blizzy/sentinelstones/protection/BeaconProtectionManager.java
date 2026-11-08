
package com.blizzy.sentinelstones.protection;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BeaconProtectionManager {

    /**
     * Get the pyramid tier of a beacon.
     *
     * A valid tier requires a complete pyramid layer.
     * The maximum tier is 4.
     */
    public static int getBeaconTier(
            ServerLevel level,
            BlockPos beaconPos
    ) {
        if (!level.getBlockState(beaconPos).is(net.minecraft.world.level.block.Blocks.BEACON)) {
            return 0;
        }
        int tier = 0;

        for (int layer = 1; layer <= 4; layer++) {
            boolean validLayer = true;

            for (int x = -layer; x <= layer && validLayer; x++) {
                for (int z = -layer; z <= layer; z++) {

                    BlockPos checkPos = beaconPos.offset(x, -layer, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (!state.is(BlockTags.BEACON_BASE_BLOCKS)) {
                        validLayer = false;
                        break;
                    }
                }
            }

            if (!validLayer) {
                break;
            }

            tier = layer;
        }

        return tier;
    }

    /**
     * Convert beacon tier into protection range.
     *
     * Tier 1 = 20 blocks
     * Tier 2 = 30 blocks
     * Tier 3 = 40 blocks
     * Tier 4 = 50 blocks
     */
    public static int getProtectionRange(int tier) {
        if (tier < 1 || tier > 4) {
            return 0;
        }

        return tier * 10 + 10;
    }

    /**
     * Get the protection range of a particular beacon.
     */
    public static int getProtectionRange(
            ServerLevel level,
            BlockPos beaconPos
    ) {
        return getProtectionRange(
                getBeaconTier(level, beaconPos)
        );
    }

    /**
     * Get the protection area of a beacon.
     *
     * Matches the vanilla beacon effect area's
     * horizontal range and vertical extension.
     */
    public static AABB getProtectionArea(
            ServerLevel level,
            BlockPos beaconPos
    ) {
        int range = getProtectionRange(level, beaconPos);

        return new AABB(beaconPos)
                .inflate(range)
                .expandTowards(0.0, level.getHeight(), 0.0);
    }

    /**
     * Check whether a beacon is bound to a settlement.
     */
    public static boolean isBeaconBound(
            ServerLevel level,
            BlockPos beaconPos
    ) {
        GlobalPos globalPos = GlobalPos.of(
                level.dimension(),
                beaconPos
        );

        return ProtectionSavedData.get(level)
                .getSettlementAtBeacon(globalPos) != null;
    }

    /**
     * Get the settlement associated with a beacon.
     */
    public static BeaconSettlement getSettlementAtBeacon(
            ServerLevel level,
            BlockPos beaconPos
    ) {
        GlobalPos globalPos = GlobalPos.of(
                level.dimension(),
                beaconPos
        );

        return ProtectionSavedData.get(level)
                .getSettlementAtBeacon(globalPos);
    }

    /**
     * Check whether a proposed beacon overlaps
     * another settlement's protection area.
     *
     * Pass null for joiningSettlementId when creating
     * a completely new settlement.
     */
    public static boolean hasOverlappingSettlement(
            ServerLevel level,
            BlockPos proposedPos,
            UUID joiningSettlementId
    ) {
        int proposedRange = getProtectionRange(level, proposedPos);

        if (proposedRange == 0) {
            return true;
        }

        AABB proposedArea = getProtectionArea(level, proposedPos);

        ProtectionSavedData data = ProtectionSavedData.get(level);

        for (BeaconSettlement settlement : data.getSettlements()) {

            // Beacons in the same settlement may overlap.
            if (settlement.getId().equals(joiningSettlementId)) {
                continue;
            }

            for (GlobalPos existingBeacon : settlement.getBeacons()) {

                // Different dimensions cannot overlap.
                if (!existingBeacon.dimension().equals(level.dimension())) {
                    continue;
                }

                BlockPos existingPos = existingBeacon.pos();

                // Ignore distant beacons before calculating their tier.
                if (Math.abs(existingPos.getX() - proposedPos.getX()) > 100
                        || Math.abs(existingPos.getZ() - proposedPos.getZ()) > 100) {
                    continue;
                }

                AABB existingArea = getProtectionArea(level, existingPos);

                if (proposedArea.intersects(existingArea)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find the settlement protecting a block position.
     */
    public static BeaconSettlement getSettlementAtLocation(
            ServerLevel level,
            BlockPos targetPos
    ) {
        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY() + 0.5;
        double z = targetPos.getZ() + 0.5;

        ProtectionSavedData data = ProtectionSavedData.get(level);

        for (BeaconSettlement settlement : data.getSettlements()) {

            for (GlobalPos beacon : settlement.getBeacons()) {

                if (!beacon.dimension().equals(level.dimension())) {
                    continue;
                }

                BlockPos beaconPos = beacon.pos();

                if (Math.abs(beaconPos.getX() - x) > 50
                        || Math.abs(beaconPos.getZ() - z) > 50) {
                    continue;
                }

                AABB area = getProtectionArea(level, beaconPos);

                if (area.contains(x, y, z)) {
                    return settlement;
                }
            }
        }

        return null;
    }

    /**
     * Check whether a player belongs to the settlement
     * protecting a particular position.
     */
    public static boolean isMemberAt(
            ServerLevel level,
            BlockPos pos,
            UUID player
    ) {
        BeaconSettlement settlement =
                getSettlementAtLocation(level, pos);

        return settlement != null && settlement.isMember(player);
    }
}
