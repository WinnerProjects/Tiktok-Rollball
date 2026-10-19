package com.github.tacowasa059.katamariio.client;

import com.github.tacowasa059.katamariio.common.networks.RemoveBlockPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class NeighboringBlockUtils {

    public static void processNeighboring(Player player, BlockPos blockPos) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.equals(player)) {
            ClientPlayNetworking.send(new RemoveBlockPacket(blockPos, player.position()));
        }
    }
}
