package com.github.tacowasa059.katamariio.client.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Builds the textured player sphere. Since 1.21.9 the geometry is emitted from a
 * {@code SubmitNodeCollector.CustomGeometryRenderer}, so this class only fills a {@link VertexConsumer}.
 * The 45 degree Y rotation the old version applied is now done by the caller on the PoseStack.
 */
public class SphereRenderer {
    public static void drawTexturedSphere(PoseStack.Pose pose, VertexConsumer vertexConsumer, float radius, int segments,
                                          float x, float z, int packedLight, int overlay) {
        addBottomSphere(radius, segments, x, z, vertexConsumer, pose, 0.1875F, false, packedLight, overlay);
        addBottomSphere(radius * 1.01F, segments, x, z, vertexConsumer, pose, 0.6875F, false, packedLight, overlay);
        addBottomSphere(radius, segments, x, z, vertexConsumer, pose, 0.3125F, true, packedLight, overlay);
        addBottomSphere(radius * 1.01F, segments, x, z, vertexConsumer, pose, 0.8125F, true, packedLight, overlay);
        addSideSphere(radius, segments, x, z, vertexConsumer, pose, false, packedLight, overlay);
        addSideSphere(radius * 1.01F, segments, x, z, vertexConsumer, pose, true, packedLight, overlay);
    }

