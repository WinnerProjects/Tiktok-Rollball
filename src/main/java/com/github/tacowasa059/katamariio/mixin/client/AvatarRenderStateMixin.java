package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.state.IKatamariRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements IKatamariRenderState {
    @Unique
    private AbstractClientPlayer katamariIO$player;

    @Override
    public AbstractClientPlayer katamariIO$getPlayer() {
        return katamariIO$player;
    }

    @Override
    public void katamariIO$setPlayer(AbstractClientPlayer player) {
        this.katamariIO$player = player;
    }
}
