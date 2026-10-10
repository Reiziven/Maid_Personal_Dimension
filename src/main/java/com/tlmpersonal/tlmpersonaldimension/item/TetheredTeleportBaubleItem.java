package com.tlmpersonal.tlmpersonaldimension.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TetheredTeleportBaubleItem extends Item {
    public TetheredTeleportBaubleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltips, flag);
        if (Screen.hasShiftDown()) {
            tooltips.add(Component.translatable("tooltip.tethered_teleport_bauble.header"));
            tooltips.add(Component.literal(""));
            tooltips.add(Component.translatable("tooltip.tethered_teleport_bauble.desc"));
        } else {
            tooltips.add(Component.translatable("tooltip.tethered_teleport_bauble.hint"));
        }
    }
}
