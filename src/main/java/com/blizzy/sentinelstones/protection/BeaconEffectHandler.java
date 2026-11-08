
package com.blizzy.sentinelstones.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class BeaconEffectHandler {

    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only update twice per second.
        if (player.tickCount % 10 != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        int amplifier =
            BeaconProtectionManager.getMiningFatigueAmplifierAtLocation(
                level,
                pos,
                player.getUUID()
            );

        // No applicable protection. The effect will expire naturally.
        if (amplifier < 0) {
            return;
        }

        // Refresh the effect while the player remains in the area.
        player.addEffect(new MobEffectInstance(
            MobEffects.DIG_SLOWDOWN,
            30,
            amplifier,
            false,
            false,
            false
        ));
    }
}
