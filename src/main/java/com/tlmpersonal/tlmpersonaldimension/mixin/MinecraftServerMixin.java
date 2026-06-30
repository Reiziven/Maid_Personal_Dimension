
package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.google.common.collect.ImmutableList;
import com.tlmpersonal.tlmpersonaldimension.PersonalLevelData;
import com.tlmpersonal.tlmpersonaldimension.PersonalLevelStateData;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.accessor.MinecraftServerAccessor;
import com.tlmpersonal.tlmpersonaldimension.worldgen.accessor.ChunkGeneratorAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<Runnable> implements MinecraftServerAccessor {

    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;

    @Shadow
    @Final
    private Executor executor;

    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow
    public abstract WorldData getWorldData();

    public MinecraftServerMixin(String name) {
        super(name);
    }

    @Override
    public boolean tlmpersonal$createWorld(ResourceKey<Level> key, ResourceLocation dimensionTypeKey) {
        try {
            MinecraftServer server = (MinecraftServer) (Object) this;

            if (levels.containsKey(key)) {
                Touhoulittlemaidpersonaldimension.LOGGER.debug("Dimension already exists: {}", key.location());
                return true;
            }

            var registryAccess = server.registryAccess();
            var dimensionRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
            LevelStem templateStem = dimensionRegistry.get(dimensionTypeKey);

            if (templateStem == null) {
                Touhoulittlemaidpersonaldimension.LOGGER.error("Template LevelStem not found for: {}", dimensionTypeKey);
                return false;
            }

            ServerLevel overworld = levels.get(Level.OVERWORLD);
            if (overworld == null) {
                Touhoulittlemaidpersonaldimension.LOGGER.error("Overworld not found, cannot create dimension");
                return false;
            }

            ServerLevelData overworldLevelData = (ServerLevelData) overworld.getLevelData();
            WorldData worldData = server.getWorldData();
            PersonalLevelData personalLevelData = new PersonalLevelData(worldData, overworldLevelData);

            long seed = BiomeManager.obfuscateSeed((long) (0x66ccff * Math.random()));

            ChunkProgressListener progressListener = new ChunkProgressListener() {
                @Override
                public void updateSpawnPos(ChunkPos pos) {}

                @Override
                public void onStatusChange(ChunkPos pChunkPosition, net.minecraft.world.level.chunk.status.ChunkStatus pNewStatus) {}

                @Override
                public void start() {}

                @Override
                public void stop() {}
            };

            ServerLevel newLevel = new ServerLevel(
                    server,
                    executor,
                    storageSource,
                    personalLevelData,
                    key,
                    templateStem,
                    progressListener,
                    overworld.isDebug(),
                    seed,
                    ImmutableList.of(),
                    true,
                    overworld.getRandomSequences()
            );

            PersonalLevelStateData.get(newLevel).attach(personalLevelData);

            // Inject dimension key into ChunkGenerator so structure filtering can check it
            if (newLevel.getChunkSource().getGenerator() instanceof ChunkGeneratorAccessor accessor) {
                accessor.tlmpersonal$setDimensionKey(key);
            }

            Touhoulittlemaidpersonaldimension.LOGGER.debug("Created dimension: {}", key.location());

            levels.put(key, newLevel);
            server.getPlayerList().addWorldborderListener(newLevel);
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(newLevel));

            try {
                newLevel.getDataStorage();
                newLevel.save(null, false, false);
            } catch (Exception e) {
                Touhoulittlemaidpersonaldimension.LOGGER.warn("Failed to initialize dimension data storage for: {}", key.location(), e);
            }

            try {
                server.getClass().getMethod("markWorldsDirty").invoke(server);
            } catch (Exception ignored) {}

            Touhoulittlemaidpersonaldimension.LOGGER.info("Successfully created dimension: {}", key.location());
            return true;

        } catch (Exception e) {
            Touhoulittlemaidpersonaldimension.LOGGER.error("Failed to create dimension: {}", key.location(), e);
            return false;
        }
    }

    @Override
    public void tlmpersonal$removeWorld(ResourceKey<Level> key) {
        try {
            ServerLevel level = levels.remove(key);
            if (level != null) {
                Touhoulittlemaidpersonaldimension.LOGGER.info("Removed dimension: {}", key.location());
            }
        } catch (Exception e) {
            Touhoulittlemaidpersonaldimension.LOGGER.error("Failed to remove dimension: {}", key.location(), e);
        }
    }
}
