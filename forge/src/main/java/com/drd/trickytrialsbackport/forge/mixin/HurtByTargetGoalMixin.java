package com.drd.trickytrialsbackport.forge.mixin;

import com.drd.trickytrialsbackport.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HurtByTargetGoal.class)
public abstract class HurtByTargetGoalMixin extends TargetGoal {
    public HurtByTargetGoalMixin(Mob mob, boolean mustSee) {
        super(mob, mustSee);
    }

    @Inject(method = "m_8036_", at = @At("HEAD"), cancellable = true, remap = false)
    private void ignoreBreezeAttacker(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob != null && this.mob.getLastHurtByMob() instanceof Breeze) {
            cir.setReturnValue(false);
        }
    }
}