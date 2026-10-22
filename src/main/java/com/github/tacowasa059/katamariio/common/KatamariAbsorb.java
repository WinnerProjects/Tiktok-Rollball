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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Server-side block pickup. Every tick the ball looks for solid blocks inside its whole hitbox (plus a magnet
 * margin) and glues them to its surface. Because the whole hitbox is covered, the ball never bumps into blocks
 * and keeps gliding. Does not depend on the client collision hook / packet.
 */
public final class KatamariAbsorb {
    /** Max blocks glued per scan. */
    private static final int MAX_PER_SCAN = 80;
    /** Candidates collected per scan before sorting (nearest are absorbed first). */
    private static final int MAX_CANDIDATES = 1500;
    /** Magnet margin around the hitbox (blocks) ... */
    private static final double REACH_BASE = 1.0;
    /** ... plus this fraction of the ball radius. */
    private static final double REACH_PER_RADIUS = 0.3;
    private static final int MAX_AXIS_EXTENT = 48;
    /** Attached blocks sit on the surface: block centre this far from the ball surface. */
    private static final double SURFACE_OFFSET = 0.5;

    private record Candidate(long pos, double distSq) {
    }

    private KatamariAbsorb() {
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide() || !player.isAlive() || player.isSpectator()) {
            return;
        }
        ICustomPlayerData data = (ICustomPlayerData) player;
        if (!data.katamariIO$getFlag()) {
            return;
        }

        float radius = data.katamariIO$getSize() / 2.0f;
        // Small balls scan every tick, big balls (expensive scan) every second tick.
        int interval = radius < 5.0f ? 1 : 2;
        if (player.tickCount % interval != 0) {
            return;
        }

        Vec3 feet = player.position();
        Vec3 center = feet.add(0, radius, 0);
        double margin = REACH_BASE + REACH_PER_RADIUS * radius;
        AABB zone = player.getBoundingBox().inflate(margin);

        int minX = Mth.floor(zone.minX);
        int maxX = Math.min(Mth.floor(zone.maxX), minX + MAX_AXIS_EXTENT);
        int minY = Mth.floor(zone.minY);
        int maxY = Math.min(Mth.floor(zone.maxY), minY + MAX_AXIS_EXTENT);
        int minZ = Mth.floor(zone.minZ);
        int maxZ = Math.min(Mth.floor(zone.maxZ), minZ + MAX_AXIS_EXTENT);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<Candidate> candidates = new ArrayList<>();

        scan:
        for (int y = minY; y <= maxY; y++) {
            // Blocks below the feet are the ground the ball rolls on, they are never picked up.
            if (y < feet.y - 1.0E-3) {
                continue;
            }
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    if (!canAbsorb(level, pos, level.getBlockState(pos))) {
                        continue;
                    }
                    double dx = x + 0.5 - center.x;
                    double dy = y + 0.5 - center.y;
                    double dz = z + 0.5 - center.z;
                    candidates.add(new Candidate(pos.asLong(), dx * dx + dy * dy + dz * dz));
                    if (candidates.size() >= MAX_CANDIDATES) {
                        break scan;
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return;
        }
        candidates.sort(Comparator.comparingDouble(Candidate::distSq));

        int limit = Math.min(MAX_PER_SCAN, candidates.size());
        for (int i = 0; i < limit; i++) {
            absorb(level, player, BlockPos.of(candidates.get(i).pos()), feet);
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

        // Blocks that are pulled in from a distance snap onto the ball's surface instead of floating around it.
        double maxDistance = radius + SURFACE_OFFSET;
        float distance = local.length();
        if (distance > maxDistance && distance > 1.0E-4f) {
            local.mul((float) (maxDistance / distance));
        }

        data.katamariIO$addBlock(block, inverseBallRotation, new Vec3(local.x, local.y, local.z));
        return true;
    }
}
