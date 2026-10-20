package com.github.tacowasa059.katamariio.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Server-side block pickup. Every few ticks the ball looks for solid blocks around it and glues them to its surface.
 * This does not depend on the client collision hook / packet, so it keeps working whatever the client does.
 */
public final class KatamariAbsorb {
    /** Scan every N ticks. */
    private static final int SCAN_INTERVAL = 2;
    /** Max blocks glued per scan, keeps big balls from lagging the server. */
    private static final int MAX_PER_SCAN = 16;
    /** Blocks stick slightly before they actually touch the ball. */
    private static final double REACH = 0.3;
    private static final int MAX_SCAN_RADIUS = 16;

    private KatamariAbsorb() {
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide() || !player.isAlive() || player.isSpectator()) {
            return;
        }
        ICustomPlayerData data = (ICustomPlayerData) player;
        if (!data.katamariIO$getFlag() || player.tickCount % SCAN_INTERVAL != 0) {
            return;
        }

        Vec3 feet = player.position();
        float radius = data.katamariIO$getSize() / 2.0f;
        Vec3 center = feet.add(0, radius, 0);
        double reach = radius + REACH;
        int range = Math.min(MAX_SCAN_RADIUS, (int) Math.ceil(reach) + 1);

        int cx = Mth.floor(center.x);
        int cy = Mth.floor(center.y);
        int cz = Mth.floor(center.z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int absorbed = 0;

        for (int dy = -range; dy <= range && absorbed < MAX_PER_SCAN; dy++) {
            int y = cy + dy;
            // Blocks below the feet are the ground the ball rolls on, they are never picked up.
            if (y < feet.y - 1.0E-3) {
                continue;
            }
            for (int dx = -range; dx <= range && absorbed < MAX_PER_SCAN; dx++) {
                for (int dz = -range; dz <= range && absorbed < MAX_PER_SCAN; dz++) {
                    pos.set(cx + dx, y, cz + dz);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (!canAbsorb(level, pos, state)) {
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
                    Optional<Vec3> closest = shape.closestPointTo(center);
                    if (closest.isEmpty() || closest.get().distanceTo(center) > reach) {
                        continue;
                    }

                    if (absorb(level, player, pos.immutable(), feet)) {
                        absorbed++;
                    }
                }
            }
        }
    }

    private static boolean canAbsorb(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /** Removes the block from the world and attaches it to the ball. */
    public static boolean absorb(Level level, Player player, BlockPos pos, Vec3 feetPos) {
        ICustomPlayerData data = (ICustomPlayerData) player;
        if (!data.katamariIO$getFlag()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        Block block = state.getBlock();

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.removeBlockEntity(pos);

        float radius = data.katamariIO$getSize() / 2.0f;
        Vec3 center = feetPos.add(0, radius, 0);
        Vec3 relativeWorld = Vec3.atCenterOf(pos).subtract(center);
        Quaternionf inverseBallRotation = new Quaternionf(data.katamariIO$getQuaternion()).invert();
        Vector3f local = new Vector3f((float) relativeWorld.x, (float) relativeWorld.y, (float) relativeWorld.z);
        local.rotate(inverseBallRotation);

        data.katamariIO$addBlock(block, inverseBallRotation, new Vec3(local.x, local.y, local.z));
        return true;
    }
}
