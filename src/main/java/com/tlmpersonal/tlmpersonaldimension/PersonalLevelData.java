
package com.tlmpersonal.tlmpersonaldimension;

import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;

public class PersonalLevelData extends DerivedLevelData {
    private long gameTime;
    private long dayTime;
    private final TimerQueue<net.minecraft.server.MinecraftServer> scheduledEvents = new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS);
    private int clearWeatherTime;
    private boolean isRaining;
    private int rainTime;
    private boolean isThundering;
    private int thunderTime;
    private Runnable dirtyListener = () -> {};

    public PersonalLevelData(WorldData worldData, ServerLevelData wrapped) {
        super(worldData, wrapped);
        this.gameTime = wrapped.getGameTime();
        this.dayTime = wrapped.getDayTime();
        this.clearWeatherTime = wrapped.getClearWeatherTime();
        this.isRaining = wrapped.isRaining();
        this.rainTime = wrapped.getRainTime();
        this.isThundering = wrapped.isThundering();
        this.thunderTime = wrapped.getThunderTime();
    }

    @Override
    public long getGameTime() {
        return this.gameTime;
    }

    @Override
    public void setGameTime(long gameTime) {
        this.gameTime = gameTime;
        this.markDirty();
    }

    @Override
    public long getDayTime() {
        return this.dayTime;
    }

    @Override
    public void setDayTime(long dayTime) {
        this.dayTime = dayTime;
        this.markDirty();
    }

    @Override
    public TimerQueue<net.minecraft.server.MinecraftServer> getScheduledEvents() {
        return this.scheduledEvents;
    }

    @Override
    public int getClearWeatherTime() {
        return this.clearWeatherTime;
    }

    @Override
    public void setClearWeatherTime(int time) {
        this.clearWeatherTime = time;
        this.markDirty();
    }

    @Override
    public boolean isRaining() {
        return this.isRaining;
    }

    @Override
    public void setRaining(boolean raining) {
        this.isRaining = raining;
        this.markDirty();
    }

    @Override
    public int getRainTime() {
        return this.rainTime;
    }

    @Override
    public void setRainTime(int time) {
        this.rainTime = time;
        this.markDirty();
    }

    @Override
    public boolean isThundering() {
        return this.isThundering;
    }

    @Override
    public void setThundering(boolean thundering) {
        this.isThundering = thundering;
        this.markDirty();
    }

    @Override
    public int getThunderTime() {
        return this.thunderTime;
    }

    @Override
    public void setThunderTime(int time) {
        this.thunderTime = time;
        this.markDirty();
    }

    public void setDirtyListener(Runnable dirtyListener) {
        this.dirtyListener = dirtyListener != null ? dirtyListener : () -> {};
    }

    private void markDirty() {
        this.dirtyListener.run();
    }
}
