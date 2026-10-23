package com.github.tacowasa059.katamariio.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geometry of the block shell that surrounds the fixed-size core ball.
 *
 * The shell is ONE complete layer of blocks laid out on a Fibonacci sphere (every slot has almost the same distance
 * to its neighbours), so the ball is always perfectly round and has no holes. The number of slots only depends on the
 * ball size and is the same on the server and on the client; the synced block list simply has one block per slot.
 *
 * To avoid the whole shell re-arranging on every 0.1 growth step, the slot count only changes in "bands"
 * (every ~12% of radius). Inside a band the slot count is sized for the top of the band, so there are never gaps.
 */
public final class KatamariShell {
    /** The core (textured) ball never grows. It is the size the ball starts with. */
    public static final float CORE_RADIUS = 1.0f;
    /** Block centres sit this far below the hitbox radius (blocks stick out slightly like a rough surface). */
    public static final float SHELL_INSET = 0.3f;

    /** Surface area one block has to cover. Smaller = denser shell = more blocks. */
    private static final double SLOT_AREA = 0.60;
    private static final double BAND_BASE = 1.0;
    private static final double BAND_GROWTH = 1.12;
    private static final int MIN_SLOTS = 6;
    private static final int MAX_SLOTS = 4000;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private static final Map<Integer, float[]> CACHE = new ConcurrentHashMap<>();

    private KatamariShell() {
    }

    /** Radius on which the block centres are placed for a ball of the given (hitbox) size. */
    public static float shellRadius(float size) {
        return Math.max(0.3f, size / 2.0f - SHELL_INSET);
    }

    /** Number of blocks in the shell for a ball of the given (hitbox) size. */
    public static int slotCount(float size) {
        double rho = shellRadius(size);
        int band = 0;
        if (rho > BAND_BASE) {
            band = (int) Math.ceil(Math.log(rho / BAND_BASE) / Math.log(BAND_GROWTH) - 1.0E-9);
        }
        double rhoTop = BAND_BASE * Math.pow(BAND_GROWTH, band);
        int slots = (int) Math.ceil(4.0 * Math.PI * rhoTop * rhoTop / SLOT_AREA);
        return Math.max(MIN_SLOTS, Math.min(MAX_SLOTS, slots));
    }

    /** Unit directions of all slots as x0,y0,z0,x1,y1,z1,... (length 3 * count). Do not modify the returned array. */
    public static float[] directions(int count) {
        if (count <= 0) {
            return new float[0];
        }
        float[] cached = CACHE.get(count);
        if (cached != null) {
            return cached;
        }
        if (CACHE.size() > 64) {
            CACHE.clear();
        }
        float[] built = build(count);
        CACHE.put(count, built);
        return built;
    }

    private static float[] build(int count) {
        float[] out = new float[count * 3];
        for (int i = 0; i < count; i++) {
            double y = 1.0 - (2.0 * i + 1.0) / count;
            double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double theta = GOLDEN_ANGLE * i;
            out[3 * i] = (float) (Math.cos(theta) * r);
            out[3 * i + 1] = (float) y;
            out[3 * i + 2] = (float) (Math.sin(theta) * r);
        }
        return out;
    }
}
