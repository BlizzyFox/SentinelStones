
package com.blizzy.sentinelstones.block;

import java.util.List;

import com.blizzy.sentinelstones.SentinelStones;
import com.blizzy.sentinelstones.protection.ProtectionSavedData;
import com.blizzy.sentinelstones.protection.ProtectionStoneHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

public class ProtectionStoneBlock extends Block {

    public static final BooleanProperty BOUND =
        BooleanProperty.create("bound");

    public ProtectionStoneBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
            this.stateDefinition.any().setValue(BOUND, false)
        );
    }

    // Prevent sentinel stones from being pushed by sticky pistons
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(BOUND);
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder builder
    ) {
        return List.of(new ItemStack(SentinelStones.SENTINEL_STONE.get()));
    }

    /**
     * Handle right-clicking the Protection Stone.
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            ProtectionStoneHandler.handleInteraction(
                    serverPlayer, serverLevel, pos
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Remove ownership when the physical block is destroyed.
     */
    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level instanceof ServerLevel serverLevel) {
            ProtectionSavedData.get(serverLevel).removeClaim(serverLevel, pos);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
