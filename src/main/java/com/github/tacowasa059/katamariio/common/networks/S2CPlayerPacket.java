package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public record S2CPlayerPacket(float collisionSize, float renderSize, boolean flag, float restitution,
                              Quaternionf quaternion, Vec3 currentPosition) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2CPlayerPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "player_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPlayerPacket> STREAM_CODEC =
            StreamCodec.of(S2CPlayerPacket::write, S2CPlayerPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, S2CPlayerPacket packet) {
        buf.writeFloat(packet.collisionSize);
        buf.writeFloat(packet.renderSize);
        buf.writeBoolean(packet.flag);
        buf.writeFloat(packet.restitution);
        buf.writeFloat(packet.quaternion.x);
        buf.writeFloat(packet.quaternion.y);
        buf.writeFloat(packet.quaternion.z);
        buf.writeFloat(packet.quaternion.w);
        buf.writeDouble(packet.currentPosition.x);
        buf.writeDouble(packet.currentPosition.y);
        buf.writeDouble(packet.currentPosition.z);
    }

    private static S2CPlayerPacket read(RegistryFriendlyByteBuf buf) {
        float collisionSize = buf.readFloat();
        float renderSize = buf.readFloat();
        boolean flag = buf.readBoolean();
        float restitution = buf.readFloat();
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float w = buf.readFloat();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new S2CPlayerPacket(collisionSize, renderSize, flag, restitution, new Quaternionf(x, y, z, w), pos);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void applyToPlayer(Player player) {
        if (!(player instanceof ICustomPlayerData data)) {
            return;
        }
        data.katamariIO$setSize(collisionSize);
        data.katamariIO$setRenderSize(renderSize);
        data.katamariIO$setFlag(flag);
        data.katamariIO$setRestitutionCoefficient(restitution);
        data.katamariIO$setQuaternion(quaternion);
        data.katamariIO$setCurrentPosition(currentPosition);
        player.setPos(player.position());
    }
}
