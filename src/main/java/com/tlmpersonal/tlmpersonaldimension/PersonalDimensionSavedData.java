package com.tlmpersonal.tlmpersonaldimension;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PersonalDimensionSavedData extends SavedData {
    public static final String DATA_NAME = Touhoulittlemaidpersonaldimension.MODID + "_personal_dimension_data";

    public record Pair<T1, T2>(T1 first, T2 second) {}

    private static final String TAG_PLAYERS = "players";
    private static final String TAG_TELEPORTER_COOLDOWNS = "teleporter_cooldowns";
    private static final String TAG_GLOBAL_JOIN_COOLDOWNS = "global_join_cooldowns";
    private static final String TAG_GLOBAL_LEAVE_COOLDOWNS = "global_leave_cooldowns";
    private static final String TAG_PLAYER_GRIDS = "player_grids";
    private static final String TAG_PRIVATE_DIMENSIONS = "private_dimensions";

    private final Map<UUID, PlayerDimensionSettings> playerSettings = new HashMap<>();
    private final Map<UUID, Long> teleporterCooldowns = new HashMap<>();
    private final Map<UUID, Long> globalJoinCooldowns = new HashMap<>();
    private final Map<UUID, Long> globalLeaveCooldowns = new HashMap<>();
    private final Map<UUID, Pair<Integer, Integer>> playerGrids = new HashMap<>();
    private final Map<Pair<Integer, Integer>, UUID> gridToPlayer = new HashMap<>();
    private final Set<UUID> privateDimensions = new HashSet<>();
    // Track tamed maids: maid UUID -> (owner UUID, last level, last position)
    private final Map<UUID, MaidInfo> trackedMaids = new HashMap<>();
    
    public static class MaidInfo {
        public UUID ownerUuid;
        public ResourceKey<Level> lastLevel;
        public double lastX, lastY, lastZ;
        
        public MaidInfo(UUID ownerUuid, ResourceKey<Level> lastLevel, double lastX, double lastY, double lastZ) {
            this.ownerUuid = ownerUuid;
            this.lastLevel = lastLevel;
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastZ = lastZ;
        }
        
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("ownerUuid", ownerUuid);
            tag.putString("lastLevel", lastLevel.location().toString());
            tag.putDouble("lastX", lastX);
            tag.putDouble("lastY", lastY);
            tag.putDouble("lastZ", lastZ);
            return tag;
        }
        
        public static MaidInfo load(CompoundTag tag) {
            UUID ownerUuid = tag.getUUID("ownerUuid");
            ResourceKey<Level> lastLevel = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("lastLevel")));
            double lastX = tag.getDouble("lastX");
            double lastY = tag.getDouble("lastY");
            double lastZ = tag.getDouble("lastZ");
            return new MaidInfo(ownerUuid, lastLevel, lastX, lastY, lastZ);
        }
    }

    public Map<UUID, MaidInfo> getTrackedMaids() {
        return trackedMaids;
    }

    public static PersonalDimensionSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        PersonalDimensionSavedData data = overworld.getDataStorage().computeIfAbsent(
                new Factory<>(
                        PersonalDimensionSavedData::new,
                        (tag, registries) -> new PersonalDimensionSavedData(tag, registries)
                ),
                DATA_NAME
        );
        return data;
    }

    public PersonalDimensionSavedData() {}

    public PersonalDimensionSavedData(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag, registries);
    }

    private void load(CompoundTag tag, HolderLookup.Provider registries) {
        playerSettings.clear();
        teleporterCooldowns.clear();
        globalJoinCooldowns.clear();
        globalLeaveCooldowns.clear();
        playerGrids.clear();
        gridToPlayer.clear();
        privateDimensions.clear();
        trackedMaids.clear();

        if (tag.contains(TAG_PLAYERS)) {
            CompoundTag playersTag = tag.getCompound(TAG_PLAYERS);
            for (String uuidStr : playersTag.getAllKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                CompoundTag playerTag = playersTag.getCompound(uuidStr);
                PlayerDimensionSettings settings = PlayerDimensionSettings.load(playerTag);
                playerSettings.put(uuid, settings);
            }
        }

        if (tag.contains(TAG_PLAYER_GRIDS)) {
            CompoundTag gridsTag = tag.getCompound(TAG_PLAYER_GRIDS);
            for (String uuidStr : gridsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                CompoundTag gridTag = gridsTag.getCompound(uuidStr);
                int gridX = gridTag.getInt("x");
                int gridZ = gridTag.getInt("z");
                Pair<Integer, Integer> grid = new Pair<>(gridX, gridZ);
                playerGrids.put(uuid, grid);
                gridToPlayer.put(grid, uuid);
            }
        }

        if (tag.contains(TAG_TELEPORTER_COOLDOWNS)) {
            CompoundTag cooldownsTag = tag.getCompound(TAG_TELEPORTER_COOLDOWNS);
            for (String uuidStr : cooldownsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                teleporterCooldowns.put(uuid, cooldownsTag.getLong(uuidStr));
            }
        }

        if (tag.contains(TAG_GLOBAL_JOIN_COOLDOWNS)) {
            CompoundTag cooldownsTag = tag.getCompound(TAG_GLOBAL_JOIN_COOLDOWNS);
            for (String uuidStr : cooldownsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                globalJoinCooldowns.put(uuid, cooldownsTag.getLong(uuidStr));
            }
        }

        if (tag.contains(TAG_GLOBAL_LEAVE_COOLDOWNS)) {
            CompoundTag cooldownsTag = tag.getCompound(TAG_GLOBAL_LEAVE_COOLDOWNS);
            for (String uuidStr : cooldownsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                globalLeaveCooldowns.put(uuid, cooldownsTag.getLong(uuidStr));
            }
        }

        if (tag.contains(TAG_PRIVATE_DIMENSIONS)) {
            ListTag list = tag.getList(TAG_PRIVATE_DIMENSIONS, 8);
            for (int i = 0; i < list.size(); i++) {
                privateDimensions.add(UUID.fromString(list.getString(i)));
            }
        }
        
        if (tag.contains("trackedMaids")) {
            CompoundTag maidsTag = tag.getCompound("trackedMaids");
            for (String uuidStr : maidsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                MaidInfo info = MaidInfo.load(maidsTag.getCompound(uuidStr));
                trackedMaids.put(uuid, info);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, PlayerDimensionSettings> entry : playerSettings.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        tag.put(TAG_PLAYERS, playersTag);

        CompoundTag gridsTag = new CompoundTag();
        for (Map.Entry<UUID, Pair<Integer, Integer>> entry : playerGrids.entrySet()) {
            CompoundTag gridTag = new CompoundTag();
            gridTag.putInt("x", entry.getValue().first());
            gridTag.putInt("z", entry.getValue().second());
            gridsTag.put(entry.getKey().toString(), gridTag);
        }
        tag.put(TAG_PLAYER_GRIDS, gridsTag);

        CompoundTag cooldownsTag = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : teleporterCooldowns.entrySet()) {
            cooldownsTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put(TAG_TELEPORTER_COOLDOWNS, cooldownsTag);

        CompoundTag joinCooldownsTag = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : globalJoinCooldowns.entrySet()) {
            joinCooldownsTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put(TAG_GLOBAL_JOIN_COOLDOWNS, joinCooldownsTag);

        CompoundTag leaveCooldownsTag = new CompoundTag();
        for (Map.Entry<UUID, Long> entry : globalLeaveCooldowns.entrySet()) {
            leaveCooldownsTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put(TAG_GLOBAL_LEAVE_COOLDOWNS, leaveCooldownsTag);

        ListTag privateList = new ListTag();
        for (UUID uuid : privateDimensions) {
            privateList.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put(TAG_PRIVATE_DIMENSIONS, privateList);
        
        CompoundTag maidsTag = new CompoundTag();
        for (Map.Entry<UUID, MaidInfo> entry : trackedMaids.entrySet()) {
            maidsTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        tag.put("trackedMaids", maidsTag);

        return tag;
    }

    public PlayerDimensionSettings getOrCreateSettings(UUID playerUuid) {
        PlayerDimensionSettings settings = playerSettings.computeIfAbsent(playerUuid, k -> new PlayerDimensionSettings());
        if (settings.getAllowedPlayers().isEmpty()) {
            settings.getAllowedPlayers().add(playerUuid.toString());
            setDirty();
        }
        return settings;
    }

    public Pair<Integer, Integer> getOrCreatePlayerGrid(UUID playerUuid) {
        if (playerGrids.containsKey(playerUuid)) return playerGrids.get(playerUuid);
        long seed = playerUuid.getMostSignificantBits();
        Random rand = new Random(seed);
        int gridX = rand.nextInt(2000) - 1000;
        int gridZ = rand.nextInt(2000) - 1000;
        Pair<Integer, Integer> grid = new Pair<>(gridX, gridZ);
        playerGrids.put(playerUuid, grid);
        gridToPlayer.put(grid, playerUuid);
        setDirty();
        return grid;
    }

    public UUID getPlayerAtGrid(int gridX, int gridZ) {
        return gridToPlayer.get(new Pair<>(gridX, gridZ));
    }

    public Long getTeleporterCooldown(UUID playerUuid) {
        return teleporterCooldowns.get(playerUuid);
    }

    public void setTeleporterCooldown(UUID playerUuid, long time) {
        teleporterCooldowns.put(playerUuid, time);
        setDirty();
    }

    public Long getGlobalJoinCooldown(UUID playerUuid) {
        return globalJoinCooldowns.get(playerUuid);
    }

    public void setGlobalJoinCooldown(UUID playerUuid, long time) {
        globalJoinCooldowns.put(playerUuid, time);
        setDirty();
    }

    public Long getGlobalLeaveCooldown(UUID playerUuid) {
        return globalLeaveCooldowns.get(playerUuid);
    }

    public void setGlobalLeaveCooldown(UUID playerUuid, long time) {
        globalLeaveCooldowns.put(playerUuid, time);
        setDirty();
    }

    public void registerPrivateDimension(UUID playerUuid) {
        if (privateDimensions.add(playerUuid)) {
            setDirty();
        }
    }

    public Set<UUID> getPrivateDimensions() {
        return privateDimensions;
    }

    public static class PlayerDimensionSettings {
        private final Set<String> allowedEntities = new HashSet<>();
        private final Set<String> blockedEntities = new HashSet<>();
        private boolean allowAllEntities = false;
        private boolean disableHostileEntities = false;
        private final Set<String> allowedPlayers = new HashSet<>();

        private boolean disableHunger = false;
        private boolean disableMaidDeath = false;
        private boolean disablePlayerDeath = false;
        private boolean naturalHealing = false;
        private boolean blockHarmfulEffects = false;
        private boolean maidEmitLight = false;

        private boolean lockDay = false;
        private int lockedDayTime = 1000;
        private boolean lockWeather = false;
        private boolean lockedWeatherRain = false;
        private boolean lockedWeatherThunder = false;

        private boolean tamedMaidProtection = false;
        private long lastTamedMaidProtectionUse = 0L;
        private boolean entityCannotTarget = false;
        private boolean maidAuthority = false;
        private boolean maidAttackDiscard = false;
        private Config.DimensionType dimensionType = null;

        public PlayerDimensionSettings() {}

        public static PlayerDimensionSettings load(CompoundTag tag) {
            PlayerDimensionSettings settings = new PlayerDimensionSettings();
            if (tag.contains("allowedEntities")) {
                ListTag list = tag.getList("allowedEntities", 8);
                for (int i = 0; i < list.size(); i++) settings.allowedEntities.add(list.getString(i));
            }
            if (tag.contains("blockedEntities")) {
                ListTag list = tag.getList("blockedEntities", 8);
                for (int i = 0; i < list.size(); i++) settings.blockedEntities.add(list.getString(i));
            }
            settings.allowAllEntities = tag.getBoolean("allowAllEntities");
            settings.disableHostileEntities = tag.getBoolean("disableHostileEntities");
            if (tag.contains("allowedPlayers")) {
                ListTag list = tag.getList("allowedPlayers", 8);
                for (int i = 0; i < list.size(); i++) settings.allowedPlayers.add(list.getString(i));
            }
            settings.disableHunger = tag.getBoolean("disableHunger");
            settings.disableMaidDeath = tag.getBoolean("disableMaidDeath");
            settings.disablePlayerDeath = tag.getBoolean("disablePlayerDeath");
            settings.naturalHealing = tag.getBoolean("naturalHealing");
            settings.blockHarmfulEffects = tag.getBoolean("blockHarmfulEffects");
            settings.maidEmitLight = tag.getBoolean("maidEmitLight");
            settings.lockDay = tag.getBoolean("lockDay");
            settings.lockedDayTime = tag.getInt("lockedDayTime");
            settings.lockWeather = tag.getBoolean("lockWeather");
            settings.lockedWeatherRain = tag.getBoolean("lockedWeatherRain");
            settings.lockedWeatherThunder = tag.getBoolean("lockedWeatherThunder");
            if (tag.contains("tamedMaidProtection")) settings.tamedMaidProtection = tag.getBoolean("tamedMaidProtection");
            if (tag.contains("lastTamedMaidProtectionUse")) settings.lastTamedMaidProtectionUse = tag.getLong("lastTamedMaidProtectionUse");
            if (tag.contains("entityCannotTarget")) settings.entityCannotTarget = tag.getBoolean("entityCannotTarget");
            if (tag.contains("maidAuthority")) settings.maidAuthority = tag.getBoolean("maidAuthority");
            if (tag.contains("maidAttackDiscard")) settings.maidAttackDiscard = tag.getBoolean("maidAttackDiscard");
            if (tag.contains("dimensionType")) settings.dimensionType = Config.DimensionType.valueOf(tag.getString("dimensionType"));
            return settings;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            ListTag allowedList = new ListTag();
            for (String entity : allowedEntities) allowedList.add(StringTag.valueOf(entity));
            tag.put("allowedEntities", allowedList);
            ListTag blockedList = new ListTag();
            for (String entity : blockedEntities) blockedList.add(StringTag.valueOf(entity));
            tag.put("blockedEntities", blockedList);
            tag.putBoolean("allowAllEntities", allowAllEntities);
            tag.putBoolean("disableHostileEntities", disableHostileEntities);
            ListTag allowedPlayersList = new ListTag();
            for (String player : allowedPlayers) allowedPlayersList.add(StringTag.valueOf(player));
            tag.put("allowedPlayers", allowedPlayersList);
            tag.putBoolean("disableHunger", disableHunger);
            tag.putBoolean("disableMaidDeath", disableMaidDeath);
            tag.putBoolean("disablePlayerDeath", disablePlayerDeath);
            tag.putBoolean("naturalHealing", naturalHealing);
            tag.putBoolean("blockHarmfulEffects", blockHarmfulEffects);
            tag.putBoolean("maidEmitLight", maidEmitLight);
            tag.putBoolean("lockDay", lockDay);
            tag.putInt("lockedDayTime", lockedDayTime);
            tag.putBoolean("lockWeather", lockWeather);
            tag.putBoolean("lockedWeatherRain", lockedWeatherRain);
            tag.putBoolean("lockedWeatherThunder", lockedWeatherThunder);
            tag.putBoolean("tamedMaidProtection", tamedMaidProtection);
            tag.putLong("lastTamedMaidProtectionUse", lastTamedMaidProtectionUse);
            tag.putBoolean("entityCannotTarget", entityCannotTarget);
            tag.putBoolean("maidAuthority", maidAuthority);
            tag.putBoolean("maidAttackDiscard", maidAttackDiscard);
            if (dimensionType != null) tag.putString("dimensionType", dimensionType.name());
            return tag;
        }

        public Set<String> getAllowedEntities() { return allowedEntities; }
        public Set<String> getBlockedEntities() { return blockedEntities; }
        public boolean isAllowAllEntities() { return allowAllEntities; }
        public void setAllowAllEntities(boolean allowAllEntities) { this.allowAllEntities = allowAllEntities; }
        public boolean isDisableHostileEntities() { return disableHostileEntities; }
        public void setDisableHostileEntities(boolean disableHostileEntities) { this.disableHostileEntities = disableHostileEntities; }
        public Set<String> getAllowedPlayers() { return allowedPlayers; }
        public boolean isDisableHunger() { return disableHunger; }
        public void setDisableHunger(boolean disableHunger) { this.disableHunger = disableHunger; }
        public boolean isDisableMaidDeath() { return disableMaidDeath; }
        public void setDisableMaidDeath(boolean disableMaidDeath) { this.disableMaidDeath = disableMaidDeath; }
        public boolean isDisablePlayerDeath() { return disablePlayerDeath; }
        public void setDisablePlayerDeath(boolean disablePlayerDeath) { this.disablePlayerDeath = disablePlayerDeath; }
        public boolean isNaturalHealing() { return naturalHealing; }
        public void setNaturalHealing(boolean naturalHealing) { this.naturalHealing = naturalHealing; }
        public boolean isBlockHarmfulEffects() { return blockHarmfulEffects; }
        public void setBlockHarmfulEffects(boolean blockHarmfulEffects) { this.blockHarmfulEffects = blockHarmfulEffects; }
        public boolean isMaidEmitLight() { return maidEmitLight; }
        public void setMaidEmitLight(boolean maidEmitLight) { this.maidEmitLight = maidEmitLight; }
        public boolean isLockDay() { return lockDay; }
        public void setLockDay(boolean lockDay) { this.lockDay = lockDay; }
        public int getLockedDayTime() { return lockedDayTime; }
        public void setLockedDayTime(int lockedDayTime) { this.lockedDayTime = lockedDayTime; }
        public boolean isLockWeather() { return lockWeather; }
        public void setLockWeather(boolean lockWeather) { this.lockWeather = lockWeather; }
        public boolean isLockedWeatherRain() { return lockedWeatherRain; }
        public void setLockedWeatherRain(boolean lockedWeatherRain) { this.lockedWeatherRain = lockedWeatherRain; }
        public boolean isLockedWeatherThunder() { return lockedWeatherThunder; }
        public void setLockedWeatherThunder(boolean lockedWeatherThunder) { this.lockedWeatherThunder = lockedWeatherThunder; }
        public boolean isTamedMaidProtection() { return tamedMaidProtection; }
        public void setTamedMaidProtection(boolean tamedMaidProtection) { this.tamedMaidProtection = tamedMaidProtection; }
        public long getLastTamedMaidProtectionUse() { return lastTamedMaidProtectionUse; }
        public void setLastTamedMaidProtectionUse(long lastTamedMaidProtectionUse) { this.lastTamedMaidProtectionUse = lastTamedMaidProtectionUse; }
        public boolean isEntityCannotTarget() { return entityCannotTarget; }
        public void setEntityCannotTarget(boolean entityCannotTarget) { this.entityCannotTarget = entityCannotTarget; }
        public boolean isMaidAuthority() { return maidAuthority; }
        public void setMaidAuthority(boolean maidAuthority) { this.maidAuthority = maidAuthority; }
        public boolean isMaidAttackDiscard() { return maidAttackDiscard; }
        public void setMaidAttackDiscard(boolean maidAttackDiscard) { this.maidAttackDiscard = maidAttackDiscard; }
        public Config.DimensionType getDimensionType() { return dimensionType; }
        public void setDimensionType(Config.DimensionType dimensionType) { this.dimensionType = dimensionType; }
    }
}
