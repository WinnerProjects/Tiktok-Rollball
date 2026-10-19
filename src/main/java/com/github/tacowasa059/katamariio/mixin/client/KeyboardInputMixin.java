package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    // 1.21.2+: KeyboardInput#tick() has no arguments and the pressed keys live in the immutable Input record.
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void katamariIO$disableSneak(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) player;
        KeyboardInput keyboardInput = (KeyboardInput) (Object) this;
        Input keys = keyboardInput.keyPresses;
        if (keys.shift() && player.onGround() && !player.isPassenger() && playerData.katamariIO$getFlag()) {
            keyboardInput.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                    keys.jump(), false, keys.sprint());
        }
    }
}
