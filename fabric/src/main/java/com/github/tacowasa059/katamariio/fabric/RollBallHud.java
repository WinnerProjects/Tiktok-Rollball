package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.client.KatamariPlayerRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public final class RollBallHud {
    private static final ResourceLocation HUD_ID = ResourceLocation.fromNamespaceAndPath("rollball", "size_hud");
    private static int countdown = -1;
    private static int countdownTick = 0;
    private static int winTicks = 0;
    private static boolean triggered = false;

    private RollBallHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, HUD_ID, RollBallHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            countdown = -1;
            countdownTick = 0;
            winTicks = 0;
            triggered = false;
            return;
        }

        double size = KatamariPlayerRenderer.MAX_SIZE;
        if (size < 50.0) {
            triggered = false;
            countdown = -1;
            countdownTick = 0;
            return;
        }
        if (!triggered && countdown < 0 && winTicks <= 0) {
            countdown = 10;
            countdownTick = 0;
        }
        if (countdown >= 0) {
            countdownTick++;
            int duration = countdown <= 3 ? 30 : 20;
            if (countdownTick >= duration) {
                countdownTick = 0;
                countdown--;
                if (countdown < 1) {
                    countdown = -1;
                    winTicks = 80;
                    triggered = true;
                    spawnFireworkRing();
                }
            }
        } else if (winTicks > 0) {
            winTicks--;
        }
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int width = graphics.guiWidth();
        int size = (int) Math.min(100.0, Math.max(0.0, KatamariPlayerRenderer.MAX_SIZE));
        String label = String.format(java.util.Locale.ROOT, "Boyut: %.1f Blok", KatamariPlayerRenderer.MAX_SIZE);
        int barWidth = 182;
        int x = (width - barWidth) / 2;
        graphics.drawCenteredString(mc.font, Component.literal(label), width / 2, 8, 0xFFFFFF);
        graphics.fill(x, 20, x + barWidth, 26, 0xAA222222);
        graphics.fill(x + 1, 21, x + 1 + (int) ((barWidth - 2) * (size / 50.0)), 25, size >= 50 ? 0xFFFFD700 : 0xFF55AAFF);

        if (countdown >= 0) {
            String text = countdown <= 3 ? Integer.toString(countdown) : Integer.toString(countdown);
            int color = countdown <= 3 ? 0xFFFF5555 : 0xFFFFFFFF;
            graphics.drawCenteredString(mc.font, Component.literal(text), width / 2, graphics.guiHeight() / 2 - 10, color);
        } else if (winTicks > 0) {
            graphics.drawCenteredString(mc.font, Component.literal("+ WIN"), width / 2, graphics.guiHeight() / 2 - 10, 0xFFFFD700);
        }
    }

    private static void spawnFireworkRing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        double cx = mc.player.getX();
        double cy = mc.player.getY() + 1.0;
        double cz = mc.player.getZ();
        int points = 48;
        double radius = Math.max(2.0, Math.min(8.0, KatamariPlayerRenderer.MAX_SIZE * 0.2));
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            mc.level.addParticle(ParticleTypes.FIREWORK, px, cy, pz, 0.0, 0.04, 0.0);
        }
    }
}
