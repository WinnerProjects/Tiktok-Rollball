package com.github.tacowasa059.katamariio.client.state;

import net.minecraft.client.player.AbstractClientPlayer;

/** Lets the (entity-less) AvatarRenderState remember which player it was extracted from. */
public interface IKatamariRenderState {
    AbstractClientPlayer katamariIO$getPlayer();

    void katamariIO$setPlayer(AbstractClientPlayer player);
}
