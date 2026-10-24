package com.github.tacowasa059.katamariio.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side effects of the ball mode:
 * - unlimited night vision while the ball mode is on,
 * - /kör blindness: the client draws a fully black screen with a 1-second counter (see BlindOverlay);
 *   a vanilla blindness effect is applied as well so the world stays dark even with the HUD hidden.
 */
public final class KatamariEffects {
    public static final int BLIND_SECONDS = 10;
    private static final Map<UUID, Long> BLIND_UNTIL = new HashMap<>();

    private KatamariEffects() {
    }

    /** Adds blindness time (stacks on top of the remaining time). Returns the remaining whole seconds. */
    public static int addBlind(ServerPlayer player, int seconds) {
        long now = System.currentTimeMillis();
        Long current = BLIND_UNTIL.get(player.getUUID());
        long base = (current != null && current > now) ? current : now;
        long until = base + seconds * 1000L;
        BLIND_UNTIL.put(player.getUUID(), until);
        int remaining = (int) (until - now);
        ModNetwork.sendBlind(player, remaining);
        applyBlindEffect(player, remaining);
        return (remaining + 999) / 1000;
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 10 != 0) {
            return;
        }
        ICustomPlayerData data = (ICustomPlayerData) player;

        MobEffectInstance night = player.getEffect(MobEffects.NIGHT_VISION);
        boolean infinite = night != null && night.getDuration() == MobEffectInstance.INFINITE_DURATION;
        if (data.katamariIO$getFlag()) {
            if (!infinite) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                        MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
            }
        } else if (infinite && !night.isVisible()) {
            // Only our own (hidden, infinite) night vision is removed, never a real potion.
            player.removeEffect(MobEffects.NIGHT_VISION);
        }

        Long until = BLIND_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining > 0) {
            applyBlindEffect(player, (int) remaining);
            if (player.tickCount % 20 == 0) {
                ModNetwork.sendBlind(player, (int) remaining);
            }
        } else {
            BLIND_UNTIL.remove(player.getUUID());
            player.removeEffect(MobEffects.BLINDNESS);
            ModNetwork.sendBlind(player, 0);
        }
    }

    private static void applyBlindEffect(ServerPlayer player, int remainingMillis) {
        int ticks = remainingMillis / 50 + 5;
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false, false));
    }
}
