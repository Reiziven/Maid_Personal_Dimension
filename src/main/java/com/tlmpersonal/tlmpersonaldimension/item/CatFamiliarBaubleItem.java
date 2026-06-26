package com.tlmpersonal.tlmpersonaldimension.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CatFamiliarBaubleItem extends Item {
    public CatFamiliarBaubleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltips, flag);
        if (Screen.hasShiftDown()) {
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.header"));
            tooltips.add(Component.literal(""));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.summon"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.attack"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.effects_header"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.effect.dodge"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.effect.taunt"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.effect.bad_luck"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.effect.fall"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.effect.luck"));
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.effect.detection"));
        } else {
            tooltips.add(Component.translatable("tooltip.cat_familiar_bauble.hint"));
        }
    }
}
