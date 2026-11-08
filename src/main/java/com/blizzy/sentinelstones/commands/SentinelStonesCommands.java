
package com.blizzy.sentinelstones.commands;

import java.util.UUID;

import com.blizzy.sentinelstones.protection.BeaconProtectionManager;
import com.blizzy.sentinelstones.protection.BeaconSettlement;
import com.blizzy.sentinelstones.protection.ProtectionSavedData;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class SentinelStonesCommands {

    public static void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
            Commands.literal("sentinelstones")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("test")

                    // Create a settlement at the targeted beacon
                    .then(Commands.literal("create")
                        .executes(ctx -> createSettlement(ctx.getSource().getPlayerOrException()))
                    )

                    // Add a player to the settlement
                    .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> addMember(
                                ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "player")
                            ))
                        )
                    )

                    // Remove a player
                    .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> removeMember(
                                ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "player")
                            ))
                        )
                    )

                    // Promote a member to officer
                    .then(Commands.literal("promote")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> promoteMember(
                                ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "player")
                            ))
                        )
                    )

                    // Demote an officer
                    .then(Commands.literal("demote")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> demoteMember(
                                ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "player")
                            ))
                        )
                    )

                    // Bind another beacon to an existing settlement
                    .then(Commands.literal("bind")
                        .then(Commands.argument("settlementId", StringArgumentType.word())
                            .executes(ctx -> bindBeacon(
                                ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "settlementId")
                            ))
                        )
                    )

                    // Display settlement information
                    .then(Commands.literal("info")
                        .executes(ctx -> showInfo(ctx.getSource().getPlayerOrException()))
                    )

                    // Delete the settlement associated with the targeted beacon
                    .then(Commands.literal("delete")
                        .executes(ctx -> deleteSettlement(ctx.getSource().getPlayerOrException()))
                    )
                )
        );
    }

    private static BlockPos getTargetedBeacon(ServerPlayer player) {
        HitResult hit = player.pick(8.0D, 0.0F, false);

        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("You must be looking at a beacon."));
            return null;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();

        if (!player.level().getBlockState(pos).is(Blocks.BEACON)) {
            player.sendSystemMessage(Component.literal("You must be looking at a beacon."));
            return null;
        }

        return pos;
    }

    private static int createSettlement(ServerPlayer player) {
        BlockPos pos = getTargetedBeacon(player);
        if (pos == null) return 0;

        ServerLevel level = player.serverLevel();
        ProtectionSavedData data = ProtectionSavedData.get(level);

        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);

        if (BeaconProtectionManager.getBeaconTier(level, pos) == 0) {
            player.sendSystemMessage(Component.literal(
                "This beacon does not have a valid pyramid."
            ));
            return 0;
        }

        if (data.getSettlementAtBeacon(globalPos) != null) {
            player.sendSystemMessage(Component.literal(
                "This beacon already belongs to a settlement."
            ));
            return 0;
        }

        if (BeaconProtectionManager.hasOverlappingSettlement(level, pos, null)) {
            player.sendSystemMessage(Component.literal(
                "This beacon overlaps another settlement."
            ));
            return 0;
        }

        var result = data.createSettlement(globalPos, player.getUUID());

        if (result.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                "Could not create settlement."
            ));
            return 0;
        }

        player.sendSystemMessage(Component.literal(
            "Settlement created! ID: " + result.get().getId()
        ));

        return 1;
    }

    private static int addMember(ServerPlayer player, ServerPlayer target) {
        BeaconSettlement settlement = getTargetedSettlement(player);
        if (settlement == null) return 0;

        boolean success = ProtectionSavedData.get(player.serverLevel())
            .addSettlementMember(
                settlement.getId(),
                player.getUUID(),
                target.getUUID()
            );

        player.sendSystemMessage(Component.literal(
            success
                ? "Added " + target.getName().getString() + " to the settlement."
                : "Could not add that player."
        ));

        return success ? 1 : 0;
    }

    private static int removeMember(ServerPlayer player, ServerPlayer target) {
        BeaconSettlement settlement = getTargetedSettlement(player);
        if (settlement == null) return 0;

        boolean success = ProtectionSavedData.get(player.serverLevel())
            .removeSettlementMember(
                settlement.getId(),
                player.getUUID(),
                target.getUUID()
            );

        player.sendSystemMessage(Component.literal(
            success
                ? "Removed " + target.getName().getString() + " from the settlement."
                : "Could not remove that player."
        ));

        return success ? 1 : 0;
    }

    private static int promoteMember(ServerPlayer player, ServerPlayer target) {
        BeaconSettlement settlement = getTargetedSettlement(player);
        if (settlement == null) return 0;

        boolean success = ProtectionSavedData.get(player.serverLevel())
            .promoteSettlementOfficer(
                settlement.getId(),
                player.getUUID(),
                target.getUUID()
            );

        player.sendSystemMessage(Component.literal(
            success
                ? "Promoted " + target.getName().getString() + " to Officer."
                : "Could not promote that player."
        ));

        return success ? 1 : 0;
    }

    private static int demoteMember(ServerPlayer player, ServerPlayer target) {
        BeaconSettlement settlement = getTargetedSettlement(player);
        if (settlement == null) return 0;

        boolean success = ProtectionSavedData.get(player.serverLevel())
            .demoteSettlementOfficer(
                settlement.getId(),
                player.getUUID(),
                target.getUUID()
            );

        player.sendSystemMessage(Component.literal(
            success
                ? "Demoted " + target.getName().getString() + " to Member."
                : "Could not demote that player."
        ));

        return success ? 1 : 0;
    }

    private static int bindBeacon(ServerPlayer player, String settlementIdString) {
        BlockPos pos = getTargetedBeacon(player);
        if (pos == null) return 0;

        UUID settlementId;

        try {
            settlementId = UUID.fromString(settlementIdString);
        } catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.literal("Invalid settlement ID."));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        ProtectionSavedData data = ProtectionSavedData.get(level);

        BeaconSettlement settlement = data.getSettlement(settlementId);

        if (settlement == null) {
            player.sendSystemMessage(Component.literal("Settlement not found."));
            return 0;
        }

        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);

        if (data.getSettlementAtBeacon(globalPos) != null) {
            player.sendSystemMessage(Component.literal(
                "This beacon is already bound."
            ));
            return 0;
        }

        if (BeaconProtectionManager.getBeaconTier(level, pos) == 0) {
            player.sendSystemMessage(Component.literal(
                "This beacon does not have a valid pyramid."
            ));
            return 0;
        }

        if (BeaconProtectionManager.hasOverlappingSettlement(
                level, pos, settlementId)) {
            player.sendSystemMessage(Component.literal(
                "This beacon overlaps another settlement."
            ));
            return 0;
        }

        boolean success = data.addBeaconToSettlement(globalPos, settlementId);

        player.sendSystemMessage(Component.literal(
            success ? "Beacon added to settlement!" : "Could not bind beacon."
        ));

        return success ? 1 : 0;
    }

    private static int showInfo(ServerPlayer player) {
        BeaconSettlement settlement = getTargetedSettlement(player);
        if (settlement == null) return 0;

        player.sendSystemMessage(Component.literal("----- Settlement Info -----"));
        player.sendSystemMessage(Component.literal("ID: " + settlement.getId()));
        player.sendSystemMessage(Component.literal("Founder: " + settlement.getFounder()));
        player.sendSystemMessage(Component.literal("Officers: " + settlement.getOfficers()));
        player.sendSystemMessage(Component.literal("Members: " + settlement.getMembers()));
        player.sendSystemMessage(Component.literal("Beacons: " + settlement.getBeacons().size()));

        return 1;
    }

    private static int deleteSettlement(ServerPlayer player) {
        BeaconSettlement settlement = getTargetedSettlement(player);
        if (settlement == null) return 0;

        boolean success = ProtectionSavedData.get(player.serverLevel())
            .deleteSettlement(settlement.getId());

        player.sendSystemMessage(Component.literal(
            success ? "Settlement deleted." : "Could not delete settlement."
        ));

        return success ? 1 : 0;
    }

    private static BeaconSettlement getTargetedSettlement(ServerPlayer player) {
        BlockPos pos = getTargetedBeacon(player);
        if (pos == null) return null;

        BeaconSettlement settlement = BeaconProtectionManager
            .getSettlementAtBeacon(player.serverLevel(), pos);

        if (settlement == null) {
            player.sendSystemMessage(Component.literal(
                "This beacon does not belong to a settlement."
            ));
        }

        return settlement;
    }
}
