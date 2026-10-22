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
        List<Quaternionf> quaternionfList = playerData.katamariIO$getSphericalPlayerQuaternions();
        int lodStep = getLodStepForPlayer(player);
        int sharedSize = Math.min(vec3List.size(), Math.min(blockList.size(), quaternionfList.size()));

        float minLen = radius - HIDDEN_DEPTH;
        double minLenSq = minLen > 0.0f ? (double) minLen * minLen : -1.0;

        for (int i = 0; i < sharedSize; i++) {
            Vec3 vec = vec3List.get(i);
            double lenSq = vec.lengthSqr();
            if (lenSq < minLenSq) {
                continue;
            }
            if (lodStep > 1 && !shouldRenderByHash(vec, i, player.getId(), lodStep)) {
                continue;
            }
            Block block = blockList.get(i);
            Quaternionf blockRotation = quaternionfList.get(i);

            poseStack.pushPose();
            poseStack.translate(vec.x, vec.y, vec.z);
            if (!isIdentity(blockRotation)) {
                poseStack.mulPose(blockRotation);
            }
            // A block model spans (0,0,0)..(1,1,1); centre it on the stored position.
            poseStack.translate(-0.5, -0.5, -0.5);
            collector.submitBlock(poseStack, block.defaultBlockState(), packedLight, overlay, 0);
            poseStack.popPose();

            if (isLocalPlayer) {
                tmpSize = Math.max(tmpSize, Math.sqrt(lenSq) * 2);
            }
        }

        if (isLocalPlayer) {
            MAX_SIZE = tmpSize;
        }

        poseStack.popPose();
        return true;
    }

    private static boolean isIdentity(Quaternionf q) {
        return q.x() == 0.0f && q.y() == 0.0f && q.z() == 0.0f && q.w() == 1.0f;
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
