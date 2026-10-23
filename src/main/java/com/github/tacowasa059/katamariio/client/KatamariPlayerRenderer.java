package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.client.utils.SphereRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.List;

public final class KatamariPlayerRenderer {
    public static double MAX_SIZE = 0;
    /**
     * Blocks deeper than this below the outer shell are completely hidden by the shell, so they are not drawn.
     * (The server keeps them a bit longer so the surface never shows holes while the ball is growing.)
     */
    private static final float HIDDEN_DEPTH = 1.0f;
    /** Blocks of the outer layer are centred this far below the hitbox radius. */
    private static final float SHELL_INSET = 0.3f;
    /** How strongly a block's lattice depth moves it radially (keeps layers smooth while the ball grows). */
    private static final float DEPTH_FACTOR = 0.35f;
    /** Blocks are drawn slightly larger than 1 so neighbours overlap and no gaps show between them. */
    private static final float BLOCK_SCALE = 1.12f;
    private static final float HALF_PI = (float) (Math.PI / 2.0);

    private KatamariPlayerRenderer() {
    }

    private static final Quaternionf SPHERE_YAW = new Quaternionf(new AxisAngle4f((float) Math.PI / 4, 0, 1, 0));

    /**
     * Submits the ball (sphere + attached blocks) for a player. Called from the LivingEntityRenderer#submit hook.
     *
     * @return true if the vanilla player model must be skipped
     */
    public static boolean submit(AbstractClientPlayer player, Identifier texture, int packedLight,
                                 PoseStack poseStack, SubmitNodeCollector collector) {
        ICustomPlayerData playerData = (ICustomPlayerData) player;
        if (!playerData.katamariIO$getFlag()) {
            return false;
        }
        if (player.isInvisible()) {
            return true;
        }

        float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float collisionSize = playerData.katamariIO$getSize();
        // The drawn core is never smaller than the hitbox, otherwise the blocks would float around a tiny sphere.
        float renderSize = Math.max(playerData.katamariIO$getRenderSize(), collisionSize);
        Quaternionf quaternion = playerData.katamariIO$getInterpolatedQuaternion(partialTicks);

        poseStack.pushPose();
        poseStack.translate(0, collisionSize / 2.0, 0);
        if (player.getVehicle() == null) {
            poseStack.mulPose(quaternion);
        } else {
            poseStack.mulPose(QuaternionUtils.getQuaternionFromEntity(player));
        }

        int overlay = OverlayTexture.NO_OVERLAY;
        if (player.hurtTime > 0 || player.deathTime > 0) {
            overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true));
        }
        final int sphereOverlay = overlay;

        // Sphere: rotated 45 degrees around Y like the original implementation.
        poseStack.pushPose();
        poseStack.mulPose(SPHERE_YAW);
        final float radius = renderSize / 2.0f;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(texture),
                (pose, consumer) -> SphereRenderer.drawTexturedSphere(pose, consumer, radius, 12, 0, 0,
                        packedLight, sphereOverlay));
        poseStack.popPose();

        boolean isLocalPlayer = player.equals(Minecraft.getInstance().player);
        double tmpSize = renderSize;
        List<Vec3> vec3List = playerData.katamariIO$getSphericalPlayerPositions();
        List<Block> blockList = playerData.katamariIO$getSphericalPlayerBlocks();
        int lodStep = getLodStepForPlayer(player);
        int sharedSize = Math.min(vec3List.size(), blockList.size());

        float minLen = radius - HIDDEN_DEPTH;
        double minLenSq = minLen > 0.0f ? (double) minLen * minLen : -1.0;

        // The server stores every block in a cell of an integer lattice (guarantees a full shell without overlaps).
        // Here the cell is only used as a direction: the block is projected onto a perfectly round sphere and
        // turned so its face lies flat on the surface, so the silhouette is round instead of stair-stepped.
        final float shellRadius = radius - SHELL_INSET;
        Quaternionf tilt = new Quaternionf();
        boolean drewAny = false;

        for (int i = 0; i < sharedSize; i++) {
            Vec3 vec = vec3List.get(i);
            double lenSq = vec.lengthSqr();
            if (lenSq < 1.0E-6 || lenSq < minLenSq) {
                continue;
            }
            if (lodStep > 1 && !shouldRenderByHash(vec, i, player.getId(), lodStep)) {
                continue;
            }
            Block block = blockList.get(i);

            double len = Math.sqrt(lenSq);
            float nx = (float) (vec.x / len);
            float ny = (float) (vec.y / len);
            float nz = (float) (vec.z / len);

            int h = hashCell(vec);
            float roll = ((h >>> 2) & 1023) / 1024.0f * HALF_PI;
            float jitter = ((h >>> 12) & 15) / 15.0f * 0.1f;
            float placeRadius = shellRadius + DEPTH_FACTOR * (float) (len - radius) + jitter;

            tilt.rotationTo(0.0f, 1.0f, 0.0f, nx, ny, nz).rotateY(roll);

            poseStack.pushPose();
            poseStack.translate(nx * placeRadius, ny * placeRadius, nz * placeRadius);
            poseStack.mulPose(tilt);
            poseStack.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
            // A block model spans (0,0,0)..(1,1,1); centre it on the placed position.
            poseStack.translate(-0.5, -0.5, -0.5);
            collector.submitBlock(poseStack, block.defaultBlockState(), packedLight, overlay, 0);
            poseStack.popPose();
            drewAny = true;
        }

        if (isLocalPlayer && drewAny) {
            tmpSize = Math.max(tmpSize, 2.0 * (radius + 0.5));
        }

        if (isLocalPlayer) {
            MAX_SIZE = tmpSize;
        }

        poseStack.popPose();
        return true;
    }

    private static int hashCell(Vec3 vec) {
        int h = (int) Math.round(vec.x) * 73856093 ^ (int) Math.round(vec.y) * 19349663 ^ (int) Math.round(vec.z) * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h;
    }

    private static int getLodStepForPlayer(Player player) {
        double distanceSq = getCameraDistanceSq(player);
        if (distanceSq >= 256.0 * 256.0) return 8;
        if (distanceSq >= 128.0 * 128.0) return 4;
        if (distanceSq >= 64.0 * 64.0) return 2;
        return 1;
    }

    private static double getCameraDistanceSq(Player player) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return 0.0;
        }
        return camera.position().distanceToSqr(player.position());
    }

    private static boolean shouldRenderByHash(Vec3 vec, int index, int entityId, int step) {
        long x = Math.round(vec.x * 2.0);
        long y = Math.round(vec.y * 2.0);
        long z = Math.round(vec.z * 2.0);
        int hash = (int) (x * 73428767L ^ y * 912931L ^ z * 389129L ^ (long) index * 12289L ^ (long) entityId * 19997L);
        return Math.floorMod(hash, step) == 0;
    }
}
