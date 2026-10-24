package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.KatamariIO;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * /kör overlay: a fully black screen with "KÖR:(seconds)" drawn in the middle in a big pixel font
 * (every number lasts exactly 1 second). Only plain rectangle fills are used, so no fonts or text scaling are needed.
 */
public final class BlindOverlay {
    private static volatile long endAtMillis = 0L;

    private static final int GLYPH_WIDTH = 5;
    private static final int GLYPH_HEIGHT = 9;
    /** Two empty rows on top leave room for the dots of the Ö. */
    private static final String[] BLANK = {"00000", "00000"};

    private interface Painter {
        void fill(int x1, int y1, int x2, int y2, int color);
    }

    private BlindOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "blind_overlay"),
                (graphics, deltaTracker) -> draw(graphics::fill));
    }

    /** Called when the server tells us how much blindness is left (0 = none). */
    public static void receive(int remainingMillis) {
        endAtMillis = remainingMillis > 0 ? System.currentTimeMillis() + remainingMillis : 0L;
    }

    private static void draw(Painter painter) {
        long end = endAtMillis;
        if (end <= 0L) {
            return;
        }
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0L) {
            endAtMillis = 0L;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        painter.fill(0, 0, width, height, 0xFF000000);

        int seconds = (int) ((remaining + 999L) / 1000L);
        drawText(painter, width, height, "KÖR:" + seconds, seconds <= 3 ? 0xFFFF5555 : 0xFFFFFF55);
    }

    private static void drawText(Painter painter, int screenWidth, int screenHeight, String text, int color) {
        int columns = text.length() * (GLYPH_WIDTH + 1) - 1;
        int pixel = Math.max(2, Math.min(screenHeight / 40, screenWidth * 3 / 4 / columns));
        int x = (screenWidth - columns * pixel) / 2;
        int y = (screenHeight - GLYPH_HEIGHT * pixel) / 2;

        for (int i = 0; i < text.length(); i++) {
            String[] rows = glyph(text.charAt(i));
            if (rows == null) {
                continue;
            }
            int gx = x + i * (GLYPH_WIDTH + 1) * pixel;
            for (int row = 0; row < rows.length; row++) {
                String bits = rows[row];
                for (int col = 0; col < GLYPH_WIDTH; col++) {
                    if (bits.charAt(col) == '1') {
                        int px = gx + col * pixel;
                        int py = y + row * pixel;
                        painter.fill(px, py, px + pixel, py + pixel, color);
                    }
                }
            }
        }
    }

    private static String[] withTop(String[] top, String... rows) {
        String[] result = new String[top.length + rows.length];
        System.arraycopy(top, 0, result, 0, top.length);
        System.arraycopy(rows, 0, result, top.length, rows.length);
        return result;
    }

    private static String[] glyph(char c) {
        switch (c) {
            case 'K':
                return withTop(BLANK, "10001", "10010", "10100", "11000", "10100", "10010", "10001");
            case 'Ö':
                return withTop(new String[]{"01010", "00000"},
                        "01110", "10001", "10001", "10001", "10001", "10001", "01110");
            case 'R':
                return withTop(BLANK, "11110", "10001", "10001", "11110", "10100", "10010", "10001");
            case ':':
                return withTop(BLANK, "00000", "00100", "00100", "00000", "00100", "00100", "00000");
            case '0':
                return withTop(BLANK, "01110", "10001", "10011", "10101", "11001", "10001", "01110");
            case '1':
                return withTop(BLANK, "00100", "01100", "00100", "00100", "00100", "00100", "01110");
            case '2':
                return withTop(BLANK, "01110", "10001", "00001", "00010", "00100", "01000", "11111");
            case '3':
                return withTop(BLANK, "11110", "00001", "00001", "01110", "00001", "00001", "11110");
            case '4':
                return withTop(BLANK, "00010", "00110", "01010", "10010", "11111", "00010", "00010");
            case '5':
                return withTop(BLANK, "11111", "10000", "11110", "00001", "00001", "10001", "01110");
            case '6':
                return withTop(BLANK, "00110", "01000", "10000", "11110", "10001", "10001", "01110");
            case '7':
                return withTop(BLANK, "11111", "00001", "00010", "00100", "01000", "01000", "01000");
            case '8':
                return withTop(BLANK, "01110", "10001", "10001", "01110", "10001", "10001", "01110");
            case '9':
                return withTop(BLANK, "01110", "10001", "10001", "01111", "00001", "00010", "01100");
            default:
                return null;
        }
    }
}
