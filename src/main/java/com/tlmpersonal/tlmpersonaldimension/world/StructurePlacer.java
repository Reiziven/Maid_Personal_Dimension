package com.tlmpersonal.tlmpersonaldimension.world;

import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.*;

public class StructurePlacer {

    private static final ResourceLocation STRUCTURE_ID =
        new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "my_island");

    private static Map<ResourceKey<Level>, Set<BlockPos>> placedIslands = new HashMap<>();

    public static void resetForNewServer() {
        placedIslands.clear();
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Resetting placedIslands for new server start!");
    }

    private static int getSafeSpawnY(ServerLevel level, int x, int z) {
        return 150;
    }

    public static void tryPlaceStructure(ServerLevel level) {
        PersonalDimensionSavedData data = PersonalDimensionSavedData.get(level);
        if (data.isIslandPlaced(level.dimension())) {
            Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Island already placed for dimension: {}", level.dimension().location());
            return;
        }

        int spawnY = getSafeSpawnY(level, 0, 0);
        placeStructureAt(level, new BlockPos(0, spawnY, 0));
        data.markIslandPlaced(level.dimension());
    }

    /**
     * Force structure placement without any checks - used on dimension creation
     */
    public static void forceStructurePlacement(ServerLevel level) {
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] FORCE PLACEMENT starting for dimension: {}", level.dimension().location());
        
        int spawnY = getSafeSpawnY(level, 0, 0);
        BlockPos targetPos = new BlockPos(0, spawnY, 0);
        
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Target position: {}", targetPos);
        
        // Place structure immediately
        placeStructureAt(level, targetPos);
        
        // Mark as placed in saved data
        try {
            PersonalDimensionSavedData data = PersonalDimensionSavedData.get(level);
            data.markIslandPlaced(level.dimension());
            Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Marked island as placed in saved data");
        } catch (Exception e) {
            Touhoulittlemaidpersonaldimension.LOGGER.error("[StructurePlacer] Failed to mark island as placed", e);
        }
    }
    
    public static void trySpawnNaturalIsland(ServerLevel level) {
        // Only natural spawn in normal/cherry dimensions
        if (Config.DIMENSION_TYPE.get() == Config.DimensionType.VOID) return;
        
        if (level.players().isEmpty()) return;
        
        Player player = level.players().iterator().next();
        BlockPos playerPos = player.blockPosition();
        
        // Generate random position within 64-128 blocks of player
        int offsetX = playerPos.getX() + level.random.nextInt(128) - 64;
        int offsetZ = playerPos.getZ() + level.random.nextInt(128) - 64;
        int spawnY = getSafeSpawnY(level, offsetX, offsetZ);
        BlockPos spawnPos = new BlockPos(offsetX, spawnY, offsetZ);
        
        Set<BlockPos> levelIslands = placedIslands.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        if (levelIslands.contains(spawnPos)) return;
        
        // Check if we're too close to an existing island
        for (BlockPos existing : levelIslands) {
            if (existing.distSqr(spawnPos) < 256 * 256) { // 256 block distance squared
                return;
            }
        }
        
        placeStructureAt(level, spawnPos);
        levelIslands.add(spawnPos);
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Naturally spawned my_island at {}", spawnPos);
    }

    public static void placeSkyIsland(ServerLevel level, BlockPos pos) {
        placeStructureAt(level, pos);
    }

    private static void placeStructureAt(ServerLevel level, BlockPos pos) {
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] ===== STARTING STRUCTURE PLACEMENT =====");
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Dimension: {}", level.dimension().location());
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Structure ID: {}", STRUCTURE_ID);
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Target Position: {}", pos);
        
        // Try manual NBT loading as fallback
        StructureTemplate template = loadStructureManually(level, STRUCTURE_ID);
        if (template == null) {
            Touhoulittlemaidpersonaldimension.LOGGER.error("[StructurePlacer] ❌❌❌ Failed to load structure manually: {}", STRUCTURE_ID);
            return;
        }
        
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Template size: X={}, Y={}, Z={}", 
            template.getSize().getX(), template.getSize().getY(), template.getSize().getZ());
        
        if (template.getSize().getX() == 0) {
            Touhoulittlemaidpersonaldimension.LOGGER.error("[StructurePlacer] ❌❌❌ Structure has ZERO size!");
            return;
        }
        
        BlockPos offset = new BlockPos(-template.getSize().getX() / 2, 0, -template.getSize().getZ() / 2);
        BlockPos finalPos = pos.offset(offset);
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Final position after centering: {}", finalPos);

        // Force load chunks where structure will be placed
        int minChunkX = finalPos.getX() >> 4;
        int minChunkZ = finalPos.getZ() >> 4;
        int maxChunkX = (finalPos.getX() + template.getSize().getX()) >> 4;
        int maxChunkZ = (finalPos.getZ() + template.getSize().getZ()) >> 4;
        
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Loading chunks from ({}, {}) to ({}, {})", 
            minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        
        int chunksLoaded = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
                chunksLoaded++;
            }
        }
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Loaded {} chunks", chunksLoaded);

        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Calling template.placeInWorld...");
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        boolean placed = false;
        try {
            placed = template.placeInWorld(level, finalPos, finalPos, settings, level.getRandom(), 2);
            Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] template.placeInWorld returned: {}", placed);
        } catch (Exception e) {
            Touhoulittlemaidpersonaldimension.LOGGER.warn("[StructurePlacer] Exception during structure placement (some blocks may have failed): {}", e.getMessage());
            placed = true; // Continue anyway, partial placement is better than nothing
        }
        
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] ✓✓✓ STRUCTURE PLACEMENT COMPLETED at {}", pos);
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] ===== END STRUCTURE PLACEMENT =====");
    }

    /**
     * Manually load structure NBT file when StructureTemplateManager fails
     */
    private static StructureTemplate loadStructureManually(ServerLevel level, ResourceLocation structureId) {
        try {
            // Try server's structure manager first
            StructureTemplateManager manager = level.getServer().getStructureManager();
            Optional<StructureTemplate> templateOpt = manager.get(structureId);
            
            if (templateOpt.isPresent()) {
                Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Loaded via StructureTemplateManager");
                return templateOpt.get();
            }
            
            // Fallback: manual NBT loading from resources
            Touhoulittlemaidpersonaldimension.LOGGER.warn("[StructurePlacer] StructureTemplateManager failed, trying manual NBT load");

            // In 1.20.1 the folder is "structures" (plural). It was renamed to "structure" in 1.21.
            String[] candidatePaths = {
                "data/" + structureId.getNamespace() + "/structures/" + structureId.getPath() + ".nbt"
            };

            for (String resourcePath : candidatePaths) {
                try (java.io.InputStream stream = StructurePlacer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (stream == null) {
                        Touhoulittlemaidpersonaldimension.LOGGER.warn("[StructurePlacer] Not found at: {}", resourcePath);
                        continue;
                    }
                    net.minecraft.nbt.CompoundTag nbt = net.minecraft.nbt.NbtIo.readCompressed(stream);
                    StructureTemplate template = new StructureTemplate();
                    template.load(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), nbt);
                    Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Manually loaded NBT from: {}", resourcePath);
                    return template;
                }
            }
            Touhoulittlemaidpersonaldimension.LOGGER.error("[StructurePlacer] Structure not found in any resource path for: {}", structureId);
            return null;
            
        } catch (Exception e) {
            Touhoulittlemaidpersonaldimension.LOGGER.error("[StructurePlacer] Exception during manual structure load", e);
            return null;
        }
    }
}