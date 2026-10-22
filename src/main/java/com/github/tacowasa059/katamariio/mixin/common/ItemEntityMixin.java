package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The ball does not pick up items, so nothing lands in the inventory. */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true, require = 0)
    private void katamariIO$noPickup(Player player, CallbackInfo ci) {
        if (player instanceof ICustomPlayerData data && data.katamariIO$getFlag()) {
            ci.cancel();
        }
    }
}
