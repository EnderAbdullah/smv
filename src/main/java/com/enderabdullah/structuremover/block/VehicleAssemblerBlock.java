package com.enderabdullah.structuremover.block;

import com.enderabdullah.structuremover.entity.VehicleAssembler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class VehicleAssemblerBlock extends Block {
    public VehicleAssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            VehicleAssembler.assemble(serverLevel, pos.immutable(), player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
