package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.KatamariAbsorb;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record RemoveBlockPacket(BlockPos pos, Vec3 playerPos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RemoveBlockPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "remove_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveBlockPacket> STREAM_CODEC =
            StreamCodec.of(RemoveBlockPacket::write, RemoveBlockPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, RemoveBlockPacket packet) {
        buf.writeBlockPos(packet.pos);
        buf.writeDouble(packet.playerPos.x);
        buf.writeDouble(packet.playerPos.y);
        buf.writeDouble(packet.playerPos.z);
    }

    private static RemoveBlockPacket read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Vec3 loc = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new RemoveBlockPacket(pos, loc);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            return;
        }
        KatamariAbsorb.absorb(serverPlayer.level(), serverPlayer, pos, playerPos);
    }
}
