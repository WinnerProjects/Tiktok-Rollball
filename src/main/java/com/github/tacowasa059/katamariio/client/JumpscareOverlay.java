package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * /jumpscare: plays the supplied video on the screen. The video was split into transparent PNG frames
 * (assets/rollball/textures/jumpscare/frame_NN.png, cropped to the visible area) and its audio is
 * assets/rollball/sounds/jumpscare.ogg. The frames are scaled so the original 854x480 picture covers the screen.
 */
public final class JumpscareOverlay {
    private static final int FRAMES = 26;
    private static final long DURATION_MILLIS = 960L;
    /** Size of the original video picture and of the cropped area that is stored in the PNGs. */
    private static final int SRC_W = 854;
    private static final int SRC_H = 480;
    private static final int CROP_X = 130;
    private static final int CROP_Y = 0;
    private static final int CROP_W = 520;
    private static final int CROP_H = 300;

    private static final Identifier[] TEXTURES = new Identifier[FRAMES];
    private static volatile long startMillis = 0L;

    private interface Blitter {
        void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height,
                  int regionWidth, int regionHeight, int textureWidth, int textureHeight);
    }

    private JumpscareOverlay() {
    }

    public static void register() {
        for (int i = 0; i < FRAMES; i++) {
            TEXTURES[i] = Identifier.fromNamespaceAndPath(KatamariIO.MODID,
                    String.format("textures/jumpscare/frame_%02d.png", i));
        }
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "jumpscare_overlay"),
                (graphics, deltaTracker) -> draw(graphics::blit));
    }

    /** Starts (or restarts) the jumpscare and plays its sound. */
    public static void start() {
        startMillis = System.currentTimeMillis();
        SoundEvent sound = SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(KatamariIO.MODID, "jumpscare"));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
    }

    private static void draw(Blitter blitter) {
        long start = startMillis;
        if (start <= 0L) {
            return;
        }
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed >= DURATION_MILLIS) {
            startMillis = 0L;
            return;
        }
        int frame = (int) Math.min(FRAMES - 1, Math.max(0L, elapsed * FRAMES / DURATION_MILLIS));

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double scale = Math.max(screenWidth / (double) SRC_W, screenHeight / (double) SRC_H);
        int x = (int) Math.round((screenWidth - SRC_W * scale) / 2.0 + CROP_X * scale);
        int y = (int) Math.round((screenHeight - SRC_H * scale) / 2.0 + CROP_Y * scale);
        int width = (int) Math.round(CROP_W * scale);
        int height = (int) Math.round(CROP_H * scale);

        blitter.blit(RenderPipelines.GUI_TEXTURED, TEXTURES[frame], x, y, 0.0F, 0.0F, width, height,
                CROP_W, CROP_H, CROP_W, CROP_H);
    }
}
