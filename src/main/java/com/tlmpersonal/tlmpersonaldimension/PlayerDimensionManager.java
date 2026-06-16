
package com.tlmpersonal.tlmpersonaldimension;

import com.tlmpersonal.tlmpersonaldimension.accessor.MinecraftServerAccessor;
import com.tlmpersonal.tlmpersonaldimension.Config.DimensionType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class PlayerDimensionManager {

    private static final Map<String, ServerLevel> playerDimensionCache = new ConcurrentHashMap<>();
    private static volatile boolean startupRestoreComplete = false;

    public static ResourceKey<Level> getPlayerPersonalDimensionKey(UUID playerUUID, DimensionType type) {
        String typeSuffix = type.name().toLowerCase();
        return ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(
                        Touhoulittlemaidpersonaldimension.MODID,
                        "personal_dimension_" + playerUUID.toString().replace("-", "_") + "_" + typeSuffix
                )
        );
    }

    public static ResourceLocation getTemplateDimensionKey(DimensionType type) {
        return switch (type) {
            case VOID -> Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_VOID_KEY.location();
            case NORMAL -> Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_NORMAL_KEY.location();
            case CHERRY -> Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_CHERRY_KEY.location();
        };
    }

    @Nullable
    public static ServerLevel getOrCreatePlayerDimensionSync(MinecraftServer server, UUID playerUUID) {
        if (!Config.PRIVATE_DIMENSION.get()) {
            return server.getLevel(Touhoulittlemaidpersonaldimension.getCurrentPersonalDimensionKey());
        }

        PersonalDimensionSavedData data = PersonalDimensionSavedData.get(server.overworld());
        PersonalDimensionSavedData.PlayerDimensionSettings settings = data.getOrCreateSettings(playerUUID);
        DimensionType type = settings.getDimensionType();
        if (type == null) type = Config.DIMENSION_TYPE.get();

        ResourceKey<Level> dimensionKey = getPlayerPersonalDimensionKey(playerUUID, type);
        ServerLevel cached = resolveLoadedDimension(server, dimensionKey, playerUUID, type);
        if (cached != null) {
            return cached;
        }

        boolean success = ((MinecraftServerAccessor) server).tlmpersonal$createWorld(
                dimensionKey,
                getTemplateDimensionKey(type)
        );

        if (success) {
            ServerLevel level = server.getLevel(dimensionKey);
            if (level != null) {
                String cacheKey = playerUUID.toString() + "_" + type.name();
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
        DimensionType type = settings.getDimensionType();
        if (type == null) type = Config.DIMENSION_TYPE.get();

        ResourceKey<Level> dimensionKey = getPlayerPersonalDimensionKey(playerUUID, type);
        return resolveLoadedDimension(server, dimensionKey, playerUUID, type);
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
                                                      DimensionType type) {
        String cacheKey = playerUUID.toString() + "_" + type.name();
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
    }
}
