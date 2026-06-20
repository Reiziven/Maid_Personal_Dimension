package com.tlmpersonal.tlmpersonaldimension.entity;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import java.util.*;

public class CherryDomainEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData
            .defineId(CherryDomainEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> MAID_ID = SynchedEntityData
            .defineId(CherryDomainEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> TICK_COUNT_REMAINING = SynchedEntityData
            .defineId(CherryDomainEntity.class, EntityDataSerializers.INT);

    private final Map<BlockPos, BlockState> savedBlocks = new HashMap<>();
    private final Map<BlockPos, CompoundTag> savedBlockEntities = new HashMap<>();
    private final int RADIUS = 5;

    public CherryDomainEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public void setOwnerId(UUID ownerId) {
        this.entityData.set(OWNER_ID, Optional.of(ownerId));
    }

    public UUID getOwnerId() {
        return this.entityData.get(OWNER_ID).orElse(null);
    }

    public void setMaidId(UUID maidId) {
        this.entityData.set(MAID_ID, Optional.of(maidId));
    }

    public UUID getMaidId() {
        return this.entityData.get(MAID_ID).orElse(null);
    }

    public void resetTimeout() {
        this.entityData.set(TICK_COUNT_REMAINING, 5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, Optional.empty());
        builder.define(MAID_ID, Optional.empty());
        builder.define(TICK_COUNT_REMAINING, 5);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("OwnerId"))
            setOwnerId(compound.getUUID("OwnerId"));
        if (compound.hasUUID("MaidId"))
            setMaidId(compound.getUUID("MaidId"));
        this.entityData.set(TICK_COUNT_REMAINING, compound.getInt("RemainingTicks"));

        if (compound.contains("SavedBlocks")) {
            ListTag blockList = compound.getList("SavedBlocks", 10);
            for (int i = 0; i < blockList.size(); i++) {
                CompoundTag tag = blockList.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(tag, "pos").orElse(BlockPos.ZERO);
                BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),
                        tag.getCompound("state"));
                savedBlocks.put(pos, state);
                if (tag.contains("entity")) {
                    savedBlockEntities.put(pos, tag.getCompound("entity"));
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (getOwnerId() != null)
            compound.putUUID("OwnerId", getOwnerId());
        if (getMaidId() != null)
            compound.putUUID("MaidId", getMaidId());
        compound.putInt("RemainingTicks", this.entityData.get(TICK_COUNT_REMAINING));

        ListTag blockList = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : savedBlocks.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.put("pos", NbtUtils.writeBlockPos(entry.getKey()));
            tag.put("state", NbtUtils.writeBlockState(entry.getValue()));
            if (savedBlockEntities.containsKey(entry.getKey())) {
                tag.put("entity", savedBlockEntities.get(entry.getKey()));
            }
            blockList.add(tag);
        }
        compound.put("SavedBlocks", blockList);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide)
            return;

        ServerLevel serverLevel = (ServerLevel) this.level();

        int remaining = this.entityData.get(TICK_COUNT_REMAINING);
        if (remaining <= 0) {
            restoreDomain();
            this.discard();
            return;
        }
        this.entityData.set(TICK_COUNT_REMAINING, remaining - 1);

        EntityMaid maid = null;
        UUID maidId = getMaidId();
        if (maidId != null) {
            Entity e = serverLevel.getEntity(maidId);
            if (e instanceof EntityMaid m)
                maid = m;
        }

        Player owner = null;
        UUID ownerId = getOwnerId();
        if (ownerId != null) {
            owner = serverLevel.getPlayerByUUID(ownerId);
        }

        if (maid != null) {
            this.setPos(maid.getX(), maid.getY(), maid.getZ());
        }

        if (remaining % 20 == 0 && isUsingEntityProtection()) {
            applyEffects();
        }

        updateBlocks(serverLevel, maid, owner);

    }

    private void updateBlocks(ServerLevel level, EntityMaid maid, Player owner) {
        Set<BlockPos> currentlyAffected = new HashSet<>();

        addBlocksInRadius(maid, currentlyAffected, level);
        if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == level) {
            addBlocksInRadius(owner, currentlyAffected, level);
        }

        Set<BlockPos> finalAffected = new HashSet<>(currentlyAffected);
        for (BlockPos p : currentlyAffected) {
            finalAffected.add(p.above());
        }

        List<BlockPos> toRestore = new ArrayList<>();
        for (BlockPos pos : savedBlocks.keySet()) {
            if (!finalAffected.contains(pos)) {
                toRestore.add(pos);
            }
        }

        toRestore.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        for (BlockPos pos : toRestore) {
            level.setBlockAndUpdate(pos, savedBlocks.get(pos));
            if (savedBlockEntities.containsKey(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    be.loadWithComponents(savedBlockEntities.get(pos), level.registryAccess());
                }
                savedBlockEntities.remove(pos);
            }
            savedBlocks.remove(pos);
        }

        for (BlockPos pos : currentlyAffected) {
            if (!savedBlocks.containsKey(pos)) {
                BlockState oldState = level.getBlockState(pos);
                if (oldState.blocksMotion()) {
                    // Save above block first so it doesn't drop when the supporting block is
                    // replaced
                    BlockPos above = pos.above();
                    BlockState aboveState = level.getBlockState(above);
                    if (!aboveState.getFluidState().isEmpty()) {
                        // Skip replacement under liquids — don't place petals in water
                    } else {
                        // Remove dependent block above without drops (flag 3|32=35 suppresses drops)
                        if (!aboveState.isAir() && !savedBlocks.containsKey(above)) {
                            savedBlocks.put(above, aboveState);
                            level.setBlock(above, Blocks.AIR.defaultBlockState(), 35);
                        }
                        savedBlocks.put(pos, oldState);
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) {
                            savedBlockEntities.put(pos, be.saveWithFullMetadata(level.registryAccess()));
                        }
                        // Replace the solid block without drops
                        level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 35);

                        // Place pink petals above only if the space is clear
                        if (aboveState.isAir() || aboveState.canBeReplaced()) {
                            if (!savedBlocks.containsKey(above)) {
                                savedBlocks.put(above, aboveState);
                            }
                            level.setBlock(above, Blocks.PINK_PETALS.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    private void addBlocksInRadius(Entity entity, Set<BlockPos> set, ServerLevel level) {
        if (entity == null) {
            return;
        }

        int r = 2;
        BlockPos center = entity.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {

                BlockPos floorPos = center.offset(x, -1, z);

                BlockState floorState = level.getBlockState(floorPos);
                BlockState aboveState = level.getBlockState(floorPos.above());

                if (floorState.blocksMotion()
                        && !floorState.is(Blocks.PINK_PETALS)
                        && (aboveState.isAir() || aboveState.canBeReplaced())) {
                    set.add(floorPos);
                }
            }
        }
    }

    private void restoreDomain() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        List<Map.Entry<BlockPos, BlockState>> sortedEntries = new ArrayList<>(savedBlocks.entrySet());
        sortedEntries.sort(Comparator.comparingInt(e -> e.getKey().getY()));
        for (Map.Entry<BlockPos, BlockState> entry : sortedEntries) {
            BlockPos pos = entry.getKey();
            BlockPos above = pos.above();
            if (serverLevel.getBlockState(above).is(Blocks.PINK_PETALS)) {
                serverLevel.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
            }
            serverLevel.setBlockAndUpdate(pos, entry.getValue());
            if (savedBlockEntities.containsKey(pos)) {
                BlockEntity be = serverLevel.getBlockEntity(pos);
                if (be != null) {
                    be.loadWithComponents(savedBlockEntities.get(pos), serverLevel.registryAccess());
                }
            }
        }
        savedBlocks.clear();
        savedBlockEntities.clear();
    }

    public boolean isUsingDimensionRules() {
        if (this.level().isClientSide)
            return true;
        ServerLevel serverLevel = (ServerLevel) this.level();
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
        if (getOwnerId() != null) {
            PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(getOwnerId());
            return settings.isDomainExpansionUseDimensionRules();
        }
        return Config.DOMAIN_EXPANSION_USE_DIMENSION_RULES.get();
    }

    public boolean isUsingEntityProtection() {
        if (this.level().isClientSide)
            return true;
        ServerLevel serverLevel = (ServerLevel) this.level();
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
        if (getOwnerId() != null) {
            PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(getOwnerId());
            return settings.isDomainExpansionUseEntityProtection();
        }
        return Config.DOMAIN_EXPANSION_USE_ENTITY_PROTECTION.get();
    }

    private void applyEffects() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        UUID ownerId = getOwnerId();
        if (ownerId == null)
            return;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
        PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(ownerId);

        for (Entity e : serverLevel.getEntities(this, this.getBoundingBox().inflate(RADIUS))) {
            double dist = e.position().distanceTo(this.position());
            if (dist <= RADIUS) {
                if (!Touhoulittlemaidpersonaldimension.isAllowed(e, ownerId, serverLevel, settings)) {
                    // Remove entity if config enabled; otherwise teleport outside the domain
                    if (Config.REMOVE_BLOCKED_ENTITIES.get() && !(e instanceof Player) && !(e instanceof EntityMaid)) {
                        e.discard();
                    } else {
                        // Teleport to a point just beyond the domain radius
                        double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                        double distance = RADIUS + 5.0;
                        double targetX = this.getX() + Math.cos(angle) * distance;
                        double targetZ = this.getZ() + Math.sin(angle) * distance;
                        double targetY = e.getY();
                        e.teleportTo(targetX, targetY, targetZ);
                    }
                } else {
                    // Apply each survival option independently (same logic as the personal
                    // dimension tick)
                    if (e instanceof Player player && (settings.isDisableHunger() || Config.DISABLE_HUNGER.get())) {
                        player.getFoodData().setFoodLevel(20);
                    }
                    if (e instanceof LivingEntity living
                            && (settings.isNaturalHealing() || Config.NATURAL_HEALING.get())
                            && living.getHealth() < living.getMaxHealth()) {
                        living.heal(1.0f);
                    }
                    if (e instanceof EntityMaid maid
                            && (settings.isMaidEmitLight() || Config.MAID_EMIT_LIGHT.get())) {
                        BlockPos p = maid.blockPosition();
                        if (serverLevel.getBlockState(p).isAir()) {
                            serverLevel.setBlockAndUpdate(p, Blocks.LIGHT.defaultBlockState());
                        }
                    }
                    // Apply combat buffs/debuffs only when entity protection is enabled
                    if (isUsingEntityProtection()) {
                        // Apply buffs to owner and maid, debuffs to others
                        if (e instanceof Player player) {
                            if (player.getUUID().equals(ownerId)) {
                                // Owner buffs: Strength III, Regeneration II, Resistance III
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, true));
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false, true));
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, true));
                            } else {
                                // Non-allied player debuffs: Weakness II, Slowness X
                                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false, true));
                            }
                        } else if (e instanceof EntityMaid maid) {
                            // Maid buffs: Strength III, Regeneration II, Resistance III
                            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, true));
                            maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false, true));
                            maid.addEffect(
                                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, true));
                        } else if (e instanceof LivingEntity living) {
                            // Non-allied entity debuffs
                            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
                            living.addEffect(
                                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false, true));
                        }
                    }
                }
            }
        }
    }
}
