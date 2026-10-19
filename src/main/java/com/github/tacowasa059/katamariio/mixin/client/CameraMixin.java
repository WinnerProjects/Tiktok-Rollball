package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.KatamariPlayerRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    // Keeps the third person camera outside the (growing) ball instead of clipping it against blocks.
    @Inject(method = "getMaxZoom(F)F", at = @At("HEAD"), cancellable = true, require = 0)
    private void katamariIO$cameraDistance(float maxZoom, CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            cir.setReturnValue((float) (KatamariPlayerRenderer.MAX_SIZE * 2));
        }
    }
}
