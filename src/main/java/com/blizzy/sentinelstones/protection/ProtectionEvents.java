package com.blizzy.sentinelstones.protection;

import com.blizzy.sentinelstones.protection.ProtectionManager;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity.Server;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(
        modid = "sentinelstones"
        //bus = EventBusSubscriber.Bus.GAME
)
public class ProtectionEvents {

    private static UUID getSentinelOwner(ServerLevel level, BlockPos pos) {
        return ProtectionSavedData.get(level).getOwnerAt(level, pos);
    }

    private static final Component DENIED_MESSAGE =
            Component.literal("This area is protected!");

    private static void deny(ServerPlayer player) {
        player.displayClientMessage(DENIED_MESSAGE, true);
    }

    // Prevent breaking blocks.
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!ProtectionManager.canModify(
                level,
                event.getPos(),
                player.getUUID()
        )) {
            event.setCanceled(true);
            deny(player);
        }
    }

    // Prevent placing blocks.
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ProtectionManager.canModify(
                level,
                event.getPos(),
                player.getUUID()
        )) {
            event.setCanceled(true);
            deny(player);
        }
    }

    // Prevent interacting with protected blocks.
    @SubscribeEvent
    public static void onBlockInteract(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ProtectionManager.canModify(
                level,
                event.getPos(),
                player.getUUID()
        )) {
            event.setCanceled(true);
            deny(player);
        }
    }

    //Prevent explosions from destorying protected blocks.
    @SubscribeEvent 
    public static void onExplosionDetonate(ExplosionEvent.Detonate event){
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        event.getAffectedBlocks().removeIf(pos -> {
                UUID sentinelOwner = getSentinelOwner(level, pos);
                
                if(sentinelOwner != null) return true;
                return false;
        });

        /*for (BlockPos pos : event.getAffectedBlocks()) {
            
            UUID sentinelOwner = getSentinelOwner(level, pos);
            
        }*/
    }

}