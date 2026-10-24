package com.github.tacowasa059.katamariio.common.networks;

import com.github.tacowasa059.katamariio.KatamariIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: how many milliseconds of blindness are left (0 = none). */
public record BlindPacket(int remainingMillis) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlindPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(KatamariIO.MODID, "blind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlindPacket> STREAM_CODEC =
            StreamCodec.of(BlindPacket::write, BlindPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, BlindPacket packet) {
        buf.writeVarInt(packet.remainingMillis);
    }

    private static BlindPacket read(RegistryFriendlyByteBuf buf) {
        return new BlindPacket(buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
