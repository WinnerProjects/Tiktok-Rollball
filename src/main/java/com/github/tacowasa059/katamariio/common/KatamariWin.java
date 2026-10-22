package com.github.tacowasa059.katamariio.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Win sequence: when the ball reaches the maximum size a 15..1 countdown is shown in the middle of the screen
 * (15..4 take 1 second each, 3-2-1 run slower), then "+Win" appears and fireworks are launched in a circle around the player.
 */
public final class KatamariWin {
    private static final float WIN_SIZE = 20.0f;
    private static final int START_VALUE = 15;
    /** Real time per number for 15..4: exactly 1 second, independent of the server tick rate. */
    private static final long NORMAL_MILLIS = 1000L;
    /** Ticks per number for 3, 2, 1 (unchanged: 50 ticks = 2.5 seconds). */
    private static final int SLOW_TICKS = 50;
    private static final long SLOW_MILLIS = SLOW_TICKS * 50L;

    private static final int FIREWORK_WAVES = 4;
    private static final int WAVE_INTERVAL_TICKS = 14;
    private static final int ROCKETS_PER_WAVE = 16;
    private static final int[] COLORS = {0xFF3B30, 0xFFCC00, 0x34C759, 0x0A84FF, 0xBF5AF2, 0xFF9F0A, 0xFFFFFF};

    private enum Phase {COUNTDOWN, CELEBRATE}

    private static final class State {
        Phase phase = Phase.COUNTDOWN;
        int value = START_VALUE;
        long nextAtMillis;
        int wave;
        int waveTimer;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Players that already got their win and are still at max size. */
    private static final Set<UUID> DONE = new HashSet<>();

    private KatamariWin() {
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        ICustomPlayerData data = (ICustomPlayerData) player;
        float size = data.katamariIO$getSize();
        boolean active = data.katamariIO$getFlag();
        boolean reached = active && size >= WIN_SIZE - 0.05f;
        boolean fellBack = !active || size < WIN_SIZE - 0.5f;

        State state = STATES.get(id);
        if (state == null) {
            if (fellBack) {
                DONE.remove(id);
                return;
            }
            if (!reached || DONE.contains(id)) {
                return;
            }
            state = new State();
            STATES.put(id, state);
            showNumber(player, state);
            return;
        }

        if (state.phase == Phase.COUNTDOWN) {
            if (fellBack) {
                // Size dropped again (command/reset): cancel the countdown.
                STATES.remove(id);
                player.connection.send(new ClientboundClearTitlesPacket(true));
                return;
            }
            if (System.currentTimeMillis() < state.nextAtMillis) {
                return;
            }
            state.value--;
            if (state.value > 0) {
                showNumber(player, state);
            } else {
                showWin(player);
                state.phase = Phase.CELEBRATE;
                state.wave = 0;
                state.waveTimer = 0;
            }
            return;
        }

        // CELEBRATE
        if (state.waveTimer-- <= 0) {
            spawnFireworkRing(player, state.wave);
            state.wave++;
            state.waveTimer = WAVE_INTERVAL_TICKS;
            if (state.wave >= FIREWORK_WAVES) {
                STATES.remove(id);
                DONE.add(id);
            }
        }
    }

    private static int ticksFor(int value) {
        return value <= 3 ? SLOW_TICKS : 20;
    }

    private static void showNumber(ServerPlayer player, State state) {
        int ticks = ticksFor(state.value);
        state.nextAtMillis = System.currentTimeMillis() + (state.value <= 3 ? SLOW_MILLIS : NORMAL_MILLIS);
        ChatFormatting color = state.value <= 3 ? ChatFormatting.RED : (state.value <= 8 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, Math.max(1, ticks - 4), 4));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(String.valueOf(state.value)).withStyle(color, ChatFormatting.BOLD)));
    }

    private static void showWin(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("+Win").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
    }

    private static void spawnFireworkRing(ServerPlayer player, int wave) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ICustomPlayerData data = (ICustomPlayerData) player;
        double ringRadius = Math.max(6.0, data.katamariIO$getSize() / 2.0 + 5.0);
        Vec3 origin = player.position();
        // Every second wave is rotated by half a step so the ring looks fuller.
        double offset = (wave % 2) * 0.5;

        for (int i = 0; i < ROCKETS_PER_WAVE; i++) {
            double angle = 2.0 * Math.PI * (i + offset) / ROCKETS_PER_WAVE;
            int x = (int) Math.floor(origin.x + Math.cos(angle) * ringRadius);
            int z = (int) Math.floor(origin.z + Math.sin(angle) * ringRadius);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));

            ItemStack rocket = createRocket(level, i + wave);
            FireworkRocketEntity entity = new FireworkRocketEntity(level, x + 0.5, ground.getY() + 0.5, z + 0.5, rocket);
            level.addFreshEntity(entity);
        }
    }

    private static ItemStack createRocket(ServerLevel level, int seed) {
        int c1 = COLORS[Math.floorMod(seed, COLORS.length)];
        int c2 = COLORS[Math.floorMod(seed + 3, COLORS.length)];
        int fade = COLORS[Math.floorMod(seed + 5, COLORS.length)];
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.LARGE_BALL,
                new IntArrayList(new int[]{c1, c2}),
                new IntArrayList(new int[]{fade}),
                true,
                true);
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponents.FIREWORKS, new Fireworks(1 + level.getRandom().nextInt(2), List.of(explosion)));
        return stack;
    }
}
