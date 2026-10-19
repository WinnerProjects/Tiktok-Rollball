package com.github.tacowasa059.katamariio.mixin.common;

import com.github.tacowasa059.katamariio.common.KatamariTravel;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the old "inject before setDeltaMovement(ordinal 2/3)" hook of LivingEntity#travel.
 * Since 1.21.2 the air movement lives in {@code travelInAir(Vec3)}, so the ball motion is applied there.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTravelMixin extends Entity {
    public LivingEntityTravelMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    protected abstract Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 input, float friction);

    @Inject(method = "travelInAir", at = @At("HEAD"), cancellable = true)
    private void katamariIO$travelInAir(Vec3 travelVector, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof Player player) || !((ICustomPlayerData) player).katamariIO$getFlag()) {
            return;
        }

        BlockPos posBelow = this.getBlockPosBelowThatAffectsMyMovement();
        float friction = this.onGround() ? this.level().getBlockState(posBelow).getBlock().getFriction() : 1.0F;
        Vec3 movement = this.handleRelativeFrictionAndCalculateMovement(travelVector, friction);
        KatamariTravel.applyCustomMotion((LivingEntity) self, movement, ci);
    }
}
