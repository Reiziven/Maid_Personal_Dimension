package com.tlmpersonal.tlmpersonaldimension;

import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionGui;
import com.tlmpersonal.tlmpersonaldimension.particle.StraightCherryParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class TouhoulittlemaidpersonaldimensionClient {
    private static CompoundTag lastSettings = new CompoundTag();
    private static boolean allowCheatConfigs = false;

    public TouhoulittlemaidpersonaldimensionClient() {
        // Register config screen
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ModConfigScreen.create(parent)));

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::registerParticleProviders);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Touhoulittlemaidpersonaldimension.DOMAIN_EXPANSION_ENTITY.get(), NoopRenderer::new);
        event.registerEntityRenderer(Touhoulittlemaidpersonaldimension.CHERRY_DOMAIN_ENTITY.get(), NoopRenderer::new);
        event.registerEntityRenderer(Touhoulittlemaidpersonaldimension.CAT_FAMILIAR_ENTITY.get(),
                net.minecraft.client.renderer.entity.CatRenderer::new);
    }

    private void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Touhoulittlemaidpersonaldimension.STRAIGHT_CHERRY_PARTICLE.get(),
                spriteSet -> new StraightCherryParticle.Provider(spriteSet));
    }

    public static void handleSettingsSync(CompoundTag settings, boolean allowCheats) {
        lastSettings = settings;
        allowCheatConfigs = allowCheats;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PersonalDimensionGui gui) {
            PersonalDimensionSavedData.PlayerDimensionSettings playerSettings =
                    PersonalDimensionSavedData.PlayerDimensionSettings.load(settings);
            gui.updateSettings(playerSettings);
        }
    }

    public static CompoundTag getLastSettings() { return lastSettings; }
    public static boolean isAllowCheatConfigs() { return allowCheatConfigs; }
}
