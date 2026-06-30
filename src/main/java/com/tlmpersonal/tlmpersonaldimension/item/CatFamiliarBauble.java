package com.tlmpersonal.tlmpersonaldimension.item;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CatFamiliarBauble implements IMaidBauble {

    private static final String CAT_UUID_KEY = "tlmpd_cat_familiar_uuid";
    private static final String CAT_LAST_INTERACTION_KEY = "tlmpd_cat_last_interaction";
    private static final String CAT_NEXT_INTERACTION_KEY = "tlmpd_cat_next_interaction";
    private static final String CAT_HEALTH_KEY = "tlmpd_cat_health";

    // Tracks maid UUIDs whose cat died (cooldown applies only after death)
    private static final Map<UUID, Long> catDeathTime = new HashMap<>();

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        if (maid.level().isClientSide)
            return;

        ServerLevel serverLevel = (ServerLevel) maid.level();
        CatFamiliarEntity existingCat = findCatForMaid(serverLevel, maid);

        if (existingCat != null && !existingCat.isAlive()) {
            // Cat just died — record death time, persist interaction cooldown, clear saved health
            catDeathTime.put(maid.getUUID(), serverLevel.getGameTime());
            saveCatInteractionTime(maid, existingCat.lastInteractionTime);
            saveCatNextInteractionTime(maid, existingCat.nextInteractionTime);
            maid.getPersistentData().remove(CAT_HEALTH_KEY);
            clearStoredCatUUID(maid);
            existingCat.discard();
            return;
        }

        if (existingCat != null) {
            // Cat alive — keep interaction time and health synced to maid NBT so they survive pick-up
            saveCatInteractionTime(maid, existingCat.lastInteractionTime);
            saveCatNextInteractionTime(maid, existingCat.nextInteractionTime);
            maid.getPersistentData().putFloat(CAT_HEALTH_KEY, existingCat.getHealth());
        }

        if (existingCat == null) {
            UUID maidId = maid.getUUID();
            long currentTime = serverLevel.getGameTime();

            // Only apply cooldown if the cat previously died
            if (catDeathTime.containsKey(maidId)) {
                long timeSinceDeath = currentTime - catDeathTime.get(maidId);
                int cooldownTicks = com.tlmpersonal.tlmpersonaldimension.Config.CAT_FAMILIAR_REVIVAL_COOLDOWN.get() * 20;
                if (timeSinceDeath < cooldownTicks) {
                    return;
                }
                catDeathTime.remove(maidId);
            }

            spawnCatForMaid(serverLevel, maid);
        }
    }

    private CatFamiliarEntity findCatForMaid(ServerLevel serverLevel, EntityMaid maid) {
        UUID storedUUID = getStoredCatUUID(maid);
        if (storedUUID != null) {
            Entity entity = serverLevel.getEntity(storedUUID);
            if (entity instanceof CatFamiliarEntity cat) {
                return cat; // return regardless of alive state — let onTick decide
            }
            // Entity gone entirely (unloaded/removed) — clear stale UUID
            clearStoredCatUUID(maid);
        }
        return null;
    }

    private void spawnCatForMaid(ServerLevel serverLevel, EntityMaid maid) {
        CatFamiliarEntity cat = Touhoulittlemaidpersonaldimension.CAT_FAMILIAR_ENTITY.get()
                .create(serverLevel);
        if (cat != null) {
            cat.setPos(maid.getX(), maid.getY(), maid.getZ());
            net.minecraft.core.Holder<net.minecraft.world.entity.animal.CatVariant> allBlackVariant =
                serverLevel.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CAT_VARIANT)
                    .getHolderOrThrow(net.minecraft.world.entity.animal.CatVariant.ALL_BLACK);
            cat.setVariant(allBlackVariant);
            cat.setPersistenceRequired();
            cat.setMaidId(maid.getUUID());
            cat.lastInteractionTime = loadCatInteractionTime(maid);
            cat.nextInteractionTime = loadCatNextInteractionTime(maid);
            // Pre-sync max health so the saved health restore below uses the correct cap,
            // not the default 20. Mirrors the logic in syncAttributesWithMaid.
            if (com.tlmpersonal.tlmpersonaldimension.Config.CAT_FAMILIAR_MIRROR_HEALTH.get()) {
                double maidMaxHealth = maid.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                cat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(maidMaxHealth);
            }
            CompoundTag pd = maid.getPersistentData();
            if (pd.contains(CAT_HEALTH_KEY)) {
                float savedHealth = pd.getFloat(CAT_HEALTH_KEY);
                cat.setHealth(Math.min(savedHealth, cat.getMaxHealth()));
            }
            serverLevel.addFreshEntity(cat);
            // Spawn particles at spawn location
            com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity.spawnWitchParticles(cat.position(), serverLevel);
            storeStoredCatUUID(maid, cat.getUUID());
        }
    }

    private UUID getStoredCatUUID(EntityMaid maid) {
        CompoundTag tag = maid.getPersistentData();
        if (tag.hasUUID(CAT_UUID_KEY)) {
            return tag.getUUID(CAT_UUID_KEY);
        }
        return null;
    }

    private void storeStoredCatUUID(EntityMaid maid, UUID catUUID) {
        maid.getPersistentData().putUUID(CAT_UUID_KEY, catUUID);
    }

    private void clearStoredCatUUID(EntityMaid maid) {
        maid.getPersistentData().remove(CAT_UUID_KEY);
    }

    private void saveCatInteractionTime(EntityMaid maid, long time) {
        maid.getPersistentData().putLong(CAT_LAST_INTERACTION_KEY, time);
    }

    private long loadCatInteractionTime(EntityMaid maid) {
        return maid.getPersistentData().getLong(CAT_LAST_INTERACTION_KEY);
    }

    private void saveCatNextInteractionTime(EntityMaid maid, long time) {
        maid.getPersistentData().putLong(CAT_NEXT_INTERACTION_KEY, time);
    }

    private long loadCatNextInteractionTime(EntityMaid maid) {
        return maid.getPersistentData().getLong(CAT_NEXT_INTERACTION_KEY);
    }
}
