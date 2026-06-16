package com.tlmpersonal.tlmpersonaldimension;

import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionGui;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class TouhoulittlemaidpersonaldimensionClient {
    private static CompoundTag lastSettings = new CompoundTag();
    private static boolean allowCheatConfigs = false;

    public TouhoulittlemaidpersonaldimensionClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (container1, parent) -> ModConfigScreen.create(parent));
    }

    public static void handleSettingsSync(CompoundTag settings, boolean allowCheats) {
        lastSettings = settings;
        allowCheatConfigs = allowCheats;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PersonalDimensionGui gui) {
            PersonalDimensionSavedData.PlayerDimensionSettings playerSettings = PersonalDimensionSavedData.PlayerDimensionSettings.load(settings);
            gui.updateSettings(playerSettings);
        }
    }

    public static CompoundTag getLastSettings() {
        return lastSettings;
    }

    public static boolean isAllowCheatConfigs() {
        return allowCheatConfigs;
    }
}
