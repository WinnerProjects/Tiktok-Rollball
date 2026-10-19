package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.state.IKatamariRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void katamariIO$rememberPlayer(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (state instanceof IKatamariRenderState holder) {
            holder.katamariIO$setPlayer(avatar instanceof AbstractClientPlayer player ? player : null);
        }
    }
}
