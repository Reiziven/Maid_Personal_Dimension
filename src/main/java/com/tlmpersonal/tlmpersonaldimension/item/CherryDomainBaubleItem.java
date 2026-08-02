package com.tlmpersonal.tlmpersonaldimension.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CherryDomainBaubleItem extends Item {
    public CherryDomainBaubleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltips, flag);
        if (Screen.hasShiftDown()) {
            tooltips.add(Component.translatable("tooltip.cherry_domain_bauble.header"));
            tooltips.add(Component.literal(""));
            tooltips.add(Component.translatable("tooltip.cherry_domain_bauble.aura"));
            tooltips.add(Component.translatable("tooltip.cherry_domain_bauble.floor"));
            tooltips.add(Component.translatable("tooltip.cherry_domain_bauble.replace"));
            tooltips.add(Component.translatable("tooltip.cherry_domain_bauble.leaves"));
            tooltips.add(Component.translatable("tooltip.cherry_domain_bauble.rules"));
        } else {
            tooltips.add(Component.translatable("tooltip.cherry_domain_bauble.hint"));
        }
    }
}
