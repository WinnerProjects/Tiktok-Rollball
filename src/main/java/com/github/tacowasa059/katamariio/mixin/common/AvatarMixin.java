package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Since 1.21.9 the player dimensions are declared on Avatar (Player's superclass), not on Player itself.
 * The eye height now lives in EntityDimensions, which replaces the old getStandingEyeHeight override.
 */
@Mixin(Avatar.class)
public abstract class AvatarMixin {
    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void katamariIO$ballDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Object self = this;
        if (self instanceof Player player && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            float size = ((ICustomPlayerData) player).katamariIO$getSize();
            cir.setReturnValue(EntityDimensions.scalable(size, size).withEyeHeight(0.85F * size));
        }
    }
}
