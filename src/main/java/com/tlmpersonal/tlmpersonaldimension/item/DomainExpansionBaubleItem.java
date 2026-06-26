package com.tlmpersonal.tlmpersonaldimension.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DomainExpansionBaubleItem extends Item {
    public DomainExpansionBaubleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltips, flag);
        if (Screen.hasShiftDown()) {
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.header"));
            tooltips.add(Component.literal(""));
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.trigger"));
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.structure"));
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.rules"));
            tooltips.add(Component.literal(""));
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.effects_header"));
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.effect.ally"));
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.effect.enemy"));
            tooltips.add(Component.literal(""));
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.cooldown"));
        } else {
            tooltips.add(Component.translatable("tooltip.domain_expansion_bauble.hint"));
        }
    }
}
