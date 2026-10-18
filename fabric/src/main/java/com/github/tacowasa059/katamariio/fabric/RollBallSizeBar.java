package com.github.tacowasa059.katamariio.fabric;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player size bossbar, updated once per server tick. */
public final class RollBallSizeBar {
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<UUID, Boolean> ENABLED = new HashMap<>();

    private RollBallSizeBar() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommand(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(RollBallSizeBar::tick);
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("boyut")
                .executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        ctx.getSource().sendFailure(Component.literal("Bu komut bir oyuncu tarafından kullanılmalı."));
                        return 0;
                    }
                    UUID id = player.getUUID();
                    boolean enabled = !ENABLED.getOrDefault(id, false);
                    ENABLED.put(id, enabled);
                    if (enabled) {
                        ServerBossEvent bar = BARS.computeIfAbsent(id, ignored ->
                                new ServerBossEvent(Component.literal("Boyut: 2.0 Blok"),
                                        BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS));
                        bar.addPlayer(player);
                        updateBar(player, bar);
                        player.sendSystemMessage(Component.literal("Boyut göstergesi açıldı.").withStyle(ChatFormatting.GREEN));
                    } else {
                        ServerBossEvent bar = BARS.get(id);
                        if (bar != null) bar.removePlayer(player);
                        player.sendSystemMessage(Component.literal("Boyut göstergesi kapatıldı.").withStyle(ChatFormatting.YELLOW));
                    }
                    return 1;
                }));
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!ENABLED.getOrDefault(id, false)) continue;
            ServerBossEvent bar = BARS.get(id);
            if (bar == null) continue;
            if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
            updateBar(player, bar);
        }

        // Remove disconnected players so the cache does not grow forever.
        ENABLED.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        BARS.keySet().removeIf(id -> {
            if (server.getPlayerList().getPlayer(id) != null) return false;
            ServerBossEvent bar = BARS.get(id);
            if (bar != null) bar.removeAllPlayers();
            return true;
        });
    }

    private static void updateBar(ServerPlayer player, ServerBossEvent bar) {
        float size = Math.max(0.0f, ((ICustomPlayerData) player).katamariIO$getRenderSize());
        bar.setName(Component.literal(String.format(java.util.Locale.ROOT, "Boyut: %.1f Blok", size)));
        bar.setProgress(Math.max(0.0f, Math.min(1.0f, size / 20.0f)));
    }
}
