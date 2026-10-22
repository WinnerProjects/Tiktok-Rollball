package com.github.tacowasa059.katamariio.common.serializers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockListSerializer implements EntityDataSerializer<List<Block>> {
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Block>> CODEC = StreamCodec.of(
            (buf, blocks) -> {
                buf.writeVarInt(blocks.size());
                for (Block block : blocks) {
                    // Raw registry id (1-2 bytes) instead of the full "minecraft:stone" identifier string.
                    buf.writeVarInt(BuiltInRegistries.BLOCK.getId(block));
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<Block> blocks = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    blocks.add(BuiltInRegistries.BLOCK.byId(buf.readVarInt()));
                }
                return blocks;
            });

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, List<Block>> codec() {
        return CODEC;
    }

    @Override
    public List<Block> copy(List<Block> blocks) {
        return new ArrayList<>(blocks);
    }
}
