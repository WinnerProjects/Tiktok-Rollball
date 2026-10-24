package com.github.tacowasa059.katamariio.common.commands;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.KatamariEffects;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class KatamariCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerTop(dispatcher);
        registerJumpscare(dispatcher);
        registerBlind(dispatcher, "kör");
        registerBlind(dispatcher, "kor");
        dispatcher.register(Commands.literal("katamari")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("size")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01f, 20.0f))
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> {
                                                ICustomPlayerData data = (ICustomPlayerData) p;
                                                data.katamariIO$setSize(value);
                                                data.katamariIO$setRenderSize(value);
                                            });
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Set size to " +
                                                            ChatFormatting.AQUA + value + ChatFormatting.GREEN + " for " + players.size() + " player(s)."), false);
                                            return 1;
                                        })))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                                    float size = ((ICustomPlayerData) player).katamariIO$getSize();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Player size: " + ChatFormatting.AQUA + size), false);
                                    return 1;
                                })))
                .then(Commands.literal("restitution")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01f, 1.2f))
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> ((ICustomPlayerData) p).katamariIO$setRestitutionCoefficient(value));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Set restitution to " +
                                                            ChatFormatting.AQUA + value + ChatFormatting.GREEN + " for " + players.size() + " player(s)."), false);
                                            return 1;
                                        })))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                                    float value = ((ICustomPlayerData) player).katamariIO$getRestitutionCoefficient();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Restitution coefficient: " + ChatFormatting.AQUA + value), false);
                                    return 1;
                                })))
                .then(Commands.literal("ball")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean flag = BoolArgumentType.getBool(ctx, "enabled");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> {
                                                ICustomPlayerData data = (ICustomPlayerData) p;
                                                // The size is kept when the ball mode is switched off.
                                                data.katamariIO$setFlag(flag);
                                            });
                                            ChatFormatting flagColor = flag ? ChatFormatting.AQUA : ChatFormatting.RED;
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Set ball flag to " + flagColor + flag +
                                                            ChatFormatting.GREEN + " for " + players.size() + " player(s)."), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("resetBlocks")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    players.forEach(p -> {
                                        ICustomPlayerData data = (ICustomPlayerData) p;
                                        data.katamariIO$setFlagAndSizeAndRestitution(
                                                data.katamariIO$getFlag(),
                                                KatamariIO.DEFAULT_BALL_SIZE,
                                                KatamariIO.DEFAULT_BALL_SIZE,
                                                data.katamariIO$getRestitutionCoefficient()
                                        );
                                        data.katamariIO$clearAttachedBlocks();
                                    });
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Cleared attached blocks for " +
                                                    ChatFormatting.AQUA + players.size() + ChatFormatting.GREEN + " player(s)."), true);
                                    return players.size();
                                }))));
    }

    /** /top: switches the ball mode on/off (for yourself; operators may add a target). The size is kept. */
    private static void registerTop(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("top")
                .executes(ctx -> toggleBall(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> toggleBall(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))));
    }

    private static int toggleBall(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            ICustomPlayerData data = (ICustomPlayerData) player;
            boolean enabled = !data.katamariIO$getFlag();
            data.katamariIO$setFlag(enabled);
            player.sendSystemMessage(Component.literal(enabled ? "Top modu açıldı." : "Top modu kapandı.")
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        return players.size();
    }

    /** /kör: the screen goes fully black for 10 seconds; running it again adds 10 more seconds. */
    private static void registerBlind(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        dispatcher.register(Commands.literal(name)
                .executes(ctx -> blind(List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> blind(EntityArgument.getPlayers(ctx, "targets")))));
    }

    private static int blind(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            int seconds = KatamariEffects.addBlind(player, KatamariEffects.BLIND_SECONDS);
            player.sendSystemMessage(Component.literal("Kör oldun! Kalan süre: " + seconds + " sn")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return players.size();
    }

    /** /jumpscare: plays the jumpscare video on your own screen (operators may add targets). */
    private static void registerJumpscare(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jumpscare")
                .executes(ctx -> jumpscare(List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> jumpscare(EntityArgument.getPlayers(ctx, "targets")))));
    }

    private static int jumpscare(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            ModNetwork.sendJumpscare(player);
        }
        return players.size();
    }
}
