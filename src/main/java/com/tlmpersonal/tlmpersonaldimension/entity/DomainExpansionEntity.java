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
    // UUIDs of entities spawned by the structure itself, to be discarded on restore
    private final java.util.Set<UUID> structureEntities = new java.util.HashSet<>();
    // UUIDs of item/XP entities that existed before the domain was summoned — must
    // be preserved
    private final java.util.Set<UUID> preExistingItems = new java.util.HashSet<>();
    private boolean initialized = false;
    private final int RADIUS = 64;
    // Bounding box of the placed structure, used for effect range checks
    private net.minecraft.world.phys.AABB structureAABB = null;

    // Command-time overrides (null/negative = fall back to config)
    private String structureOverride = null;
    private int durationOverride = -2; // -2 = unset, -1 = infinite, >=0 = seconds
    private boolean costOverride = true; // false = no XP cost regardless of config
    private boolean hasCostOverride = false; // whether costOverride was explicitly set

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

    /** Set a structure override for this domain (used by commands). */
    public void setStructureOverride(String structureName) {
        this.structureOverride = structureName;
    }

    /** Set a duration override in seconds. -1 = infinite. */
    public void setDurationOverride(int seconds) {
        this.durationOverride = seconds;
        // Update synched tick count immediately if entity data is already initialised
        int ticks = (seconds < 0) ? -1 : (seconds * 20);
        this.entityData.set(TICK_COUNT_REMAINING, ticks);
    }

    /** Override whether XP cost is enabled for this domain. */
    public void setCostOverride(boolean enabled) {
        this.costOverride = enabled;
        this.hasCostOverride = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, Optional.empty());
        this.entityData.define(MAID_ID, Optional.empty());
        int durationSeconds = Config.DOMAIN_EXPANSION_DURATION_SECONDS.get();
        int ticksRemaining = (durationSeconds < 0) ? -1 : (durationSeconds * 20);
        this.entityData.define(TICK_COUNT_REMAINING, ticksRemaining);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("OwnerId"))
            setOwnerId(compound.getUUID("OwnerId"));
        if (compound.hasUUID("MaidId"))
            setMaidId(compound.getUUID("MaidId"));
        this.entityData.set(TICK_COUNT_REMAINING, compound.getInt("RemainingTicks"));
        this.initialized = compound.getBoolean("Initialized");
        if (compound.contains("StructureOverride"))
            this.structureOverride = compound.getString("StructureOverride");
        if (compound.contains("DurationOverride"))
            this.durationOverride = compound.getInt("DurationOverride");
        if (compound.contains("CostOverride")) {
            this.costOverride = compound.getBoolean("CostOverride");
            this.hasCostOverride = true;
        }

        if (compound.contains("SavedBlocks")) {
            ListTag blockList = compound.getList("SavedBlocks", 10);
            for (int i = 0; i < blockList.size(); i++) {
                CompoundTag tag = blockList.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(tag.getCompound("pos"));
                // Forge 1.20.1: readBlockState needs HolderGetter from level's registry access
                BlockState state = NbtUtils.readBlockState(this.level().holderLookup(net.minecraft.core.registries.Registries.BLOCK),
                        tag.getCompound("state"));
                savedBlocks.put(pos, state);
                if (tag.contains("entity")) {
                    savedBlockEntities.put(pos, tag.getCompound("entity"));
                }
            }
        }
        if (compound.contains("StructureAABB")) {
            CompoundTag aabbTag = compound.getCompound("StructureAABB");
            structureAABB = new net.minecraft.world.phys.AABB(
                    aabbTag.getDouble("minX"), aabbTag.getDouble("minY"), aabbTag.getDouble("minZ"),
                    aabbTag.getDouble("maxX"), aabbTag.getDouble("maxY"), aabbTag.getDouble("maxZ"));
        }
        if (compound.contains("StructureEntities")) {
            ListTag list = compound.getList("StructureEntities", 11);
            for (int i = 0; i < list.size(); i++) {
                structureEntities.add(NbtUtils.loadUUID(list.get(i)));
            }
        }
        if (compound.contains("PreExistingItems")) {
            ListTag list = compound.getList("PreExistingItems", 11);
            for (int i = 0; i < list.size(); i++) {
                preExistingItems.add(NbtUtils.loadUUID(list.get(i)));
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
        if (structureOverride != null)
            compound.putString("StructureOverride", structureOverride);
        if (durationOverride != -2)
            compound.putInt("DurationOverride", durationOverride);
        if (hasCostOverride)
            compound.putBoolean("CostOverride", costOverride);

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

        if (structureAABB != null) {
            CompoundTag aabbTag = new CompoundTag();
            aabbTag.putDouble("minX", structureAABB.minX);
            aabbTag.putDouble("minY", structureAABB.minY);
            aabbTag.putDouble("minZ", structureAABB.minZ);
            aabbTag.putDouble("maxX", structureAABB.maxX);
            aabbTag.putDouble("maxY", structureAABB.maxY);
            aabbTag.putDouble("maxZ", structureAABB.maxZ);
            compound.put("StructureAABB", aabbTag);
        }

        ListTag structureEntityList = new ListTag();
        for (UUID uuid : structureEntities) {
            structureEntityList.add(NbtUtils.createUUID(uuid));
        }
        compound.put("StructureEntities", structureEntityList);

        ListTag preExistingItemList = new ListTag();
        for (UUID uuid : preExistingItems) {
            preExistingItemList.add(NbtUtils.createUUID(uuid));
        }
        compound.put("PreExistingItems", preExistingItemList);
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
        boolean infinite = (remaining < 0);

        if (!infinite) {
            if (remaining == 0) {
                restoreDomain();
                this.discard();
                return;
            }
            this.entityData.set(TICK_COUNT_REMAINING, remaining - 1);
        }

        // Use tickCount for periodic work so infinite mode still applies effects
        int periodicTick = this.tickCount;

        boolean canSustain = true;
        boolean xpCostEnabled = hasCostOverride ? costOverride : Config.DOMAIN_EXPANSION_XP_COST_ENABLED.get();
        if (xpCostEnabled && getOwnerId() != null) {
            Player owner = serverLevel.getServer().getPlayerList().getPlayer(getOwnerId());
            if (owner != null) {
                int cost = Config.DOMAIN_EXPANSION_XP_COST.get();
                if (cost > 0) {
                    int intervalTicks = Config.DOMAIN_EXPANSION_XP_COST_INTERVAL_SECONDS.get() * 20;
                    if (owner.experienceLevel < cost) {
                        canSustain = false;
                    } else if (periodicTick % intervalTicks == 0) {
                        owner.giveExperienceLevels(-cost);
                    }
                }
            } else {
                canSustain = false;
            }
        }

        if (!canSustain) {
            restoreDomain();
            this.discard();
            return;
        }

        if (periodicTick % 20 == 0) {
            applyEffects();
        }

        // Continuously track new item/XP drops inside the domain so they survive
        // restore
        if (structureAABB != null) {
            for (Entity e : serverLevel.getEntities(this, structureAABB)) {
                if (e instanceof net.minecraft.world.entity.item.ItemEntity
                        || e instanceof net.minecraft.world.entity.ExperienceOrb) {
                    preExistingItems.add(e.getUUID());
                }
            }
        }
    }

    private void applyMaidLight(ServerLevel serverLevel) {
        UUID ownerId = getOwnerId();
        if (ownerId == null)
            return;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
        PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(ownerId);

        net.minecraft.world.phys.AABB searchAABB = structureAABB;
        if (searchAABB == null) return;

        if (settings.isMaidEmitLight() || Config.MAID_EMIT_LIGHT.get()) {
            for (Entity e : serverLevel.getEntities(this, searchAABB)) {
                if (!(e instanceof EntityMaid maid))
                    continue;
                BlockPos newLightPos = maid.blockPosition().above();
                Touhoulittlemaidpersonaldimension.MaidLightEntry lastEntry =
                        Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.get(maid.getUUID());
                BlockPos lastLightPos = lastEntry != null ? lastEntry.pos() : null;
                if (lastEntry != null && lastEntry.dimension().equals(serverLevel.dimension()) && lastLightPos.equals(newLightPos)) {
                    continue;
                }
                // Always remove old light first if maid has moved (uses right level via entry)
                if (lastLightPos != null && (!lastEntry.dimension().equals(serverLevel.dimension()) || !lastLightPos.equals(newLightPos))) {
                    Touhoulittlemaidpersonaldimension.removeMaidLight(maid.getUUID(), serverLevel.getServer());
                }
                // Place new light if spot is free
                BlockState atNew = serverLevel.getBlockState(newLightPos);
                if (atNew.isAir() || atNew.is(Blocks.LIGHT)) {
                    serverLevel.setBlockAndUpdate(newLightPos, Blocks.LIGHT.defaultBlockState());
                    Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.put(maid.getUUID(),
                            new Touhoulittlemaidpersonaldimension.MaidLightEntry(newLightPos, serverLevel.dimension()));
                } else if (lastLightPos != null && !lastLightPos.equals(newLightPos)) {
                    // Can't place at new pos, but maid has moved — remove from map
                    Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.remove(maid.getUUID());
                }
            }
        }

        // Clean up lights for maids no longer in range or when setting is off.
        // IMPORTANT: use entry.dimension() to get the right ServerLevel — do NOT use
        // serverLevel directly, the light may have been placed in a different dimension.
        Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.entrySet().removeIf(entry -> {
            if (entry.getValue().dimension().equals(serverLevel.dimension())
                    && serverLevel.getEntity(entry.getKey()) == null) {
                Touhoulittlemaidpersonaldimension.MaidLightEntry lightEntry = entry.getValue();
                ServerLevel lightLevel = serverLevel.getServer().getLevel(lightEntry.dimension());
                if (lightLevel != null && lightLevel.getBlockState(lightEntry.pos()).is(Blocks.LIGHT)) {
                    lightLevel.setBlockAndUpdate(lightEntry.pos(), Blocks.AIR.defaultBlockState());
                }
                return true;
            }
            return false;
        });
    }

    public net.minecraft.world.phys.AABB getStructureAABB() {
        return structureAABB;
    }

    /** Returns the effective structure name used by this domain (override or config). */
    public String getActiveStructureName() {
        if (structureOverride != null && !structureOverride.isEmpty()) return structureOverride;
        return com.tlmpersonal.tlmpersonaldimension.Config.DOMAIN_EXPANSION_STRUCTURE.get();
    }

    /** Public entry point so commands can cleanly collapse a domain. */
    public void callRestoreAndDiscard() {
        if (!this.level().isClientSide) {
            restoreDomain();
            this.discard();
        }
    }

    public boolean isUsingDimensionRules() {
        if (this.level().isClientSide)
            return true;
        return Config.DOMAIN_EXPANSION_USE_DIMENSION_RULES.get();
    }

    public boolean isUsingEntityProtection() {
        if (this.level().isClientSide)
            return true;
        return Config.DOMAIN_EXPANSION_USE_ENTITY_PROTECTION.get();
    }

    public boolean isUsingEntityFiltering() {
        if (this.level().isClientSide)
            return true;
        return Config.DOMAIN_EXPANSION_USE_ENTITY_FILTERING.get();
    }

    private void applyEffects() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        UUID ownerId = getOwnerId();
        if (ownerId == null)
            return;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
        PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(ownerId);

        net.minecraft.world.phys.AABB effectAABB = structureAABB;
        if (effectAABB == null) return;

        // Cache owner and maid list once for the whole tick — avoids repeated entity scans
        Player owner = serverLevel.getPlayerByUUID(ownerId);
        List<EntityMaid> maidsInDomain = serverLevel.getEntitiesOfClass(EntityMaid.class, effectAABB);

        for (Entity e : serverLevel.getEntities(this, effectAABB)) {
            if (!effectAABB.contains(e.position()))
                continue;
            if (isUsingEntityFiltering() && !Touhoulittlemaidpersonaldimension.isAllowed(e, ownerId, serverLevel, settings)) {
                // Remove entity if config enabled; otherwise teleport outside the domain
                if (Config.REMOVE_BLOCKED_ENTITIES.get() && !(e instanceof Player) && !(e instanceof EntityMaid)) {
                    e.discard();
                } else {
                    Touhoulittlemaidpersonaldimension.expelFromDomain(e, serverLevel, this.getX(), this.getZ());
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
                // Apply combat buffs/debuffs only when entity protection effects are enabled
                if (isUsingEntityProtection()) {
                    if (e instanceof Player player) {
                        if (player.getUUID().equals(ownerId)) {
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40,
                                    Config.DOMAIN_EXPANSION_ALLY_STRENGTH.get(), false, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40,
                                    Config.DOMAIN_EXPANSION_ALLY_REGEN.get(), false, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40,
                                    Config.DOMAIN_EXPANSION_ALLY_RESISTANCE.get(), false, false, true));
                        } else if (isMaidOrOwnerTarget(e, owner, maidsInDomain)) {
                            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40,
                                    Config.DOMAIN_EXPANSION_ENEMY_WEAKNESS.get(), false, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40,
                                    Config.DOMAIN_EXPANSION_ENEMY_SLOWNESS.get(), false, false, true));
                        }
                    } else if (e instanceof EntityMaid maid) {
                        maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40,
                                Config.DOMAIN_EXPANSION_ALLY_STRENGTH.get(), false, false, true));
                        maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40,
                                Config.DOMAIN_EXPANSION_ALLY_REGEN.get(), false, false, true));
                        maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40,
                                Config.DOMAIN_EXPANSION_ALLY_RESISTANCE.get(), false, false, true));
                    } else if (e instanceof LivingEntity living && isMaidOrOwnerTarget(e, owner, maidsInDomain)) {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40,
                                Config.DOMAIN_EXPANSION_ENEMY_WEAKNESS.get(), false, false, true));
                        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40,
                                Config.DOMAIN_EXPANSION_ENEMY_SLOWNESS.get(), false, false, true));
                    }
                }
            }
        }
    }

    /**
     * Returns true if the candidate is being targeted by the owner or any domain maid,
     * or is itself targeting the owner or a maid.
     * Accepts pre-cached owner and maid list to avoid repeated entity scans per tick.
     */
    private boolean isMaidOrOwnerTarget(Entity candidate, Player owner, List<EntityMaid> maidsInDomain) {
        UUID candidateId = candidate.getUUID();

        // Owner's last hurt mob (what the owner is actively attacking)
        if (owner != null) {
            LivingEntity ownerTarget = owner.getLastHurtMob();
            if (ownerTarget != null && ownerTarget.getUUID().equals(candidateId)) {
                return true;
            }
        }

        // Any maid's current target
        for (EntityMaid maid : maidsInDomain) {
            LivingEntity maidTarget = maid.getTarget();
            if (maidTarget != null && maidTarget.getUUID().equals(candidateId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean willDropItems(BlockState state) {
        if (state.isAir()) return false;
        return !state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                || state.is(net.minecraft.tags.BlockTags.CROPS)
                || state.is(net.minecraft.tags.BlockTags.SAPLINGS)
                || state.is(net.minecraft.tags.BlockTags.FLOWERS)
                || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.SugarCaneBlock;
    }

    /**
     * Fallback NBT loader when StructureTemplateManager doesn't find the structure.
     * Tries both "structures/" (plural, standard) and "structure/" (singular) resource paths.
     */
    private static StructureTemplate loadStructureFromResources(ServerLevel level, ResourceLocation id) {
        // In 1.20.1 the correct folder is "structures" (plural). Renamed to "structure" in 1.21.
        String[] paths = {
            "data/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt"
        };
        for (String resourcePath : paths) {
            try (java.io.InputStream stream = DomainExpansionEntity.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) continue;
                net.minecraft.nbt.CompoundTag nbt = net.minecraft.nbt.NbtIo.readCompressed(stream);
                StructureTemplate template = new StructureTemplate();
                template.load(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), nbt);
                Touhoulittlemaidpersonaldimension.LOGGER.info("Manually loaded structure '{}' from {}", id, resourcePath);
                return template;
            } catch (Exception e) {
                Touhoulittlemaidpersonaldimension.LOGGER.warn("Failed to load structure from {}: {}", resourcePath, e.getMessage());
            }
        }
        return null;
    }

    private void initializeDomain() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        BlockPos center = this.blockPosition();

        // Save entity positions before modifications, and snapshot pre-existing items
        for (Entity e : serverLevel.getEntities(this, this.getBoundingBox().inflate(RADIUS))) {
            savedEntityPositions.put(e.getUUID(), e.position());
            if (e instanceof net.minecraft.world.entity.item.ItemEntity
                    || e instanceof net.minecraft.world.entity.ExperienceOrb) {
                preExistingItems.add(e.getUUID());
            }
        }

        // Place structure and save only the blocks it will overwrite
        StructureTemplateManager templateManager = serverLevel.getServer().getStructureManager();
        
        // Parse the structure config to support both simple names and full namespaced paths
        String structureConfig = (structureOverride != null && !structureOverride.isEmpty())
                ? structureOverride
                : Config.DOMAIN_EXPANSION_STRUCTURE.get().trim();
        ResourceLocation structureLocation;
        
        try {
            // If the config contains a colon, treat it as a full namespaced path (e.g. "minecraft:village/plains/houses/plains_small_house_1")
            // Otherwise, assume it's a structure in this mod's namespace
            if (structureConfig.contains(":")) {
                structureLocation = new ResourceLocation(structureConfig);
            } else {
                structureLocation = new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, structureConfig);
            }
        } catch (net.minecraft.ResourceLocationException e) {
            // Invalid resource location format
            Touhoulittlemaidpersonaldimension.LOGGER.error("Invalid structure name format: '{}'. Error: {}", structureConfig, e.getMessage());
            
            // Notify the owner
            UUID ownerId = getOwnerId();
            if (ownerId != null) {
                Player owner = serverLevel.getPlayerByUUID(ownerId);
                if (owner != null) {
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cDomain Expansion failed: Invalid structure name format!"));
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7Config value: '" + structureConfig + "'"));
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7Must be either 'structure_name' or 'namespace:path/to/structure'"));
                }
            }
            return;
        }
        
        Optional<StructureTemplate> templateOpt = templateManager.get(structureLocation);

        // Fallback: manually load NBT from mod jar resources if StructureTemplateManager missed it
        if (templateOpt.isEmpty()) {
            Touhoulittlemaidpersonaldimension.LOGGER.warn("StructureTemplateManager did not find '{}', attempting manual NBT load", structureLocation);
            StructureTemplate loaded = loadStructureFromResources(serverLevel, structureLocation);
            if (loaded != null) {
                templateOpt = Optional.of(loaded);
            }
        }

        // If not found and looks like a structure set (no path), try to give helpful message
        if (templateOpt.isEmpty() && !structureConfig.contains("/")) {
            Touhoulittlemaidpersonaldimension.LOGGER.warn("Structure '{}' not found. If this is a structure set like 'minecraft:village_plains', you need to use a specific structure piece path instead.", structureLocation);
        }
        if (templateOpt.isPresent()) {
            StructureTemplate template = templateOpt.get();
            BlockPos structurePos = center.offset(-template.getSize().getX() / 2, 0,
                    -template.getSize().getZ() / 2);

            // Save ALL blocks within the structure's footprint before placing (including
            // air, so positions overwritten by the structure's own air blocks are also restored)
            net.minecraft.core.Vec3i size = template.getSize();
            for (int y = 0; y < size.getY(); y++) {
                for (int x = 0; x < size.getX(); x++) {
                    for (int z = 0; z < size.getZ(); z++) {
                        BlockPos p = structurePos.offset(x, y, z);
                        BlockState state = serverLevel.getBlockState(p);
                        savedBlocks.put(p, state);
                        BlockEntity be = serverLevel.getBlockEntity(p);
                        if (be != null) {
                            // Forge 1.20.1: saveWithFullMetadata doesn't take parameters
                            savedBlockEntities.put(p, be.saveWithFullMetadata());
                        }
                    }
                }
            }

            // Also save the layer directly below the structure footprint — vanilla will
            // convert farmland to dirt when a solid block is placed above it, and since
            // those positions are outside the footprint DE wouldn't restore them otherwise.
            for (int x = 0; x < size.getX(); x++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos p = structurePos.offset(x, -1, z);
                    if (!savedBlocks.containsKey(p)) {
                        savedBlocks.put(p, serverLevel.getBlockState(p));
                    }
                }
            }

            // Allow entities saved in the structure NBT to be spawned.
            StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);

            // Silently remove any blocks that would drop items when overwritten by the structure
            // (crops, plants, etc.) — prevents duplicate items when the domain restores them.
            for (int y = 0; y < size.getY(); y++) {
                for (int x = 0; x < size.getX(); x++) {
                    for (int z = 0; z < size.getZ(); z++) {
                        BlockPos p = structurePos.offset(x, y, z);
                        BlockState state = serverLevel.getBlockState(p);
                        if (!state.isAir() && willDropItems(state)) {
                            // Flag 4 = no drops, 2 = send to clients, 16 = no observer updates
                            serverLevel.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 4 | 2 | 16);
                        }
                    }
                }
            }

            template.placeInWorld(serverLevel, structurePos, structurePos, settings, serverLevel.random, 3);

            // Build AABB from structure footprint for effect range checks
            structureAABB = new net.minecraft.world.phys.AABB(
                    structurePos.getX(), structurePos.getY(), structurePos.getZ(),
                    structurePos.getX() + size.getX(), structurePos.getY() + size.getY(),
                    structurePos.getZ() + size.getZ());

            // Only teleport entities to the ground for my_island structure
            String resolvedStructure = (structureOverride != null && !structureOverride.isEmpty())
                    ? structureOverride : Config.DOMAIN_EXPANSION_STRUCTURE.get();
            if (resolvedStructure.equals("my_island")) {
                // Teleport entities in the structure area down to the ground of the domain (25 blocks above structurePos Y)
                double groundY = structurePos.getY() + 25.0 + 1.0;
                for (Entity e : serverLevel.getEntities(this, structureAABB)) {
                    if (e == this) continue;
                    // Exclude technical entities
                    if (e instanceof net.minecraft.world.entity.item.ItemEntity 
                            || e instanceof net.minecraft.world.entity.ExperienceOrb
                            || e instanceof net.minecraft.world.entity.projectile.Projectile
                            || e instanceof net.minecraft.world.entity.decoration.Painting
                            || e instanceof net.minecraft.world.entity.decoration.ItemFrame
                            || net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).toString().equals("touhou_little_maid:chair")) {
                        continue;
                    }
                    // Check for air at entity's feet and head to prevent suffocation
                    BlockPos feetPos = new BlockPos((int)e.getX(), (int)groundY, (int)e.getZ());
                    BlockPos headPos = feetPos.above();
                    if (serverLevel.getBlockState(feetPos).isAir() && serverLevel.getBlockState(headPos).isAir()) {
                        e.teleportTo(e.getX(), groundY, e.getZ());
                    } else {
                        // If not enough air, search upwards for a safe spot
                        BlockPos.MutableBlockPos safePos = new BlockPos.MutableBlockPos((int)e.getX(), (int)groundY, (int)e.getZ());
                        int maxSearch = 10;
                        for (int i = 0; i < maxSearch; i++) {
                            if (serverLevel.getBlockState(safePos).isAir() && serverLevel.getBlockState(safePos.above()).isAir()) {
                                e.teleportTo(e.getX(), safePos.getY(), e.getZ());
                                break;
                            }
                            safePos.move(0, 1, 0);
                        }
                    }
                }
            }

            // Collect entities spawned by the structure (not present before placement)
            for (Entity e : serverLevel.getEntities(this, structureAABB)) {
                if (!savedEntityPositions.containsKey(e.getUUID()) && !(e instanceof Player) && e != this) {
                    structureEntities.add(e.getUUID());
                }
            }
        } else {
            // Structure template not found - log warning and notify owner
            Touhoulittlemaidpersonaldimension.LOGGER.error("Domain Expansion structure template not found: {}", structureLocation);
            Touhoulittlemaidpersonaldimension.LOGGER.error("Make sure the structure file exists at: data/{}/structures/{}.nbt", 
                structureLocation.getNamespace(), structureLocation.getPath());
            Touhoulittlemaidpersonaldimension.LOGGER.error("NOTE: Structure sets like 'minecraft:village_plains' won't work - you need specific piece paths like 'minecraft:village/plains/houses/plains_small_house_1'");
            
            // Notify the owner if present
            UUID ownerId = getOwnerId();
            if (ownerId != null) {
                Player owner = serverLevel.getPlayerByUUID(ownerId);
                if (owner != null) {
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cDomain Expansion failed: Structure '" + structureLocation + "' not found!"));
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§7Structure template must exist as an NBT file."));
                    if (!structureConfig.contains("/")) {
                        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§7Tip: Use specific structure piece paths like 'minecraft:village/plains/houses/plains_small_house_1'"));
                    }
                }
            }
        }
    }

    private void restoreDomain() {
        ServerLevel serverLevel = (ServerLevel) this.level();

        // Discard all entities that were spawned by the structure
        for (UUID uuid : structureEntities) {
            Entity e = serverLevel.getEntity(uuid);
            if (e != null)
                e.discard();
        }
        // Also sweep the structure footprint for any item drops or leftover entities,
        // but preserve items that existed before the domain was summoned
        if (structureAABB != null) {
            for (Entity e : serverLevel.getEntities(this, structureAABB)) {
                if ((e instanceof net.minecraft.world.entity.item.ItemEntity
                        || e instanceof net.minecraft.world.entity.ExperienceOrb)
                        && !preExistingItems.contains(e.getUUID())) {
                    e.discard();
                }
            }
        }

        // Clear only the blocks that were saved (structure footprint) before restoring.
        // Must go top-down so dependent blocks (petals, grass, etc.) are removed before
        // their support blocks, preventing physics-triggered item drops.
        List<BlockPos> clearOrder = new ArrayList<>(savedBlocks.keySet());
        clearOrder.sort((a, b) -> Integer.compare(b.getY(), a.getY())); // highest Y first
        for (BlockPos p : clearOrder) {
            // Flag 4 = no drops, 2 = send to clients, 16 = no observer updates (suppress
            // cascades)
            serverLevel.setBlock(p, Blocks.AIR.defaultBlockState(), 4 | 2 | 16);
        }

        // Restore saved blocks (bottom-up so support blocks exist before dependent
        // ones)
        List<Map.Entry<BlockPos, BlockState>> sortedEntries = new ArrayList<>(savedBlocks.entrySet());
        sortedEntries.sort(Comparator.comparingInt(e -> e.getKey().getY()));
        for (Map.Entry<BlockPos, BlockState> entry : sortedEntries) {
            serverLevel.setBlock(entry.getKey(), entry.getValue(), 4 | 2 | 16);
            if (savedBlockEntities.containsKey(entry.getKey())) {
                BlockEntity be = serverLevel.getBlockEntity(entry.getKey());
                if (be != null) {
                    // Forge 1.20.1: use load() instead of loadWithComponents()
                    be.load(savedBlockEntities.get(entry.getKey()));
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

        // Clean up any maid light blocks left by this domain (uses correct level per entry)
    }
}
