
package com.blizzy.sentinelstones.protection;

import com.blizzy.sentinelstones.Config;
import com.blizzy.sentinelstones.SentinelStones;
import com.blizzy.sentinelstones.protection.BeaconProtectionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import java.util.UUID;

public class BeaconProtectionEvents {

    private static void deny(Player player) {
        player.displayClientMessage(
            Component.literal("Some kind of magic protects this place."),
            true
        );
    }
    private static UUID getSentinelOwner(ServerLevel level, BlockPos pos) {
        return ProtectionSavedData.get(level).getOwnerAt(level, pos);
    }

    // Prevent non-members from breaking a settlement's beacon.
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Player player = event.getPlayer();
        BlockPos pos = event.getPos();

        // Sentinel Stone claims take priority over beacon settlements.
        UUID sentinelOwner = getSentinelOwner(level, pos);

        if (sentinelOwner != null) {
            // The Sentinel Stone owner is allowed to handle this block.
            // Other players will be rejected by Sentinel Stone protection.
            if (!sentinelOwner.equals(player.getUUID())) {
                deny(player);
                return;
            }

            // If this is a bound beacon, remove it from its settlement.
            if (event.getState().is(Blocks.BEACON)) {
                GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);

                if (ProtectionSavedData.get(level).getSettlementAtBeacon(globalPos) != null) {
                    ProtectionSavedData.get(level).removeBeacon(globalPos);

                    player.sendSystemMessage(Component.literal(
                        "Beacon removed from settlement."
                    ));
                }
            }

            return;
        }

        BeaconSettlement settlement =
            BeaconProtectionManager.getSettlementAtBeacon(level, pos);

        if (settlement == null) return;

        if (!settlement.isMember(player.getUUID())) {
            event.setCanceled(true);
            deny(player);
            return;
        }

        if (event.getState().is(Blocks.BEACON)) {
            GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);

            ProtectionSavedData.get(level).removeBeacon(globalPos);

            player.sendSystemMessage(Component.literal(
                "Beacon removed from settlement."
            ));
        }
    }

    // Prevent non-members from placing griefing blocks.
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        BlockPos pos = event.getPos();
        // Sentinel Stone claims take priority over beacon protection.
        if (getSentinelOwner(level, pos) != null) return;

        BeaconSettlement settlement =
            BeaconProtectionManager.getSettlementAtLocation(level, pos);

        if (settlement == null || settlement.isMember(player.getUUID())) return;

        BlockState placedBlock = event.getPlacedBlock();

        if (isBlacklistedBlock(placedBlock)) {
            event.setCanceled(true);
            deny(player);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ProtectionSavedData data = ProtectionSavedData.get(level);

        for (BlockPos pos : event.getAffectedBlocks()) {

            // Only interested in beacons.
            if (!level.getBlockState(pos).is(Blocks.BEACON)) {
                continue;
            }

            GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);

            // Only remove beacons that dontbelong to a settlement.
            if (data.getSettlementAtBeacon(globalPos) == null) {
                continue;
            }

            data.removeBeacon(globalPos);
        }
    }

    // Prevent non-members from accessing storage blocks.
    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();

        BeaconSettlement settlement =
            BeaconProtectionManager.getSettlementAtLocation(level, pos);

        if (settlement == null || settlement.isMember(player.getUUID())) return;

        BlockState state = level.getBlockState(pos);
        // Sentinel Stone claims take priority over beacon protection.
        if (getSentinelOwner(level, pos) != null) return;

        // Allow workstations and beacon interaction.
        if (isAllowedWorkstation(state) || state.is(Blocks.BEACON)) {
            return;
        }

        // Deny access to other blocks with block entities (chests,
        // barrels, shulker boxes, furnaces not on the whitelist, etc.).
        if (state.hasBlockEntity()) {
            event.setCanceled(true);
            deny(player);
        }
    }

    private static boolean isBlacklistedBlock(BlockState state) {
        return Config.blacklistedBlocksCache.contains(state.getBlock());
    }

    private static boolean isAllowedWorkstation(BlockState state) {
        return Config.allowedWorkstationsCache.contains(state.getBlock());
    }

}
