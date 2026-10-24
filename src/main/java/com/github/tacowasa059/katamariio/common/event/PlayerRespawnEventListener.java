package com.github.tacowasa059.katamariio.common.event;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.networks.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PlayerRespawnEventListener {
    public static void onPlayerClone(Player originalPlayer, Player newPlayer) {
        ICustomPlayerData originalPlayerData = (ICustomPlayerData) originalPlayer;
        ICustomPlayerData newPlayerData = (ICustomPlayerData) newPlayer;

        newPlayerData.katamariIO$setFlagAndSizeAndRestitution(
                originalPlayerData.katamariIO$getFlag(),
                originalPlayerData.katamariIO$getSize(),
                originalPlayerData.katamariIO$getRenderSize(),
                originalPlayerData.katamariIO$getRestitutionCoefficient());
        // Size and attached blocks are kept after death / dimension change.
        newPlayerData.katamariIO$restoreBlocks(
                originalPlayerData.katamariIO$getSphericalPlayerBlocks(),
                originalPlayerData.katamariIO$getAttachedBlockCount());
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (player != null) {
            ICustomPlayerData data = (ICustomPlayerData) player;
            ModNetwork.sendInitialData(
                    player,
                    data.katamariIO$getSize(),
                    data.katamariIO$getRenderSize(),
                    data.katamariIO$getFlag(),
                    data.katamariIO$getRestitutionCoefficient(),
                    data.katamariIO$getQuaternion(),
                    data.katamariIO$getCurrentPosition()
            );
        }
    }
}
