package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class QuaternionfListSerializer implements EntityDataSerializer<List<Quaternionf>> {
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Quaternionf>> CODEC = StreamCodec.of(
            (buf, list) -> {
                buf.writeVarInt(list.size());
                for (Quaternionf q : list) {
                    // Shell blocks are never rotated: 1 byte for the identity, full quaternion only for old data.
                    if (q.x() == 0.0f && q.y() == 0.0f && q.z() == 0.0f && q.w() == 1.0f) {
                        buf.writeByte(0);
                    } else {
                        buf.writeByte(1);
                        buf.writeFloat(q.x());
                        buf.writeFloat(q.y());
                        buf.writeFloat(q.z());
                        buf.writeFloat(q.w());
                    }
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<Quaternionf> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    if (buf.readByte() == 0) {
                        list.add(new Quaternionf());
                    } else {
                        list.add(new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
                    }
                }
                return list;
            });

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, List<Quaternionf>> codec() {
        return CODEC;
    }

    @Override
    public List<Quaternionf> copy(List<Quaternionf> list) {
        List<Quaternionf> copy = new ArrayList<>(list.size());
        for (Quaternionf q : list) {
            copy.add(new Quaternionf(q));
        }
        return copy;
    }
}
