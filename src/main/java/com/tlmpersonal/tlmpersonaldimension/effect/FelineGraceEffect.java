package com.tlmpersonal.tlmpersonaldimension.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class FelineGraceEffect extends MobEffect {
    public static final float FALL_DAMAGE_REDUCTION = 0.70f; // 70% reduction

    public FelineGraceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAA44FF); // Purple color
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is passive — fall damage reduction handled in event listener
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
