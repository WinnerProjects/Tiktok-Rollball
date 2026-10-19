package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.KatamariPlayerRenderer;
import com.github.tacowasa059.katamariio.client.state.IKatamariRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the old PlayerRenderer#render hook. In 1.21.9+ entities are rendered through
 * {@code submit(state, poseStack, collector, cameraState)}.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState,
        M extends EntityModel<? super S>> extends EntityRenderer<T, S> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void katamariIO$submitBall(S state, PoseStack poseStack, SubmitNodeCollector collector,
                                       CameraRenderState cameraState, CallbackInfo ci) {
        Object stateObject = state;
        Object rendererObject = this;
        if (!(stateObject instanceof AvatarRenderState avatarState)
                || !(stateObject instanceof IKatamariRenderState holder)
                || !(rendererObject instanceof AvatarRenderer<?> avatarRenderer)) {
            return;
        }

        AbstractClientPlayer player = holder.katamariIO$getPlayer();
        if (player == null) {
            return;
        }

        Identifier texture = avatarRenderer.getTextureLocation(avatarState);
        if (KatamariPlayerRenderer.submit(player, texture, state.lightCoords, poseStack, collector)) {
            // EntityRenderer#submit only handles the name tag / leash, so the player's name stays visible.
            super.submit(state, poseStack, collector, cameraState);
            ci.cancel();
        }
    }
}
