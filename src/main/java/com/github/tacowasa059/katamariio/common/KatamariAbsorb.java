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
import java.util.Arrays;
import java.util.List;

/**
 * Server-side block pickup. The ball periodically looks for solid blocks inside a sphere around its centre
 * (ball radius + a magnet margin), removes them from the world and hands them to the player data in ONE batch.
 * The player data decides where every block sits on the ball (see ICustomPlayerData#katamariIO$absorbBlocks).
 *
 * Optimisations compared to the first version:
 * - only the sphere is scanned (not the whole bounding cube), columns outside the sphere are skipped early;
 * - the scan interval grows with the ball size (1 / 2 / 4 ticks);
 * - candidates are sorted as primitive longs, no per-candidate objects;
 * - the inverse ball rotation is computed once per scan, not once per block;
 * - one batch call per scan means one list rebuild and one network sync instead of one per block.
 */
public final class KatamariAbsorb {
    /** Blocks eaten per tick on average; a scan every N ticks may eat N times this many. */
    private static final int MAX_PER_TICK = 60;
    private static final int MAX_PER_SCAN_CAP = 240;
    /** Hard safety cap for candidates collected in one scan. */
    private static final int MAX_CANDIDATES = 8000;
    /** Magnet margin around the ball (blocks) ... */
    private static final double REACH_BASE = 1.0;
    /** ... plus this fraction of the ball radius. */
    private static final double REACH_PER_RADIUS = 0.3;

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
        // Small balls scan every tick, big balls (expensive scan) less often.
        int interval = radius < 3.0f ? 1 : (radius < 6.0f ? 2 : 4);
        if (player.tickCount % interval != 0) {
            return;
        }
        int perScan = Math.min(MAX_PER_SCAN_CAP, MAX_PER_TICK * interval);

        Vec3 feet = player.position();
        Vec3 center = feet.add(0, radius, 0);
        double margin = REACH_BASE + REACH_PER_RADIUS * radius;
        double reach = radius + margin;
        double reachSq = reach * reach;
        AABB zone = player.getBoundingBox().inflate(margin);

        int minX = Mth.floor(zone.minX);
        int maxX = Mth.floor(zone.maxX);
        int minZ = Mth.floor(zone.minZ);
        int maxZ = Mth.floor(zone.maxZ);
        // Blocks below the feet are the ground the ball rolls on, they are never picked up.
        int minY = Math.max(Mth.floor(zone.minY), (int) Math.ceil(feet.y - 1.0E-3));
        int maxY = Mth.floor(zone.maxY);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long[] packed = new long[256];
        long[] sortKeys = new long[256];
        int found = 0;

        scan:
        for (int x = minX; x <= maxX; x++) {
            double dx = x + 0.5 - center.x;
            double dxSq = dx * dx;
            if (dxSq > reachSq) {
                continue;
            }
            for (int z = minZ; z <= maxZ; z++) {
                double dz = z + 0.5 - center.z;
                double dxzSq = dxSq + dz * dz;
                if (dxzSq > reachSq) {
                    continue;
                }
                double dyMax = Math.sqrt(reachSq - dxzSq);
                int yLo = Math.max(minY, Mth.ceil(center.y - dyMax - 0.5));
                int yHi = Math.min(maxY, Mth.floor(center.y + dyMax - 0.5));
                if (yLo > yHi) {
                    continue;
                }
                pos.set(x, yLo, z);
                if (!level.hasChunkAt(pos)) {
                    continue;
                }
                for (int y = yLo; y <= yHi; y++) {
                    pos.set(x, y, z);
                    if (!canAbsorb(level, pos, level.getBlockState(pos))) {
                        continue;
                    }
                    double dy = y + 0.5 - center.y;
                    double distSq = dxzSq + dy * dy;
                    if (distSq > reachSq) {
                        continue;
                    }
                    if (found == packed.length) {
                        packed = Arrays.copyOf(packed, found * 2);
                        sortKeys = Arrays.copyOf(sortKeys, found * 2);
                    }
                    packed[found] = pos.asLong();
                    // Positive floats order like their raw int bits: nearest block first.
                    sortKeys[found] = ((long) Float.floatToIntBits((float) distSq) << 32) | found;
                    found++;
                    if (found >= MAX_CANDIDATES) {
                        break scan;
                    }
                }
            }
        }

        if (found == 0) {
            return;
        }
        Arrays.sort(sortKeys, 0, found);

        Quaternionf inverseBallRotation = new Quaternionf(data.katamariIO$getQuaternion()).invert();
        int limit = Math.min(perScan, found);
        List<Block> eaten = new ArrayList<>(limit);
        List<Vec3> hits = new ArrayList<>(limit);
        Vector3f local = new Vector3f();

        for (int i = 0; i < limit; i++) {
            int index = (int) (sortKeys[i] & 0xFFFFFFFFL);
            BlockPos blockPos = BlockPos.of(packed[index]);
            BlockState state = level.getBlockState(blockPos);
            if (!canAbsorb(level, blockPos, state)) {
                continue;
            }
            Block block = state.getBlock();
            level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());

            Vec3 relativeWorld = Vec3.atCenterOf(blockPos).subtract(center);
            local.set((float) relativeWorld.x, (float) relativeWorld.y, (float) relativeWorld.z);
            local.rotate(inverseBallRotation);
            eaten.add(block);
            hits.add(new Vec3(local.x, local.y, local.z));
        }

        if (!eaten.isEmpty()) {
            data.katamariIO$absorbBlocks(eaten, hits);
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
}
