package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.KatamariIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: play the jumpscare video (frames + sound) once. */
public record JumpscarePacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<JumpscarePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "jumpscare"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JumpscarePacket> STREAM_CODEC =
            StreamCodec.of(JumpscarePacket::write, JumpscarePacket::read);

    private static void write(RegistryFriendlyByteBuf buf, JumpscarePacket packet) {
        buf.writeByte(0);
    }

    private static JumpscarePacket read(RegistryFriendlyByteBuf buf) {
        buf.readByte();
        return new JumpscarePacket();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
