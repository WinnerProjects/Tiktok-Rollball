package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Vec3ListSerializer implements EntityDataSerializer<List<Vec3>> {
    private static final double FIXED_SCALE = 256.0;

    private static int toFixed(double value) {
        return (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value * FIXED_SCALE)));
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Vec3>> CODEC = StreamCodec.of(
            (buf, list) -> {
                buf.writeVarInt(list.size());
                for (Vec3 v : list) {
                    // Fixed point 1/256 in a short: 6 bytes instead of 24 (block positions are small lattice values).
                    buf.writeShort(toFixed(v.x));
                    buf.writeShort(toFixed(v.y));
                    buf.writeShort(toFixed(v.z));
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<Vec3> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(new Vec3(buf.readShort() / FIXED_SCALE, buf.readShort() / FIXED_SCALE, buf.readShort() / FIXED_SCALE));
                }
                return list;
            });

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, List<Vec3>> codec() {
        return CODEC;
    }

    @Override
    public List<Vec3> copy(List<Vec3> list) {
        return new ArrayList<>(list);
    }
}
