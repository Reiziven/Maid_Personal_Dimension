package com.tlmpersonal.tlmpersonaldimension.accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public interface MinecraftServerAccessor {
    boolean tlmpersonal$createWorld(ResourceKey<Level> key, ResourceLocation dimensionTypeKey);
    void tlmpersonal$removeWorld(ResourceKey<Level> key);
}
