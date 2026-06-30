package com.tlmpersonal.tlmpersonaldimension.worldgen.accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface ChunkGeneratorAccessor {
    void tlmpersonal$setDimensionKey(ResourceKey<Level> dimensionKey);

    @Nullable
    ResourceKey<Level> tlmpersonal$getDimensionKey();
}
