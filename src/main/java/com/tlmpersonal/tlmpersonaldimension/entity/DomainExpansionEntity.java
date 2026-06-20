package com.tlmpersonal.tlmpersonaldimension.entity;

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

public class DomainExpansionEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData
            .defineId(DomainExpansionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> MAID_ID = SynchedEntityData
            .defineId(DomainExpansionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> TICK_COUNT_REMAINING = SynchedEntityData
            .defineId(DomainExpansionEntity.class, EntityDataSerializers.INT);

    private final Map<BlockPos, BlockState> savedBlocks = new HashMap<>();
    private final Map<BlockPos, CompoundTag> savedBlockEntities = new HashMap<>();
    private final Map<UUID, Vec3> savedEntityPositions = new HashMap<>();
    private boolean initialized = false;
    private final int RADIUS = 64;

    public DomainExpansionEntity(EntityType<?> entityType, Level level) {
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, Optional.empty());
        builder.define(MAID_ID, Optional.empty());
        builder.define(TICK_COUNT_REMAINING, Config.DOMAIN_EXPANSION_DURATION_SECONDS.get() * 20);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("OwnerId"))
            setOwnerId(compound.getUUID("OwnerId"));
        if (compound.hasUUID("MaidId"))
            setMaidId(compound.getUUID("MaidId"));
        this.entityData.set(TICK_COUNT_REMAINING, compound.getInt("RemainingTicks"));
        this.initialized = compound.getBoolean("Initialized");

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
        compound.putBoolean("Initialized", this.initialized);

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

        // Ensure serverLevel reference
        ServerLevel serverLevel = (ServerLevel) this.level();

        if (!initialized) {
            initializeDomain();
            initialized = true;
        }

        int remaining = this.entityData.get(TICK_COUNT_REMAINING);
        if (remaining <= 0) {
            restoreDomain();
            this.discard();
            return;
        }
        this.entityData.set(TICK_COUNT_REMAINING, remaining - 1);

        // On first tick, remove any item or experience orb entities to prevent drops
        if (remaining == Config.DOMAIN_EXPANSION_DURATION_SECONDS.get() * 20) {
            for (Entity e : serverLevel.getEntities(this, this.getBoundingBox().inflate(RADIUS))) {
                String name = e.getClass().getSimpleName();
                if (name.equalsIgnoreCase("ItemEntity") || name.equalsIgnoreCase("ExperienceOrb")) {
                    e.discard();
                }
            }
        }

        if (remaining % 20 == 0 && isUsingEntityProtection()) {
            applyEffects();
        }
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
                    // Apply each survival option independently (same logic as the personal dimension tick)
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
                                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, true));
                                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false, true));
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
                            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, true));
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



    private void initializeDomain() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        BlockPos center = this.blockPosition();

        // Save entity positions before modifications
        for (Entity e : serverLevel.getEntities(this, this.getBoundingBox().inflate(RADIUS))) {
            savedEntityPositions.put(e.getUUID(), e.position());
        }

        // Save blocks
        for (int y = RADIUS; y >= -RADIUS; y--) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq <= RADIUS * RADIUS) {
                        BlockPos p = center.offset(x, y, z);
                        BlockState state = serverLevel.getBlockState(p);
                        if (!state.isAir()) {
                            savedBlocks.put(p, state);
                            BlockEntity be = serverLevel.getBlockEntity(p);
                            if (be != null) {
                                savedBlockEntities.put(p, be.saveWithFullMetadata(serverLevel.registryAccess()));
                            }
                            // Use flag 18 to avoid drops during structural replacements if possible, but 2
                            // is fine since we go top down.
                            serverLevel.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        // Barrier blocks removed as per user request

        // Removed solid floor generation as per user request

        // Place structure
        StructureTemplateManager templateManager = serverLevel.getServer().getStructureManager();
        Optional<StructureTemplate> templateOpt = templateManager
                .get(ResourceLocation.fromNamespaceAndPath(Touhoulittlemaidpersonaldimension.MODID, "my_island"));
        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            BlockPos structurePos = center.offset(-template.getSize().getX() / 2, -template.getSize().getY() / 2,
                    -template.getSize().getZ() / 2);
            StructurePlaceSettings settings = new StructurePlaceSettings();
            template.placeInWorld(serverLevel, structurePos, structurePos, settings, serverLevel.random, 3);
        }
    }

    private void restoreDomain() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        BlockPos center = this.blockPosition();

        // Clear everything inside
        for (int y = RADIUS; y >= -RADIUS; y--) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq <= RADIUS * RADIUS) {
                        BlockPos p = center.offset(x, y, z);
                        serverLevel.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Restore saved blocks
        List<Map.Entry<BlockPos, BlockState>> sortedEntries = new ArrayList<>(savedBlocks.entrySet());
        sortedEntries.sort(Comparator.comparingInt(e -> e.getKey().getY()));
        for (Map.Entry<BlockPos, BlockState> entry : sortedEntries) {
            serverLevel.setBlock(entry.getKey(), entry.getValue(), 3);
            if (savedBlockEntities.containsKey(entry.getKey())) {
                BlockEntity be = serverLevel.getBlockEntity(entry.getKey());
                if (be != null) {
                    be.loadWithComponents(savedBlockEntities.get(entry.getKey()), serverLevel.registryAccess());
                }
            }
        }

        // Restore saved entity positions
        for (Map.Entry<UUID, Vec3> entry : savedEntityPositions.entrySet()) {
            Entity e = serverLevel.getEntity(entry.getKey());
            if (e != null) {
                Vec3 pos = entry.getValue();
                e.teleportTo(pos.x, pos.y, pos.z);
            }
        }
    }
}
