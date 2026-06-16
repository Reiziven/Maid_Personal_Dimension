package com.tlmpersonal.tlmpersonaldimension.inventory;

import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PersonalDimensionMenu extends AbstractMaidContainer {
    private static final int PLAYER_INVENTORY_SIZE = 27;

    public PersonalDimensionMenu(int id, Inventory inventory, int entityId) {
        super(Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_MENU.get(), id, inventory, entityId);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack stack1 = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack2 = slot.getItem();
            stack1 = stack2.copy();
            if (index < PLAYER_INVENTORY_SIZE) {
                if (!this.moveItemStackTo(stack2, PLAYER_INVENTORY_SIZE, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack2, 0, PLAYER_INVENTORY_SIZE, true)) {
                return ItemStack.EMPTY;
            }
            if (stack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack1;
    }
}
