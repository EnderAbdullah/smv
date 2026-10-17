package com.enderabdullah.structuremover.entity;

import com.enderabdullah.structuremover.StructureMoverMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VehicleAssembler {
    public static final int MAX_BLOCKS = 1500;

    private VehicleAssembler() {
    }

    /** Terrain and unbreakable blocks are never picked up. */
    private static boolean canTake(ServerLevel level, BlockPos pos, BlockState s) {
        if (s.isAir()) return false;
        if (!s.getFluidState().isEmpty()) return false;
        if (s.hasBlockEntity()) return false;
        if (s.getDestroySpeed(level, pos) < 0) return false;
        if (s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER)
                || s.is(BlockTags.SAND) || s.is(BlockTags.SNOW) || s.is(BlockTags.LEAVES)
                || s.is(BlockTags.REPLACEABLE) || s.is(Blocks.GRAVEL)) {
            return false;
        }
        return true;
    }

    public static void assemble(ServerLevel level, BlockPos start, Player player) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> found = new ArrayList<>();
        int skipped = 0;

        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            found.add(pos);
            if (found.size() > MAX_BLOCKS) {
                player.displayClientMessage(Component.translatable("message.structuremover.too_big", MAX_BLOCKS), true);
                return;
            }
            for (Direction dir : Direction.values()) {
                BlockPos n = pos.relative(dir).immutable();
                if (!visited.add(n)) continue;
                BlockState s = level.getBlockState(n);
                if (s.hasBlockEntity()) {
                    skipped++;
                    continue;
                }
                if (canTake(level, n, s)) {
                    queue.add(n);
                }
            }
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : found) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxZ = Math.max(maxZ, p.getZ());
        }
        BlockPos anchor = new BlockPos(Math.floorDiv(minX + maxX + 1, 2), minY, Math.floorDiv(minZ + maxZ + 1, 2));

        List<VehicleEntity.Part> parts = new ArrayList<>();
        for (BlockPos p : found) {
            parts.add(new VehicleEntity.Part(p.subtract(anchor), level.getBlockState(p)));
        }

        float yaw = Math.round(player.getYRot() / 90.0f) * 90.0f;

        VehicleEntity vehicle = StructureMoverMod.VEHICLE.get().create(level);
        if (vehicle == null) return;
        vehicle.moveTo(anchor.getX(), anchor.getY(), anchor.getZ(), yaw, 0.0f);
        vehicle.setParts(parts, yaw);

        for (BlockPos p : found) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        level.addFreshEntity(vehicle);
        player.startRiding(vehicle);

        player.displayClientMessage(Component.translatable("message.structuremover.assembled", found.size()), true);
        if (skipped > 0) {
            player.sendSystemMessage(Component.translatable("message.structuremover.skipped", skipped));
        }
    }
}
