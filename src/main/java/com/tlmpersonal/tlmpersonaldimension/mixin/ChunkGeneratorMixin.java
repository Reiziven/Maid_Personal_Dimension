package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.worldgen.accessor.ChunkGeneratorAccessor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Prevents non-whitelisted structures from generating in personal dimensions
 * when ENABLE_STRUCTURES is false.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements ChunkGeneratorAccessor {

    // Hardcoded: our island is always allowed regardless of config
    @Unique
    private static final String HARDCODED_ISLAND =
            Touhoulittlemaidpersonaldimension.MODID + ":my_island";

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

    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void onTryGenerateStructure(
            StructureSet.StructureSelectionEntry structureEntry,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState randomState,
            StructureTemplateManager templateManager,
            long seed,
            ChunkAccess chunk,
            ChunkPos chunkPos,
            SectionPos sectionPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Only act on our personal dimensions
        if (!Touhoulittlemaidpersonaldimension.isOurDimension(tlmpersonal$dimensionKey)) return;

        // Get this structure's resource location
        var structureKey = structureEntry.structure().unwrapKey();
        if (structureKey.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        ResourceLocation structureId = structureKey.get().location();
        String idString = structureId.toString();

        if (Config.ENABLE_STRUCTURES.get()) {
            // Structures enabled — block anything in the blacklist
            List<? extends String> blacklist = Config.STRUCTURE_BLACKLIST.get();
            for (String entry : blacklist) {
                if (entry.equals(idString)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        } else {
            // Structures disabled — only allow hardcoded island and whitelist
            if (idString.equals(HARDCODED_ISLAND)) return;
            List<? extends String> whitelist = Config.STRUCTURE_WHITELIST.get();
            for (String entry : whitelist) {
                if (entry.equals(idString)) return;
            }
            cir.setReturnValue(false);
        }
    }
}
