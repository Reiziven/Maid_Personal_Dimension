package com.tlmpersonal.tlmpersonaldimension.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class CatReflexesEffect extends MobEffect {
    public CatReflexesEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF9933); // Orange color
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is passive, handled in event listener
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
