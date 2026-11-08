
package com.blizzy.sentinelstones.protection;

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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public class BeaconProtectionEvents {

    private static void deny(Player player) {
        player.displayClientMessage(
            Component.literal("Some kind of magic protects this place."),
            true
        );
    }

    // Prevent non-members from breaking a settlement's beacon.
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Player player = event.getPlayer();
        BlockPos pos = event.getPos();

        BeaconSettlement settlement =
            BeaconProtectionManager.getSettlementAtBeacon(level, pos);

        if (settlement == null) return;

        // Only settlement members can break bound beacons.
        if (!settlement.isMember(player.getUUID())) {
            event.setCanceled(true);
            deny(player);
            return;
        }

        // Remove the beacon from its settlement.
        if (event.getState().is(Blocks.BEACON)) {
            GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);

            ProtectionSavedData.get(level).removeBeacon(globalPos);

            player.sendSystemMessage(Component.literal(
                "Beacon removed from settlement."
            ));
        }
    }

    // Prevent non-members from placing griefing blocks.
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        BlockPos pos = event.getPos();

        BeaconSettlement settlement =
            BeaconProtectionManager.getSettlementAtLocation(level, pos);

        if (settlement == null || settlement.isMember(player.getUUID())) return;

        BlockState placedBlock = event.getPlacedBlock();

        if (isBlacklistedBlock(placedBlock)) {
            event.setCanceled(true);
            deny(player);
        }
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ProtectionSavedData data = ProtectionSavedData.get(level);

        for (BlockPos pos : event.getAffectedBlocks()) {

            // Only interested in beacons.
            if (!level.getBlockState(pos).is(Blocks.BEACON)) {
                continue;
            }

            GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);

            // Only remove beacons that belong to a settlement.
            if (data.getSettlementAtBeacon(globalPos) == null) {
                continue;
            }

            data.removeBeacon(globalPos);
        }
    }

    // Prevent non-members from accessing storage blocks.
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();

        BeaconSettlement settlement =
            BeaconProtectionManager.getSettlementAtLocation(level, pos);

        if (settlement == null || settlement.isMember(player.getUUID())) return;

        BlockState state = level.getBlockState(pos);

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
        return state.is(Blocks.TNT)
            || state.is(Blocks.PISTON)
            || state.is(Blocks.STICKY_PISTON)
            || state.is(Blocks.DISPENSER)
            || state.is(Blocks.DROPPER)
            || state.is(Blocks.REDSTONE_WIRE)
            || state.is(Blocks.REDSTONE_TORCH)
            || state.is(Blocks.REPEATER)
            || state.is(Blocks.COMPARATOR)
            || state.is(Blocks.RAIL)
            || state.is(Blocks.POWERED_RAIL)
            || state.is(Blocks.RESPAWN_ANCHOR);
    }

    private static boolean isAllowedWorkstation(BlockState state) {
        return state.is(Blocks.CRAFTING_TABLE)
            || state.is(Blocks.FURNACE)
            || state.is(Blocks.BLAST_FURNACE)
            || state.is(Blocks.SMOKER)
            || state.is(Blocks.SMITHING_TABLE)
            || state.is(Blocks.ANVIL)
            || state.is(Blocks.CHIPPED_ANVIL)
            || state.is(Blocks.DAMAGED_ANVIL)
            || state.is(Blocks.ENCHANTING_TABLE)
            || state.is(Blocks.STONECUTTER)
            || state.is(Blocks.LOOM)
            || state.is(Blocks.CARTOGRAPHY_TABLE)
            || state.is(Blocks.GRINDSTONE)
            || state.is(Blocks.BREWING_STAND);
    }
}
