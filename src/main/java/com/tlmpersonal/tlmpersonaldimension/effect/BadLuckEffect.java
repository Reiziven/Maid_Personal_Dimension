package com.tlmpersonal.tlmpersonaldimension.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BadLuckEffect extends MobEffect {
    public static final float MISS_CHANCE = 0.10f; // 10% chance to miss

    public BadLuckEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B0082); // Dark purple color
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Passive — miss logic handled in event listener
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
