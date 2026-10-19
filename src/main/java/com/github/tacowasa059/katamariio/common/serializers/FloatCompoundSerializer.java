package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;

/**
 * Synced CompoundTag that only holds float entries (quaternion / position).
 * EntityDataSerializers.COMPOUND_TAG is gone in 1.21.11, so the tag is encoded by hand.
 */
public class FloatCompoundSerializer implements EntityDataSerializer<CompoundTag> {
    private static final StreamCodec<RegistryFriendlyByteBuf, CompoundTag> CODEC = StreamCodec.of(
            (buf, tag) -> {
                buf.writeVarInt(tag.size());
                for (String key : tag.keySet()) {
                    buf.writeUtf(key);
                    buf.writeFloat(tag.getFloatOr(key, 0f));
                }
            },
            buf -> {
                int size = buf.readVarInt();
                CompoundTag tag = new CompoundTag();
                for (int i = 0; i < size; i++) {
                    String key = buf.readUtf();
                    tag.putFloat(key, buf.readFloat());
                }
                return tag;
            });

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, CompoundTag> codec() {
        return CODEC;
    }

    @Override
    public CompoundTag copy(CompoundTag tag) {
        return tag.copy();
    }
}
