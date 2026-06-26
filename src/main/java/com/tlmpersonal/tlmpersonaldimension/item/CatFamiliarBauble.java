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

    // Tracks maid UUIDs whose cat died (cooldown applies only after death)
    private static final Map<UUID, Long> catDeathTime = new HashMap<>();

    @Override
    public void onTick(EntityMaid maid, ItemStack baubleItem) {
        if (maid.level().isClientSide)
            return;

        ServerLevel serverLevel = (ServerLevel) maid.level();
        CatFamiliarEntity existingCat = findCatForMaid(serverLevel, maid);

        if (existingCat != null && !existingCat.isAlive()) {
            // Cat just died — record death time, clean up, start cooldown
            catDeathTime.put(maid.getUUID(), serverLevel.getGameTime());
            clearStoredCatUUID(maid);
            existingCat.discard();
            return;
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
            serverLevel.addFreshEntity(cat);
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
}