    private static void addBottomSphere(float radius, int segments, float x, float z, VertexConsumer vertexBuilder,
                                        PoseStack.Pose pose, float u0, boolean isLower, int packedLight, int overlay) {
        float cubeSize = 0.0625F;
        for (int j = 0; j < Math.round(segments / 4f); j++) {
            float theta = (float) (Math.PI * j / (4 * Math.round(segments / 4f)));
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            float nextTheta = (float) (Math.PI * (j + 1) / (4 * Math.round(segments / 4f)));
            float nextSinTheta = (float) Math.sin(nextTheta);
            float nextCosTheta = (float) Math.cos(nextTheta);

            for (int i = 0; i < segments; i++) {
                double angle = -Math.PI * 2 * i / segments;
                double nextAngle = -Math.PI * 2 * (i + 1) / segments;
                if (isLower) {
                    angle = -angle;
                    nextAngle = -nextAngle;
                }

                float dx = (float) (radius * sinTheta * Math.cos(angle));
                float dz = (float) (radius * sinTheta * Math.sin(angle));
                float nextIdx = (float) (radius * sinTheta * Math.cos(nextAngle));
                float nextIdz = (float) (radius * sinTheta * Math.sin(nextAngle));
                float nextJdx = (float) (radius * nextSinTheta * Math.cos(angle));
                float nextJdz = (float) (radius * nextSinTheta * Math.sin(angle));
                float nextIJdx = (float) (radius * nextSinTheta * Math.cos(nextAngle));
                float nextIJdz = (float) (radius * nextSinTheta * Math.sin(nextAngle));

                float[] posList = isLower
                        ? new float[]{dx, -radius * cosTheta, dz, nextIdx, -radius * cosTheta, nextIdz, nextJdx, -radius * nextCosTheta, nextJdz, nextIJdx, -radius * nextCosTheta, nextIJdz}
                        : new float[]{dx, radius * cosTheta, dz, nextIdx, radius * cosTheta, nextIdz, nextJdx, radius * nextCosTheta, nextJdz, nextIJdx, radius * nextCosTheta, nextIJdz};

                float x1 = (float) Math.cos(angle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float z1 = (float) Math.sin(angle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextIx1 = (float) Math.cos(nextAngle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextIz1 = (float) Math.sin(nextAngle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextJx1 = (float) Math.cos(angle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextJz1 = (float) Math.sin(angle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextIJx1 = (float) Math.cos(nextAngle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextIJz1 = (float) Math.sin(nextAngle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);

                float u = u0 + getSquareX(x1, z1, cubeSize);
                float v = 0.0625F + getSquareZ(x1, z1, cubeSize);
                float nextIU = u0 + getSquareX(nextIx1, nextIz1, cubeSize);
                float nextIV = 0.0625F + getSquareZ(nextIx1, nextIz1, cubeSize);
                float nextJU = u0 + getSquareX(nextJx1, nextJz1, cubeSize);
                float nextJV = 0.0625F + getSquareZ(nextJx1, nextJz1, cubeSize);
                float nextIJU = u0 + getSquareX(nextIJx1, nextIJz1, cubeSize);
                float nextIJV = 0.0625F + getSquareZ(nextIJx1, nextIJz1, cubeSize);

                float[] uvList = new float[]{u, v, nextIU, nextIV, nextJU, nextJV, nextIJU, nextIJV};
                addSideSphereQuads(vertexBuilder, pose, x, z, posList, uvList, packedLight, overlay);
            }
        }
    }

    private static void addSideSphere(float radius, int segments, float x, float z, VertexConsumer vertexBuilder,
                                      PoseStack.Pose pose, boolean isInner, int packedLight, int overlay) {
        for (int j = Math.round(segments / 4f); j < Math.round(3 * segments / 4f); j++) {
            float theta = (float) (Math.PI * j / (4 * Math.round(segments / 4f)));
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            float nextTheta = (float) (Math.PI * (j + 1) / (4 * Math.round(segments / 4f)));
            float nextSinTheta = (float) Math.sin(nextTheta);
            float nextCosTheta = (float) Math.cos(nextTheta);

            for (int i = 0; i < segments; i++) {
                double angle = -Math.PI / 2 - Math.PI * 2 * i / segments;
                double nextAngle = -Math.PI / 2 - Math.PI * 2 * (i + 1) / segments;

                float dx = (float) (radius * Math.cos(angle) * sinTheta);
                float dz = (float) (radius * Math.sin(angle) * sinTheta);
                float nextIdx = (float) (radius * Math.cos(nextAngle) * sinTheta);
                float nextIdz = (float) (radius * Math.sin(nextAngle) * sinTheta);
                float nextJdx = (float) (radius * Math.cos(angle) * nextSinTheta);
                float nextJdz = (float) (radius * Math.sin(angle) * nextSinTheta);
                float nextIJdx = (float) (radius * Math.cos(nextAngle) * nextSinTheta);
                float nextIJdz = (float) (radius * Math.sin(nextAngle) * nextSinTheta);

                float[] posList = new float[]{
                        dx, radius * cosTheta, dz,
                        nextIdx, radius * cosTheta, nextIdz,
                        nextJdx, radius * nextCosTheta, nextJdz,
                        nextIJdx, radius * nextCosTheta, nextIJdz
                };

                float u = 0.5F * i / segments;
                float v = 0.125F + 0.125F * (j - Math.round(segments / 4f)) / Math.round(segments / 2f);
                float nextIU = 0.5F * (i + 1) / segments;
                float nextJU = 0.5F * i / segments;
                float nextJV = 0.125F + 0.125F * (j + 1 - Math.round(segments / 4f)) / Math.round(segments / 2f);
                float nextIJU = 0.5F * (i + 1) / segments;

                if (isInner) {
                    u += 0.5F;
                    nextIU += 0.5F;
                    nextJU += 0.5F;
                    nextIJU += 0.5F;
                }

                float[] uvList = new float[]{u, v, nextIU, v, nextJU, nextJV, nextIJU, nextJV};
                addSideSphereQuads(vertexBuilder, pose, x, z, posList, uvList, packedLight, overlay);
            }
        }
    }

    private static void addSideSphereQuads(VertexConsumer vertexConsumer, PoseStack.Pose pose,
                                           float x, float z, float[] posList, float[] uvList, int packedLight, int overlay) {
        vertex(vertexConsumer, pose, posList[0] + x, posList[1], posList[2] + z, uvList[0], uvList[1],
                packedLight, overlay, posList[0], posList[1], posList[2]);
        vertex(vertexConsumer, pose, posList[3] + x, posList[4], posList[5] + z, uvList[2], uvList[3],
                packedLight, overlay, posList[3], posList[4], posList[5]);
        vertex(vertexConsumer, pose, posList[9] + x, posList[10], posList[11] + z, uvList[6], uvList[7],
                packedLight, overlay, posList[9], posList[10], posList[11]);
        vertex(vertexConsumer, pose, posList[6] + x, posList[7], posList[8] + z, uvList[4], uvList[5],
                packedLight, overlay, posList[6], posList[7], posList[8]);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float px, float py, float pz,
                               float u, float v, int packedLight, int overlay, float nx, float ny, float nz) {
        consumer.addVertex(pose, px, py, pz)
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    private static float getSquareZ(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 + 2 * Math.sqrt(2.0) * z1)
                - 0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 - 2 * Math.sqrt(2.0) * z1)));
    }

    private static float getSquareX(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 + 2 * Math.sqrt(2.0) * x1)
                - 0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 - 2 * Math.sqrt(2.0) * x1)));
    }
}
