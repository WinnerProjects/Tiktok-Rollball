package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.client.utils.SphereRenderer;
import com.github.tacowasa059.katamariio.common.KatamariShell;
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
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.List;

public final class KatamariPlayerRenderer {
    public static double MAX_SIZE = 0;
    /** Blocks are drawn slightly larger than 1 so neighbours overlap and no gaps show between them. */
    private static final float BLOCK_SCALE = 1.12f;
    /** Tiny per-block height difference so overlapping neighbours never z-fight. */
    private static final float MAX_JITTER = 0.05f;
    private static final float HALF_PI = (float) (Math.PI / 2.0);

    private KatamariPlayerRenderer() {
    }

    private static final Quaternionf SPHERE_YAW = new Quaternionf(new AxisAngle4f((float) Math.PI / 4, 0, 1, 0));

    /**
     * Submits the ball for a player: a core sphere of FIXED size plus one complete layer of blocks that sits on a
     * perfectly round sphere around it (the layer grows with the hitbox, the core does not).
     * Called from the LivingEntityRenderer#submit hook.
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
        // The core never grows; it only shrinks if the hitbox itself is made smaller than the core.
        final float coreRadius = Math.min(KatamariShell.CORE_RADIUS, collisionSize / 2.0f);
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

        // Core sphere: rotated 45 degrees around Y like the original implementation.
        poseStack.pushPose();
        poseStack.mulPose(SPHERE_YAW);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(texture),
                (pose, consumer) -> SphereRenderer.drawTexturedSphere(pose, consumer, coreRadius, 12, 0, 0,
                        packedLight, sphereOverlay));
        poseStack.popPose();

        boolean isLocalPlayer = player.equals(Minecraft.getInstance().player);
        List<Block> blockList = playerData.katamariIO$getSphericalPlayerBlocks();
        int slots = blockList.size();
        int lodStep = getLodStepForPlayer(player);
        float blockScale = lodStep > 1 ? BLOCK_SCALE * (float) Math.sqrt(lodStep) : BLOCK_SCALE;

        // One block per slot of an evenly spaced (Fibonacci) sphere. Each block lies flat on the round surface,
        // so the silhouette is a true sphere and there are no holes between the blocks.
        float[] dirs = KatamariShell.directions(slots);
        final float shellRadius = KatamariShell.shellRadius(collisionSize);
        Quaternionf tilt = new Quaternionf();

        for (int i = 0; i < slots; i++) {
            if (lodStep > 1 && i % lodStep != 0) {
                continue;
            }
            float nx = dirs[3 * i];
            float ny = dirs[3 * i + 1];
            float nz = dirs[3 * i + 2];

            int h = hashSlot(i);
            float roll = ((h >>> 2) & 1023) / 1024.0f * HALF_PI;
            float jitter = ((h >>> 12) & 15) / 15.0f * MAX_JITTER;
            float placeRadius = shellRadius + jitter;

            tilt.rotationTo(0.0f, 1.0f, 0.0f, nx, ny, nz).rotateY(roll);

            poseStack.pushPose();
            poseStack.translate(nx * placeRadius, ny * placeRadius, nz * placeRadius);
            poseStack.mulPose(tilt);
            poseStack.scale(blockScale, blockScale, blockScale);
            // A block model spans (0,0,0)..(1,1,1); centre it on the placed position.
            poseStack.translate(-0.5, -0.5, -0.5);
            collector.submitBlock(poseStack, blockList.get(i).defaultBlockState(), packedLight, overlay, 0);
            poseStack.popPose();
        }

        if (isLocalPlayer) {
            // Camera distance: the hitbox plus the part of the blocks that sticks out of it.
            MAX_SIZE = slots > 0 ? collisionSize + 1.0 : collisionSize;
        }

        poseStack.popPose();
        return true;
    }

    private static int hashSlot(int index) {
        int h = index * 73856093 ^ (index >>> 3) * 19349663;
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
}
