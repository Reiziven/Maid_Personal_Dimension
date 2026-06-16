package com.tlmpersonal.tlmpersonaldimension.world;

import com.tlmpersonal.tlmpersonaldimension.Config;
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
        ResourceLocation.fromNamespaceAndPath(Touhoulittlemaidpersonaldimension.MODID, "my_island");

    private static Map<ResourceKey<Level>, Set<BlockPos>> placedIslands = new HashMap<>();

    public static void resetForNewServer() {
        placedIslands.clear();
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Resetting placedIslands for new server start!");
    }

    public static void tryPlaceStructure(ServerLevel level) {
        Set<BlockPos> levelIslands = placedIslands.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        if (levelIslands.contains(BlockPos.ZERO)) return;

        // Place structure at Y=100 for normal/cherry dimensions, Y=64 for void
        int spawnY = Config.DIMENSION_TYPE.get() == Config.DimensionType.VOID ? 64 : 100;
        placeStructureAt(level, new BlockPos(0, spawnY, 0));
        levelIslands.add(BlockPos.ZERO);
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
        int spawnY = 100;
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
        Touhoulittlemaidpersonaldimension.LOGGER.info("[StructurePlacer] Placing structure: {} at {}", STRUCTURE_ID, pos);
        
        StructureTemplateManager manager = level.getStructureManager();
        StructureTemplate template = manager.getOrCreate(STRUCTURE_ID);
        
        if (template.getSize().getX() == 0) {
            Touhoulittlemaidpersonaldimension.LOGGER.error("[StructurePlacer] ❌ Structure not found: {}", STRUCTURE_ID);
            return;
        }
        
        BlockPos offset = new BlockPos(-template.getSize().getX() / 2, 0, -template.getSize().getZ() / 2);
        BlockPos finalPos = pos.offset(offset);

        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        template.placeInWorld(level, finalPos, finalPos, settings, level.getRandom(), 2);
    }
}
