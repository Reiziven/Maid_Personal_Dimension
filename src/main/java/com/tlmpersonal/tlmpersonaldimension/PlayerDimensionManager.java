
package com.tlmpersonal.tlmpersonaldimension;

import com.tlmpersonal.tlmpersonaldimension.accessor.MinecraftServerAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDimensionManager {
    private static final Logger LOGGER = Touhoulittlemaidpersonaldimension.LOGGER;

    private static final Map<String, ServerLevel> playerDimensionCache = new ConcurrentHashMap<>();
    private static volatile boolean startupRestoreComplete = false;
    private static List<CustomDimensionConfig> loadedDimensions = null;

    private static List<CustomDimensionConfig> getLoadedDimensions() {
        if (loadedDimensions == null) {
            loadedDimensions = CustomDimensionConfig.loadFromConfig();
        }
        return loadedDimensions;
    }

    public static ResourceKey<Level> getPlayerPersonalDimensionKey(UUID playerUUID, String dimensionTypeId) {
        // Sanitize: ResourceLocation paths must be [a-z0-9/._-] only
        String safeSuffix = dimensionTypeId.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        return ResourceKey.create(
                Registries.DIMENSION,
                new ResourceLocation(
                        Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_" + playerUUID.toString().replace("-", "_") + "_" + safeSuffix
                )
        );
    }

    public static ResourceLocation getTemplateDimensionKey(String dimensionTypeId) {
        CustomDimensionConfig config = CustomDimensionConfig.findById(dimensionTypeId, getLoadedDimensions());
        if (config != null) {
            return config.getTemplateDimensionKey();
        }
        // Fallback to first dimension if ID not found
        CustomDimensionConfig fallback = CustomDimensionConfig.getByIndex(0, getLoadedDimensions());
        if (fallback != null) {
            LOGGER.warn("Dimension type ID '{}' not found, falling back to '{}'", dimensionTypeId, fallback.getId());
            return fallback.getTemplateDimensionKey();
        }
        // Ultimate fallback
        return Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_VOID_KEY.location();
    }

    @Nullable
    public static ServerLevel getOrCreatePlayerDimensionSync(MinecraftServer server, UUID playerUUID) {
        if (!Config.PRIVATE_DIMENSION.get()) {
            return server.getLevel(Touhoulittlemaidpersonaldimension.getCurrentPersonalDimensionKey());
        }

        PersonalDimensionSavedData data = PersonalDimensionSavedData.get(server.overworld());
        PersonalDimensionSavedData.PlayerDimensionSettings settings = data.getOrCreateSettings(playerUUID);
        String dimensionTypeId = settings.getDimensionTypeId();
        
        // If no dimension type set, use configured default
        if (dimensionTypeId == null || dimensionTypeId.isEmpty()) {
            String defaultId = Config.DEFAULT_DIMENSION_TYPE_ID.get();
            CustomDimensionConfig defaultDim = CustomDimensionConfig.findById(defaultId, getLoadedDimensions());
            if (defaultDim == null) defaultDim = CustomDimensionConfig.getByIndex(0, getLoadedDimensions());
            dimensionTypeId = defaultDim != null ? defaultDim.getId() : "void";
            settings.setDimensionTypeId(dimensionTypeId);
            data.setDirty();
        }

        ResourceKey<Level> dimensionKey = getPlayerPersonalDimensionKey(playerUUID, dimensionTypeId);
        ServerLevel cached = resolveLoadedDimension(server, dimensionKey, playerUUID, dimensionTypeId);
        if (cached != null) {
            return cached;
        }

        boolean success = ((MinecraftServerAccessor) server).tlmpersonal$createWorld(
                dimensionKey,
                getTemplateDimensionKey(dimensionTypeId)
        );

        if (success) {
            ServerLevel level = server.getLevel(dimensionKey);
            if (level != null) {
                String cacheKey = playerUUID.toString() + "_" + dimensionTypeId;
                playerDimensionCache.put(cacheKey, level);
                registerPlayerDimension(server, playerUUID);
                return level;
            }
        }
        return null;
    }

    @Nullable
    public static ServerLevel getExistingPlayerDimension(MinecraftServer server, UUID playerUUID) {
        if (!Config.PRIVATE_DIMENSION.get()) {
            return server.getLevel(Touhoulittlemaidpersonaldimension.getCurrentPersonalDimensionKey());
        }

        PersonalDimensionSavedData data = PersonalDimensionSavedData.get(server.overworld());
        PersonalDimensionSavedData.PlayerDimensionSettings settings = data.getOrCreateSettings(playerUUID);
        String dimensionTypeId = settings.getDimensionTypeId();
        
        if (dimensionTypeId == null || dimensionTypeId.isEmpty()) {
            String defaultId = Config.DEFAULT_DIMENSION_TYPE_ID.get();
            CustomDimensionConfig defaultDim = CustomDimensionConfig.findById(defaultId, getLoadedDimensions());
            if (defaultDim == null) defaultDim = CustomDimensionConfig.getByIndex(0, getLoadedDimensions());
            dimensionTypeId = defaultDim != null ? defaultDim.getId() : "void";
        }

        ResourceKey<Level> dimensionKey = getPlayerPersonalDimensionKey(playerUUID, dimensionTypeId);
        return resolveLoadedDimension(server, dimensionKey, playerUUID, dimensionTypeId);
    }

    public static synchronized void preloadPersistedPersonalDimensionState(MinecraftServer server) {
        if (startupRestoreComplete) return;
        startupRestoreComplete = true;
        Touhoulittlemaidpersonaldimension.LOGGER.info("Personal dimension state ready (lazy loading enabled).");
        if (Config.PRIVATE_DIMENSION.get()) {
            PersonalDimensionSavedData data = PersonalDimensionSavedData.get(server.overworld());
            for (UUID playerUUID : data.getPrivateDimensions()) {
                getOrCreatePlayerDimensionSync(server, playerUUID);
            }
        }
    }

    @Nullable
    private static ServerLevel resolveLoadedDimension(MinecraftServer server,
                                                      ResourceKey<Level> dimensionKey,
                                                      UUID playerUUID,
                                                      String dimensionTypeId) {
        String cacheKey = playerUUID.toString() + "_" + dimensionTypeId;
        ServerLevel cached = playerDimensionCache.get(cacheKey);
        if (cached != null) {
            if (cached.getServer() == server
                    && cached.dimension().equals(dimensionKey)
                    && server.getLevel(dimensionKey) == cached) {
                return cached;
            } else {
                playerDimensionCache.remove(cacheKey);
            }
        }

        ServerLevel existingLevel = server.getLevel(dimensionKey);
        if (existingLevel != null) {
            playerDimensionCache.put(cacheKey, existingLevel);
            return existingLevel;
        }
        return null;
    }

    private static void registerPlayerDimension(MinecraftServer server, UUID playerUUID) {
        PersonalDimensionSavedData.get(server.overworld()).registerPrivateDimension(playerUUID);
    }

    public static void clearCache() {
        playerDimensionCache.clear();
        startupRestoreComplete = false;
        loadedDimensions = null;  // Force reload on next access
    }
}
