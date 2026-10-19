package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true, require = 0)
    public void modifyKnockback(double strength, double ratioX, double ratioZ, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        strength = strength * (1.0f - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (strength <= 0.0F) {
            return;
        }

        entity.needsSync = true;
        Vec3 currentMotion = entity.getDeltaMovement();
        Vec3 knockbackVector = (new Vec3(ratioX, 0.0, ratioZ)).normalize().scale(strength);
        entity.setDeltaMovement(currentMotion.x / 2.0 - knockbackVector.x,
                currentMotion.y / 2.0 + strength / 2.0 + 0.1D, currentMotion.z / 2.0 - knockbackVector.z);
        ci.cancel();
    }

    @Inject(method = "getFrictionInfluencedSpeed", at = @At("HEAD"), cancellable = true, require = 0)
    public void modifyFrictionInfluencedSpeed(float friction, CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        if (!entity.onGround()) {
            cir.setReturnValue(entity.getSpeed() * (0.21600002F / (friction * friction * friction)));
        }
    }

    // 1.21.2+: getJumpPower() delegates to getJumpPower(float). Both are covered so the boosted jump is applied
    // whichever one the game ends up calling.
    @Inject(method = "getJumpPower()F", at = @At("HEAD"), cancellable = true, require = 0)
    protected void katamariIO$getJumpPower(CallbackInfoReturnable<Float> cir) {
        katamariIO$applyJumpPower(1.0F, cir);
    }

    @Inject(method = "getJumpPower(F)F", at = @At("HEAD"), cancellable = true, require = 0)
    protected void katamariIO$getJumpPowerScaled(float multiplier, CallbackInfoReturnable<Float> cir) {
        katamariIO$applyJumpPower(multiplier, cir);
    }

    @Unique
    private void katamariIO$applyJumpPower(float multiplier, CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }
        ICustomPlayerData playerData = (ICustomPlayerData) entity;
        if (!playerData.katamariIO$getFlag()) {
            return;
        }

        float jumpPower = 0.42F * multiplier * this.katamariIO$getJumpFactor();
        jumpPower *= 1.75f;
        cir.setReturnValue(jumpPower);
    }

    @Unique
    protected float katamariIO$getJumpFactor() {
        LivingEntity entity = (LivingEntity) (Object) this;
        float blockJumpFactor = entity.level().getBlockState(entity.blockPosition()).getBlock().getJumpFactor();
        BlockPos pos = this.getOnPos(0.500001F);
        float onPosJumpFactor = entity.level().getBlockState(pos).getBlock().getJumpFactor();
        return (double) blockJumpFactor == 1.0 ? onPosJumpFactor : blockJumpFactor;
    }
}
