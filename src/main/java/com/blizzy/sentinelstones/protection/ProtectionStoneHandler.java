
package com.blizzy.sentinelstones.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;

import com.blizzy.sentinelstones.SentinelStones;
import com.blizzy.sentinelstones.block.ProtectionStoneBlock;

import java.util.UUID;

public class ProtectionStoneHandler {

    public static void handleInteraction(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos
    ) {

        ProtectionSavedData data = ProtectionSavedData.get(level);

        UUID playerId = player.getUUID();

        UUID existingOwner = data.getOwner(level, pos);

        // ---------------------------------
        // CASE 1: Stone is already bound
        // ---------------------------------

        if (existingOwner != null) {
            if (existingOwner.equals(playerId)) {
                data.removeClaim(level, pos);

                setBoundState(level, pos, false);

                level.sendParticles(
                        ParticleTypes.SMOKE,
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        12,
                        0.3, 0.3, 0.3,
                        0.02
                );

                player.displayClientMessage(
                        Component.literal("You release the stone."),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("This stone does not obey you."),
                        true
                );
            }

            return;
        }

        // ---------------------------------
        // CASE 2: Check overlapping stones
        // ---------------------------------

        if (data.hasOverlappingClaim(level, pos, playerId)) {

            player.displayClientMessage(
                    Component.literal(
                            "Another being's influence prevents you from commanding this stone."
                    ),
                    true
            );

            return;
        }

        // ---------------------------------
        // CASE 3: Bind the stone
        // ---------------------------------
        data.getPlayerStone(playerId).ifPresent(oldStone -> {
            ServerLevel oldLevel = level.getServer().getLevel(
                    oldStone.dimension()
            );

            if (oldLevel != null) {
                setBoundState(oldLevel, oldStone.pos(), false);
            }
        });

        data.bind(level, pos, playerId);

        setBoundState(level, pos, true);

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                15,
                0.4, 0.4, 0.4,
                0.05
        );

        player.displayClientMessage(
                Component.literal(
                        "The stone emits a reassuring energy in response to your touch."
                ),
                true
        );
    }

    private static void setBoundState(
        ServerLevel level,
        BlockPos pos,
        boolean bound
    ) {
        BlockState state = level.getBlockState(pos);

        if (state.is(SentinelStones.SENTINEL_STONE.get())) {
            level.setBlockAndUpdate(
                    pos,
                    state.setValue(ProtectionStoneBlock.BOUND, bound)
            );
        }
    }
}
