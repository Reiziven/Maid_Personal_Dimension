package com.tlmpersonal.tlmpersonaldimension.item;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableFloat;
import net.minecraft.world.entity.Entity;

public class DomainExpansionBauble implements IMaidBauble {

    @Override
    public boolean onInjured(EntityMaid maid, ItemStack baubleItem, DamageSource source, MutableFloat damage) {
        // Trigger domain expansion when maid is attacked, if conditions are met
        // We will implement the trigger logic inside a helper method in Touhoulittlemaidpersonaldimension
        // so it can be called from both maid being attacked and owner being attacked.
        com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.tryTriggerDomainExpansion(maid);
        return false;
    }
}
