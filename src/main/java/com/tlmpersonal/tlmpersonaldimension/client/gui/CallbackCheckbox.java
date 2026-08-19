package com.tlmpersonal.tlmpersonaldimension.client.gui;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

/**
 * Forge 1.20.1 compatible Checkbox with callback support.
 * NeoForge 1.21+ has onValueChange in builder, but 1.20.1 doesn't.
 */
public class CallbackCheckbox extends Checkbox {
    private final BiConsumer<Checkbox, Boolean> callback;

    public CallbackCheckbox(int x, int y, int width, int height,
                           Component message, boolean selected,
                           BiConsumer<Checkbox, Boolean> callback) {
        super(x, y, width, height, message, selected);
        this.callback = callback;
    }

    @Override
    public void onPress() {
        super.onPress();
        if (callback != null) {
            callback.accept(this, this.selected());
        }
    }
}
