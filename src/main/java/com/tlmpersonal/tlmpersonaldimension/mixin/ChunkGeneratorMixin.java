package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.worldgen.accessor.ChunkGeneratorAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * DISABLED FOR FORGE 1.20.1 COMPATIBILITY
 * 
 * This mixin was designed to filter structures in personal dimensions,
 * but the target method "tryGenerateStructure" does not exist in Forge 1.20.1.
 * It was added in later Minecraft versions (1.21+).
 * 
 * In Forge 1.20.1, structure generation uses a different system.
 * To implement structure filtering for 1.20.1, you would need to:
 * 1. Use Forge's StructureModifier system (data-driven approach), or
 * 2. Hook into WorldGenRegion or other structure placement methods, or
 * 3. Use events like StructureGrowEvent if available
 * 
 * For now, all structures will generate normally in personal dimensions.
 * The config options (ENABLE_STRUCTURES, STRUCTURE_WHITELIST, STRUCTURE_BLACKLIST)
 * are defined but not actively enforced at runtime.
 * 
 * Original functionality from 1.21.1:
 * - Prevented non-whitelisted structures when ENABLE_STRUCTURES is false
 * - Blocked blacklisted structures when ENABLE_STRUCTURES is true
 * - Always allowed the mod's island structure
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements ChunkGeneratorAccessor {

    @Unique
    @Nullable
    private ResourceKey<Level> tlmpersonal$dimensionKey = null;

    @Override
    public void tlmpersonal$setDimensionKey(ResourceKey<Level> dimensionKey) {
        this.tlmpersonal$dimensionKey = dimensionKey;
    }

    @Override
    @Nullable
    public ResourceKey<Level> tlmpersonal$getDimensionKey() {
        return this.tlmpersonal$dimensionKey;
    }

    // Structure filtering code removed - method doesn't exist in 1.20.1
    // See class documentation for details
}
