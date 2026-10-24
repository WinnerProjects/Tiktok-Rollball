package com.github.tacowasa059.katamariio.common.networks;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class ModNetwork {
    private ModNetwork() {
    }

    /** Registers payload types (both sides) and the server-side receiver. */
    public static void register() {
        PayloadTypeRegistry.playC2S().register(RemoveBlockPacket.TYPE, RemoveBlockPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(S2CPlayerPacket.TYPE, S2CPlayerPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BlindPacket.TYPE, BlindPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(JumpscarePacket.TYPE, JumpscarePacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RemoveBlockPacket.TYPE,
                (payload, context) -> payload.handle(context.player()));
    }

    public static void sendJumpscare(ServerPlayer player) {
        ServerPlayNetworking.send(player, new JumpscarePacket());
    }

    public static void sendBlind(ServerPlayer player, int remainingMillis) {
        ServerPlayNetworking.send(player, new BlindPacket(Math.max(0, remainingMillis)));
    }

    public static void sendInitialData(ServerPlayer player,
                                       float collisionSize, float renderSize, boolean flag, float restitution,
                                       Quaternionf quaternion, Vec3 currentPos) {
        ServerPlayNetworking.send(player,
                new S2CPlayerPacket(collisionSize, renderSize, flag, restitution, quaternion, currentPos));
    }
}
