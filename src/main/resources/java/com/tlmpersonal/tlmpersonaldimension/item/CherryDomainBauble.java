package com.tlmpersonal.tlmpersonaldimension.item;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;

public class CherryDomainBauble implements IMaidBauble {

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.tickCherryDomain(maid);
    }
}
