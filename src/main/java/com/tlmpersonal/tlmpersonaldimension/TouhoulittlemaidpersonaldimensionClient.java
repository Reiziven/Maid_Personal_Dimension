package com.tlmpersonal.tlmpersonaldimension;

import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionGui;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;

public class TouhoulittlemaidpersonaldimensionClient {
    private static CompoundTag lastSettings = new CompoundTag();
    private static boolean allowCheatConfigs = false;

    public TouhoulittlemaidpersonaldimensionClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (container1, parent) -> ModConfigScreen.create(parent));
        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(this::registerEntityRenderers);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Touhoulittlemaidpersonaldimension.DOMAIN_EXPANSION_ENTITY.get(), NoopRenderer::new);
        event.registerEntityRenderer(Touhoulittlemaidpersonaldimension.CHERRY_DOMAIN_ENTITY.get(), NoopRenderer::new);
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
