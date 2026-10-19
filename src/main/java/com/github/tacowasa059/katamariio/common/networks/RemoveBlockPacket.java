package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

        Level level = serverPlayer.level();
        Block block = level.getBlockState(pos).getBlock();
        if (block.equals(Blocks.AIR) || block.equals(Blocks.CAVE_AIR) || block.equals(Blocks.VOID_AIR)) {
            return;
        }

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.removeBlockEntity(pos);

        ICustomPlayerData playerData = (ICustomPlayerData) serverPlayer;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        float radius = playerData.katamariIO$getSize() / 2.0f;
        Vec3 center = playerPos.add(0, radius, 0);
        Vec3 relativePosWorld = new Vec3(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f).subtract(center);
        Quaternionf inverseBallRotation = new Quaternionf(playerData.katamariIO$getQuaternion()).invert();
        Vector3f relativePosLocal = new Vector3f((float) relativePosWorld.x, (float) relativePosWorld.y, (float) relativePosWorld.z);
        relativePosLocal.rotate(inverseBallRotation);

        playerData.katamariIO$addBlock(block, inverseBallRotation,
                new Vec3(relativePosLocal.x, relativePosLocal.y, relativePosLocal.z));
    }
}
