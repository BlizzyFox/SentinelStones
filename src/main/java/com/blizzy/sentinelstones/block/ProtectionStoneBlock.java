
package com.blizzy.sentinelstones.block;

import com.blizzy.sentinelstones.protection.ProtectionSavedData;
import com.blizzy.sentinelstones.protection.ProtectionStoneHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;

public class ProtectionStoneBlock extends Block {

    public static final BooleanProperty BOUND =
        BooleanProperty.create("bound");

    public ProtectionStoneBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
            this.stateDefinition.any().setValue(BOUND, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(BOUND);
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
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {

        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel serverLevel) {

            ProtectionSavedData.get(serverLevel)
                    .removeClaim(serverLevel, pos);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
