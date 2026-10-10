package com.tlmpersonal.tlmpersonaldimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class PersonalLevelStateData extends SavedData {
    private static final String DATA_NAME = Touhoulittlemaidpersonaldimension.MODID + "_personal_level_state";
    private long gameTime;
    private long dayTime;
    private int clearWeatherTime;
    private boolean isRaining;
    private int rainTime;
    private boolean isThundering;
    private int thunderTime;
    private boolean initialized;
    private transient PersonalLevelData attachedLevelData;

    public PersonalLevelStateData() {}

    private static PersonalLevelStateData load(CompoundTag tag) {
        PersonalLevelStateData data = new PersonalLevelStateData();
        data.initialized = tag.getBoolean("Initialized");
        data.gameTime = tag.getLong("GameTime");
        data.dayTime = tag.getLong("DayTime");
        data.clearWeatherTime = tag.getInt("ClearWeatherTime");
        data.isRaining = tag.getBoolean("IsRaining");
        data.rainTime = tag.getInt("RainTime");
        data.isThundering = tag.getBoolean("IsThundering");
        data.thunderTime = tag.getInt("ThunderTime");
        return data;
    }

    public static PersonalLevelStateData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                PersonalLevelStateData::load,
                PersonalLevelStateData::new,
                DATA_NAME);
    }

    public void attach(PersonalLevelData levelData) {
        this.attachedLevelData = levelData;
        levelData.setDirtyListener(this::setDirty);
        if (!this.initialized) {
            this.capture(levelData);
            this.initialized = true;
            this.setDirty();
            return;
        }
        this.applyTo(levelData);
    }

    public void capture(PersonalLevelData levelData) {
        this.gameTime = levelData.getGameTime();
        this.dayTime = levelData.getDayTime();
        this.clearWeatherTime = levelData.getClearWeatherTime();
        this.isRaining = levelData.isRaining();
        this.rainTime = levelData.getRainTime();
        this.isThundering = levelData.isThundering();
        this.thunderTime = levelData.getThunderTime();
    }

    public void applyTo(PersonalLevelData levelData) {
        levelData.setDirtyListener(null);
        levelData.setGameTime(this.gameTime);
        levelData.setDayTime(this.dayTime);
        levelData.setClearWeatherTime(this.clearWeatherTime);
        levelData.setRaining(this.isRaining);
        levelData.setRainTime(this.rainTime);
        levelData.setThundering(this.isThundering);
        levelData.setThunderTime(this.thunderTime);
        levelData.setDirtyListener(this::setDirty);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (this.attachedLevelData != null) this.capture(this.attachedLevelData);
        tag.putBoolean("Initialized", true);
        tag.putLong("GameTime", this.gameTime);
        tag.putLong("DayTime", this.dayTime);
        tag.putInt("ClearWeatherTime", this.clearWeatherTime);
        tag.putBoolean("IsRaining", this.isRaining);
        tag.putInt("RainTime", this.rainTime);
        tag.putBoolean("IsThundering", this.isThundering);
        tag.putInt("ThunderTime", this.thunderTime);
        return tag;
    }
}
