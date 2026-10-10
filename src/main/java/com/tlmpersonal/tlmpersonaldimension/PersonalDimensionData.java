
package com.tlmpersonal.tlmpersonaldimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PersonalDimensionData extends SavedData {
    private static final String DATA_NAME = Touhoulittlemaidpersonaldimension.MODID + "_personal_dimensions";
    private static final long ACCESS_TIME_UPDATE_INTERVAL_MS = 30_000L;

    private final Map<UUID, DimensionInfo> playerDimensions = new HashMap<>();

    public PersonalDimensionData() {
        super();
    }

    public PersonalDimensionData(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag, registries);
    }

    public static class DimensionInfo {
        public final UUID playerUUID;
        public long createdTime;
        public long lastAccessTime;

        public boolean structureGenerated;
        @Nullable
        public String pendingRestoreDimension;
        @Nullable
        public BlockPos pendingRestorePos;

        public DimensionInfo(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = this.createdTime;
            this.structureGenerated = false;
            this.pendingRestoreDimension = null;
            this.pendingRestorePos = null;
        }

        public DimensionInfo(UUID playerUUID, long createdTime, long lastAccessTime,
                             boolean structureGenerated,
                             @Nullable String pendingRestoreDimension, @Nullable BlockPos pendingRestorePos) {
            this.playerUUID = playerUUID;
            this.createdTime = createdTime;
            this.lastAccessTime = lastAccessTime;
            this.structureGenerated = structureGenerated;
            this.pendingRestoreDimension = pendingRestoreDimension;
            this.pendingRestorePos = pendingRestorePos;
        }

        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("PlayerUUID", playerUUID);
            tag.putLong("CreatedTime", createdTime);
            tag.putLong("LastAccessTime", lastAccessTime);
            tag.putBoolean("StructureGenerated", structureGenerated);
            if (pendingRestoreDimension != null && pendingRestorePos != null) {
                tag.putString("PendingRestoreDimension", pendingRestoreDimension);
                tag.putInt("PendingRestoreX", pendingRestorePos.getX());
                tag.putInt("PendingRestoreY", pendingRestorePos.getY());
                tag.putInt("PendingRestoreZ", pendingRestorePos.getZ());
            }
            return tag;
        }

        public static DimensionInfo load(CompoundTag tag) {
            UUID playerUUID = tag.getUUID("PlayerUUID");
            long createdTime = tag.getLong("CreatedTime");
            long lastAccessTime = tag.getLong("LastAccessTime");
            boolean structureGenerated = tag.getBoolean("StructureGenerated");
            String pendingRestoreDimension = tag.contains("PendingRestoreDimension", Tag.TAG_STRING)
                    ? tag.getString("PendingRestoreDimension")
                    : null;
            BlockPos pendingRestorePos = null;
            if (pendingRestoreDimension != null
                    && tag.contains("PendingRestoreX", Tag.TAG_INT)
                    && tag.contains("PendingRestoreY", Tag.TAG_INT)
                    && tag.contains("PendingRestoreZ", Tag.TAG_INT)) {
                pendingRestorePos = new BlockPos(
                        tag.getInt("PendingRestoreX"),
                        tag.getInt("PendingRestoreY"),
                        tag.getInt("PendingRestoreZ")
                );
            }

            return new DimensionInfo(
                    playerUUID,
                    createdTime,
                    lastAccessTime,
                    structureGenerated,
                    pendingRestoreDimension,
                    pendingRestorePos
            );
        }

        @Nullable
        public ResourceKey<Level> getPendingRestoreDimensionKey() {
            if (pendingRestoreDimension == null || pendingRestoreDimension.isBlank()) {
                return null;
            }

            ResourceLocation location = ResourceLocation.tryParse(pendingRestoreDimension);
            if (location == null) {
                return null;
            }

            ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
            return dimensionKey;
        }
    }

    public static PersonalDimensionData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(
                        PersonalDimensionData::new,
                        PersonalDimensionData::new
                ),
                DATA_NAME
        );
    }

    private void load(CompoundTag tag, HolderLookup.Provider registries) {
        playerDimensions.clear();

        if (tag.contains("PlayerDimensions", Tag.TAG_LIST)) {
            ListTag list = tag.getList("PlayerDimensions", Tag.TAG_COMPOUND);
            for (Tag element : list) {
                CompoundTag dimensionTag = (CompoundTag) element;
                DimensionInfo info = DimensionInfo.load(dimensionTag);
                playerDimensions.put(info.playerUUID, info);
            }
        }

        Touhoulittlemaidpersonaldimension.LOGGER.info("Loaded {} personal dimension records", playerDimensions.size());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (DimensionInfo info : playerDimensions.values()) {
            list.add(info.save());
        }

        tag.put("PlayerDimensions", list);

        Touhoulittlemaidpersonaldimension.LOGGER.debug("Saved {} personal dimension records", playerDimensions.size());
        return tag;
    }

    public void registerDimension(UUID playerUUID) {
        if (!playerDimensions.containsKey(playerUUID)) {
            playerDimensions.put(playerUUID, new DimensionInfo(playerUUID));
            setDirty();
            Touhoulittlemaidpersonaldimension.LOGGER.info("Registered new personal dimension for player: " + playerUUID);
        } else {
            updateAccessTime(playerUUID);
        }
    }

    public void updateAccessTime(UUID playerUUID) {
        DimensionInfo info = playerDimensions.get(playerUUID);
        if (info != null) {
            long now = System.currentTimeMillis();
            if ((now - info.lastAccessTime) >= ACCESS_TIME_UPDATE_INTERVAL_MS) {
                info.lastAccessTime = now;
                setDirty();
            }
        }
    }

    public boolean hasDimension(UUID playerUUID) {
        return playerDimensions.containsKey(playerUUID);
    }

    public DimensionInfo getDimensionInfo(UUID playerUUID) {
        return playerDimensions.get(playerUUID);
    }

    public void removeDimension(UUID playerUUID) {
        if (playerDimensions.remove(playerUUID) != null) {
            setDirty();
            Touhoulittlemaidpersonaldimension.LOGGER.info("Removed personal dimension record for player: " + playerUUID);
        }
    }

    public Map<UUID, DimensionInfo> getAllDimensions() {
        return new HashMap<>(playerDimensions);
    }

    public void cleanupOldDimensions(long maxInactiveTime) {
        long currentTime = System.currentTimeMillis();
        playerDimensions.entrySet().removeIf(entry -> {
            DimensionInfo info = entry.getValue();
            boolean shouldRemove = (currentTime - info.lastAccessTime) > maxInactiveTime;
            if (shouldRemove) {
                Touhoulittlemaidpersonaldimension.LOGGER.info("Cleaned up inactive personal dimension for player: " + entry.getKey());
            }
            return shouldRemove;
        });

        if (!playerDimensions.isEmpty()) {
            setDirty();
        }
    }

    public void markStructureGenerated(UUID playerUUID) {
        DimensionInfo info = playerDimensions.get(playerUUID);
        if (info != null && !info.structureGenerated) {
            info.structureGenerated = true;
            setDirty();
            Touhoulittlemaidpersonaldimension.LOGGER.info("Persisted structure generated flag for player: {}", playerUUID);
        }
    }

    public void setPendingRestore(UUID playerUUID, ResourceKey<Level> dimensionKey, BlockPos pos) {
        DimensionInfo info = playerDimensions.computeIfAbsent(playerUUID, DimensionInfo::new);
        info.pendingRestoreDimension = dimensionKey.location().toString();
        info.pendingRestorePos = pos.immutable();
        setDirty();
        Touhoulittlemaidpersonaldimension.LOGGER.info("Saved pending personal restore for player {}: {} @ {}", playerUUID, info.pendingRestoreDimension, pos);
    }

    public void clearPendingRestore(UUID playerUUID) {
        DimensionInfo info = playerDimensions.get(playerUUID);
        if (info != null && (info.pendingRestoreDimension != null || info.pendingRestorePos != null)) {
            info.pendingRestoreDimension = null;
            info.pendingRestorePos = null;
            setDirty();
            Touhoulittlemaidpersonaldimension.LOGGER.debug("Cleared pending personal restore for player {}", playerUUID);
        }
    }
}
