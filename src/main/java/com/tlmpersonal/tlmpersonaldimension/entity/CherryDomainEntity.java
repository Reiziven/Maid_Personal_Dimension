package com.tlmpersonal.tlmpersonaldimension.entity;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

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
    private final Set<BlockPos> containerPositions = new HashSet<>();
    private final Map<BlockPos, Integer> outOfRangeCounter = new HashMap<>();
    private final Map<UUID, DyeColor> savedSheepColors = new HashMap<>();
    private final Map<UUID, Boat.Type> savedBoatTypes = new HashMap<>();
    private final int RADIUS = 5;
    private static final int HORIZONTAL_RADIUS = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
    private static final int VERTICAL_HALF = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
    private static final int SET_BLOCK_FLAGS = 2 | 16;

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
        this.entityData.set(TICK_COUNT_REMAINING, 20);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, Optional.empty());
        builder.define(MAID_ID, Optional.empty());
        builder.define(TICK_COUNT_REMAINING, 20);
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
                if (tag.getBoolean("container")) {
                    containerPositions.add(pos);
                }
            }
        }

        if (compound.contains("SavedSheep")) {
            ListTag sheepList = compound.getList("SavedSheep", 10);
            for (int i = 0; i < sheepList.size(); i++) {
                CompoundTag tag = sheepList.getCompound(i);
                savedSheepColors.put(tag.getUUID("uuid"), DyeColor.byId(tag.getInt("color")));
            }
        }

        if (compound.contains("SavedBoats")) {
            ListTag boatList = compound.getList("SavedBoats", 10);
            for (int i = 0; i < boatList.size(); i++) {
                CompoundTag tag = boatList.getCompound(i);
                savedBoatTypes.put(tag.getUUID("uuid"), Boat.Type.byId(tag.getInt("type")));
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
            if (containerPositions.contains(entry.getKey())) {
                tag.putBoolean("container", true);
            }
            blockList.add(tag);
        }
        compound.put("SavedBlocks", blockList);

        ListTag sheepList = new ListTag();
        for (Map.Entry<UUID, DyeColor> entry : savedSheepColors.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", entry.getKey());
            tag.putInt("color", entry.getValue().getId());
            sheepList.add(tag);
        }
        compound.put("SavedSheep", sheepList);

        ListTag boatList = new ListTag();
        for (Map.Entry<UUID, Boat.Type> entry : savedBoatTypes.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", entry.getKey());
            tag.putInt("type", entry.getValue().ordinal());
            boatList.add(tag);
        }
        compound.put("SavedBoats", boatList);
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

        applyMaidLight(serverLevel, maid);

        spawnCherryParticles(serverLevel, maid);
        if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == serverLevel) {
            spawnCherryParticles(serverLevel, owner);
        }

        if (remaining % 20 == 0) {
            // If any DomainExpansion is active, pause block transformation and restore
            // all cherry blocks immediately so DE snapshots clean originals.
            if (isDomainExpansionActive(serverLevel)) {
                if (!savedBlocks.isEmpty()) {
                    restoreDomain();
                }
            } else {
                updateBlocks(serverLevel, maid, owner);
            }
            updateEntities(serverLevel, maid, owner);
        }
    }

    // ======================== BLOCK REPLACEMENT LOGIC ========================

    private void updateBlocks(ServerLevel level, EntityMaid maid, Player owner) {
        Set<BlockPos> inRange = new HashSet<>();
        collectPositionsInRange(maid, inRange);
        if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == level) {
            collectPositionsInRange(owner, inRange);
        }

        List<BlockPos> toRestore = new ArrayList<>();
        for (BlockPos pos : new ArrayList<>(savedBlocks.keySet())) {
            if (!inRange.contains(pos)) {
                int count = outOfRangeCounter.getOrDefault(pos, 0) + 1;
                if (count >= 6) {
                    toRestore.add(pos);
                    outOfRangeCounter.remove(pos);
                } else {
                    outOfRangeCounter.put(pos, count);
                }
            } else {
                outOfRangeCounter.remove(pos);
            }
        }

        toRestore.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        for (BlockPos pos : toRestore) {
            restoreBlock(level, pos);
        }

        if (maid != null)
            transformAroundEntity(maid, level);
        if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == level) {
            transformAroundEntity(owner, level);
        }
    }

    private void collectPositionsInRange(Entity entity, Set<BlockPos> inRange) {
        if (entity == null)
            return;
        BlockPos center = entity.blockPosition();
        for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
            for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                for (int y = -VERTICAL_HALF; y <= VERTICAL_HALF - 1; y++) {
                    inRange.add(center.offset(x, y, z));
                }
            }
        }
    }

    private void transformAroundEntity(Entity entity, ServerLevel level) {
        BlockPos center = entity.blockPosition();

        // 1) Floor petals: 5x5 at feet level, only on grass blocks
        for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
            for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                BlockPos floorPos = center.offset(x, -1, z);
                BlockPos petalPos = floorPos.above();

                if (savedBlocks.containsKey(petalPos))
                    continue;

                BlockState floorState = level.getBlockState(floorPos);
                if (!floorState.is(Blocks.GRASS_BLOCK))
                    continue;

                BlockState aboveState = level.getBlockState(petalPos);
                if (!aboveState.getFluidState().isEmpty())
                    continue;
                if (isProtectedPlant(aboveState))
                    continue;
                if (!aboveState.isAir() && !aboveState.canBeReplaced() && !aboveState.is(Blocks.PINK_PETALS))
                    continue;
                if (aboveState.is(Blocks.PINK_PETALS))
                    continue;

                savedBlocks.put(petalPos, aboveState);
                level.setBlock(petalPos, Blocks.PINK_PETALS.defaultBlockState(), SET_BLOCK_FLAGS);
            }
        }

        // 2) Block replacement: 5x5 XZ, 20Y vertical
        for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
            for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                for (int y = -VERTICAL_HALF; y <= VERTICAL_HALF - 1; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (savedBlocks.containsKey(pos))
                        continue;

                    BlockState currentState = level.getBlockState(pos);
                    Block replacement = getCherryReplacement(currentState);
                    if (replacement != null) {
                        saveAndReplace(level, pos, currentState, replacement);
                    }
                }
            }
        }
    }

    private void saveAndReplace(ServerLevel level, BlockPos pos, BlockState currentState, Block replacement) {
        savedBlocks.put(pos, currentState);

        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag savedData = null;
        if (be != null) {
            savedData = be.saveWithFullMetadata(level.registryAccess());
            savedBlockEntities.put(pos, savedData);
            if (be instanceof Container container) {
                containerPositions.add(pos);
                container.clearContent();
            }
        }

        BlockState newState = copyMatchingProperties(currentState, replacement);
        level.setBlock(pos, newState, SET_BLOCK_FLAGS);

        if (savedData != null) {
            BlockEntity newBe = level.getBlockEntity(pos);
            if (newBe != null) {
                try {
                    newBe.loadWithComponents(savedData, level.registryAccess());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void restoreBlock(ServerLevel level, BlockPos pos) {
        BlockState currentWorldState = level.getBlockState(pos);

        // Only skip restore if the block was genuinely destroyed (air).
        // If it's solid but not cherry (e.g. replaced by DomainExpansion structure),
        // we still restore — once DE is gone the original needs to come back.
        if (currentWorldState.isAir()) {
            savedBlocks.remove(pos);
            savedBlockEntities.remove(pos);
            containerPositions.remove(pos);
            return;
        }

        BlockEntity existingBe = level.getBlockEntity(pos);
        CompoundTag currentData = null;
        if (existingBe != null && containerPositions.contains(pos)) {
            currentData = existingBe.saveWithFullMetadata(level.registryAccess());
            if (existingBe instanceof Container container) {
                container.clearContent();
            }
        }

        level.setBlock(pos, savedBlocks.get(pos), SET_BLOCK_FLAGS);

        CompoundTag dataToLoad = containerPositions.contains(pos) ? currentData : savedBlockEntities.get(pos);
        if (dataToLoad != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                try {
                    be.loadWithComponents(dataToLoad, level.registryAccess());
                } catch (Exception ignored) {
                }
            }
        }

        savedBlocks.remove(pos);
        savedBlockEntities.remove(pos);
        containerPositions.remove(pos);
    }

    private void restoreDomain() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        List<Map.Entry<BlockPos, BlockState>> sortedEntries = new ArrayList<>(savedBlocks.entrySet());
        sortedEntries.sort((a, b) -> Integer.compare(b.getKey().getY(), a.getKey().getY()));

        for (Map.Entry<BlockPos, BlockState> entry : sortedEntries) {
            BlockPos pos = entry.getKey();

            // Only skip if the block was genuinely destroyed (air) — don't restore into nothing.
            // If a solid block is there (DE structure, or DE already restored the original),
            // restore our original on top — it's the correct pre-cherry state either way.
            if (serverLevel.getBlockState(pos).isAir())
                continue;

            BlockEntity existingBe = serverLevel.getBlockEntity(pos);
            CompoundTag currentData = null;
            if (existingBe != null && containerPositions.contains(pos)) {
                currentData = existingBe.saveWithFullMetadata(serverLevel.registryAccess());
                if (existingBe instanceof Container container) {
                    container.clearContent();
                }
            }

            serverLevel.setBlock(pos, entry.getValue(), SET_BLOCK_FLAGS);

            CompoundTag dataToLoad = containerPositions.contains(pos) ? currentData : savedBlockEntities.get(pos);
            if (dataToLoad != null) {
                BlockEntity be = serverLevel.getBlockEntity(pos);
                if (be != null) {
                    try {
                        be.loadWithComponents(dataToLoad, serverLevel.registryAccess());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        savedBlocks.clear();
        savedBlockEntities.clear();
        containerPositions.clear();

        for (Map.Entry<UUID, DyeColor> entry : savedSheepColors.entrySet()) {
            Entity e = serverLevel.getEntity(entry.getKey());
            if (e instanceof Sheep sheep) {
                sheep.setColor(entry.getValue());
            }
        }
        savedSheepColors.clear();

        for (Map.Entry<UUID, Boat.Type> entry : savedBoatTypes.entrySet()) {
            Entity e = serverLevel.getEntity(entry.getKey());
            if (e instanceof Boat boat) {
                boat.setVariant(entry.getValue());
            }
        }
        savedBoatTypes.clear();
    }

    // ======================== CHERRY/PINK BLOCK MAPPING ========================

    private Block getCherryReplacement(BlockState state) {
        Block block = state.getBlock();
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();

        if (name.contains("cherry") || name.contains("pink"))
            return null;
        if (name.contains("banner"))
            return null;

        // === Wood type replacements ===
        if (state.is(BlockTags.LOGS)) {
            if (name.contains("stripped")) {
                return (name.contains("_wood") || name.contains("hyphae"))
                        ? Blocks.STRIPPED_CHERRY_WOOD
                        : Blocks.STRIPPED_CHERRY_LOG;
            }
            return (name.contains("_wood") || name.contains("hyphae"))
                    ? Blocks.CHERRY_WOOD
                    : Blocks.CHERRY_LOG;
        }
        if (state.is(BlockTags.PLANKS) || name.equals("bamboo_mosaic"))
            return Blocks.CHERRY_PLANKS;
        if (state.is(BlockTags.LEAVES))
            return Blocks.CHERRY_LEAVES;
        if (state.is(BlockTags.SAPLINGS))
            return Blocks.CHERRY_SAPLING;
        if (state.is(BlockTags.WOODEN_DOORS))
            return Blocks.CHERRY_DOOR;
        if (state.is(BlockTags.WOODEN_TRAPDOORS))
            return Blocks.CHERRY_TRAPDOOR;
        if (state.is(BlockTags.WOODEN_STAIRS) || name.equals("bamboo_mosaic_stairs"))
            return Blocks.CHERRY_STAIRS;
        if (state.is(BlockTags.WOODEN_SLABS) || name.equals("bamboo_mosaic_slab"))
            return Blocks.CHERRY_SLAB;
        if (state.is(BlockTags.WOODEN_FENCES))
            return Blocks.CHERRY_FENCE;
        if (state.is(BlockTags.FENCE_GATES))
            return Blocks.CHERRY_FENCE_GATE;
        if (state.is(BlockTags.WOODEN_BUTTONS))
            return Blocks.CHERRY_BUTTON;
        if (state.is(BlockTags.WOODEN_PRESSURE_PLATES))
            return Blocks.CHERRY_PRESSURE_PLATE;

        // Signs (check most specific first)
        if (name.endsWith("_wall_hanging_sign"))
            return Blocks.CHERRY_WALL_HANGING_SIGN;
        if (name.endsWith("_hanging_sign"))
            return Blocks.CHERRY_HANGING_SIGN;
        if (name.endsWith("_wall_sign"))
            return Blocks.CHERRY_WALL_SIGN;
        if (name.endsWith("_sign") && !name.equals("sign"))
            return Blocks.CHERRY_SIGN;

        // === Color type replacements ===
        if (state.is(BlockTags.WOOL))
            return Blocks.PINK_WOOL;
        if (state.is(BlockTags.WOOL_CARPETS))
            return Blocks.PINK_CARPET;
        if (state.is(BlockTags.CANDLES))
            return Blocks.PINK_CANDLE;
        if (state.is(BlockTags.SHULKER_BOXES))
            return Blocks.PINK_SHULKER_BOX;
        if (state.is(BlockTags.BEDS))
            return Blocks.PINK_BED;

        if (name.endsWith("_stained_glass_pane"))
            return Blocks.PINK_STAINED_GLASS_PANE;
        if (name.endsWith("_stained_glass"))
            return Blocks.PINK_STAINED_GLASS;
        if (block == Blocks.GLASS)
            return Blocks.PINK_STAINED_GLASS;
        if (block == Blocks.GLASS_PANE)
            return Blocks.PINK_STAINED_GLASS_PANE;
        if (name.endsWith("_concrete_powder"))
            return Blocks.PINK_CONCRETE_POWDER;
        if (name.endsWith("_concrete") && !name.equals("concrete"))
            return Blocks.PINK_CONCRETE;
        if (name.endsWith("_glazed_terracotta"))
            return Blocks.PINK_GLAZED_TERRACOTTA;
        if (name.endsWith("_terracotta") && !name.equals("terracotta"))
            return Blocks.PINK_TERRACOTTA;

        return null;
    }

    private BlockState copyMatchingProperties(BlockState oldState, Block newBlock) {
        BlockState newState = newBlock.defaultBlockState();
        for (Property<?> prop : oldState.getProperties()) {
            if (newState.hasProperty(prop)) {
                newState = applyProperty(newState, oldState, prop);
            }
        }
        if (newBlock == Blocks.CHERRY_LEAVES && newState.hasProperty(LeavesBlock.PERSISTENT)) {
            newState = newState.setValue(LeavesBlock.PERSISTENT, true);
        }
        return newState;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(
            BlockState target, BlockState source, Property<T> prop) {
        T value = source.getValue(prop);
        if (prop.getPossibleValues().contains(value)) {
            return target.setValue(prop, value);
        }
        return target;
    }

    // ======================== CHERRY PARTICLES ========================

    private void spawnCherryParticles(ServerLevel level, Entity entity) {
        if (entity == null)
            return;
        double cx = entity.getX();
        double cy = entity.getY() + 1.5;
        double cz = entity.getZ();
        
        double hRange = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
        double vRange = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
        
        // Default volume: hRadius=2 (4 width), vHalf=10 (20 height) → 4×20×4=320
        double defaultVolume = 320.0;
        double currentVolume = (hRange * 2) * (vRange * 2) * (hRange * 2);
        double densityMultiplier = (currentVolume / defaultVolume) * 0.4;
        
        // Calculate number of particles to spawn
        int particlesToSpawn = 0;
        double fractionalPart = densityMultiplier / 3; // Default is 1/3 chance
        particlesToSpawn += (int) fractionalPart;
        if (level.random.nextDouble() < (fractionalPart - (int) fractionalPart)) {
            particlesToSpawn += 1;
        }
        
        for (int i = 0; i < particlesToSpawn; i++) {
            double x = cx + (level.random.nextDouble() - 0.5) * (hRange * 2);
            double y = cy + (level.random.nextDouble() - 0.5) * (vRange * 2);
            double z = cz + (level.random.nextDouble() - 0.5) * (hRange * 2);
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ======================== ENTITY TRANSFORMATION (SHEEP & BOATS)
    // ========================

    private void updateEntities(ServerLevel level, EntityMaid maid, Player owner) {
        Set<UUID> inRange = new HashSet<>();
        if (maid != null)
            collectEntitiesInRange(level, maid, inRange);
        if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == level) {
            collectEntitiesInRange(level, owner, inRange);
        }

        for (UUID uuid : new ArrayList<>(savedSheepColors.keySet())) {
            if (!inRange.contains(uuid)) {
                Entity e = level.getEntity(uuid);
                if (e instanceof Sheep sheep) {
                    sheep.setColor(savedSheepColors.get(uuid));
                }
                savedSheepColors.remove(uuid);
            }
        }
        for (UUID uuid : new ArrayList<>(savedBoatTypes.keySet())) {
            if (!inRange.contains(uuid)) {
                Entity e = level.getEntity(uuid);
                if (e instanceof Boat boat) {
                    boat.setVariant(savedBoatTypes.get(uuid));
                }
                savedBoatTypes.remove(uuid);
            }
        }

        for (UUID uuid : inRange) {
            Entity e = level.getEntity(uuid);
            if (e instanceof Sheep sheep && !savedSheepColors.containsKey(uuid)
                    && sheep.getColor() != DyeColor.PINK) {
                savedSheepColors.put(uuid, sheep.getColor());
                sheep.setColor(DyeColor.PINK);
            }
            if (e instanceof Boat boat && !savedBoatTypes.containsKey(uuid)
                    && boat.getVariant() != Boat.Type.CHERRY) {
                savedBoatTypes.put(uuid, boat.getVariant());
                boat.setVariant(Boat.Type.CHERRY);
            }
        }

        // Discard any pink petals item entities inside the domain to prevent farming.
        Entity center = maid != null ? maid : owner;
        if (center != null) {
            for (Entity e : level.getEntities(center,
                    center.getBoundingBox().inflate(HORIZONTAL_RADIUS + 1, VERTICAL_HALF, HORIZONTAL_RADIUS + 1))) {
                if (e instanceof net.minecraft.world.entity.item.ItemEntity item
                        && item.getItem().is(net.minecraft.world.item.Items.PINK_PETALS)) {
                    item.discard();
                }
            }
        }
    }

    private void collectEntitiesInRange(ServerLevel level, Entity source, Set<UUID> result) {
        for (Entity e : level.getEntities(source,
                source.getBoundingBox().inflate(HORIZONTAL_RADIUS + 1, VERTICAL_HALF, HORIZONTAL_RADIUS + 1))) {
            if (e instanceof Sheep || e instanceof Boat) {
                result.add(e.getUUID());
            }
        }
    }

    // ======================== PLANT PROTECTION ========================

    private boolean isProtectedPlant(BlockState state) {
        if (state.is(BlockTags.REPLACEABLE_BY_TREES))
            return true;
        if (state.is(BlockTags.FLOWERS))
            return true;
        if (state.is(BlockTags.CROPS))
            return true;
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.BAMBOO) || state.is(Blocks.KELP)
                || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.SugarCaneBlock;
    }

    // ======================== DIMENSION RULES ========================

    private boolean isDomainExpansionActive(ServerLevel level) {
        return !level.getEntitiesOfClass(DomainExpansionEntity.class,
                this.getBoundingBox().inflate(500)).isEmpty();
    }

    public boolean isUsingDimensionRules() {
        if (this.level().isClientSide)
            return true;
        // Roll bypass chance — if it hits, rules don't apply this check
        int bypassChance = Config.CHERRY_DOMAIN_RULES_BYPASS_CHANCE.get();
        if (bypassChance > 0 && this.level().random.nextInt(100) < bypassChance) {
            return false;
        }
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

    // ======================== MAID LIGHT ========================

    private void applyMaidLight(ServerLevel serverLevel, EntityMaid maid) {
        UUID maidId = getMaidId();
        if (maid == null) {
            if (maidId != null) {
                BlockPos lastP = Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.get(maidId);
                if (lastP != null && serverLevel.getBlockState(lastP).is(Blocks.LIGHT)) {
                    serverLevel.setBlockAndUpdate(lastP, Blocks.AIR.defaultBlockState());
                    Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.remove(maidId);
                }
            }
            return;
        }
        UUID ownerId = getOwnerId();
        if (ownerId == null)
            return;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
        PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(ownerId);
        if (!(settings.isMaidEmitLight() || Config.MAID_EMIT_LIGHT.get())) {
            BlockPos lastP = Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.get(maid.getUUID());
            if (lastP != null && serverLevel.getBlockState(lastP).is(Blocks.LIGHT)) {
                serverLevel.setBlockAndUpdate(lastP, Blocks.AIR.defaultBlockState());
                Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.remove(maid.getUUID());
            }
            return;
        }

        BlockPos newLightPos = maid.blockPosition().above();
        BlockPos lastLightPos = Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.get(maid.getUUID());

        BlockState atNew = serverLevel.getBlockState(newLightPos);
        if (atNew.isAir() || atNew.is(Blocks.LIGHT)) {
            serverLevel.setBlockAndUpdate(newLightPos, Blocks.LIGHT.defaultBlockState());
            Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.put(maid.getUUID(), newLightPos);
        }
        if (lastLightPos != null && !lastLightPos.equals(newLightPos)
                && serverLevel.getBlockState(lastLightPos).is(Blocks.LIGHT)) {
            serverLevel.setBlockAndUpdate(lastLightPos, Blocks.AIR.defaultBlockState());
        }
    }

    // ======================== ENTITY EFFECTS ========================

    private void applyEffects() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        UUID ownerId = getOwnerId();
        if (ownerId == null)
            return;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
        PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(ownerId);
        
        int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
        int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();

        for (Entity e : serverLevel.getEntities(this, this.getBoundingBox().inflate(hRadius, vHalf, hRadius))) {
            double dx = e.getX() - this.getX();
            double dy = e.getY() - this.getY();
            double dz = e.getZ() - this.getZ();
            if (Math.abs(dx) <= hRadius && Math.abs(dy) <= vHalf && Math.abs(dz) <= hRadius) {
                if (!Touhoulittlemaidpersonaldimension.isAllowed(e, ownerId, serverLevel, settings)) {
                    if (Config.REMOVE_BLOCKED_ENTITIES.get() && !(e instanceof Player) && !(e instanceof EntityMaid)) {
                        e.discard();
                    } else {
                        double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                        double distance = hRadius + 5.0;
                        double targetX = this.getX() + Math.cos(angle) * distance;
                        double targetZ = this.getZ() + Math.sin(angle) * distance;
                        int safeY = Touhoulittlemaidpersonaldimension.findSafeSurfaceY(serverLevel, (int) targetX, (int) targetZ);
                        if (safeY > serverLevel.getMinBuildHeight()) {
                            e.teleportTo(targetX, safeY, targetZ);
                        } else {
                            e.teleportTo(targetX, e.getY(), targetZ);
                        }
                    }
                } else {
                    if (e instanceof Player player && (settings.isDisableHunger() || Config.DISABLE_HUNGER.get())) {
                        player.getFoodData().setFoodLevel(20);
                    }
                    if (e instanceof LivingEntity living
                            && (settings.isNaturalHealing() || Config.NATURAL_HEALING.get())
                            && living.getHealth() < living.getMaxHealth()) {
                        living.heal(1.0f);
                    }
                    if (isUsingEntityProtection()) {
                        if (e instanceof Player player) {
                            if (player.getUUID().equals(ownerId)) {
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, true));
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false, true));
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, true));
                            } else {
                                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
                                player.addEffect(
                                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false, true));
                            }
                        } else if (e instanceof EntityMaid maid) {
                            maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, true));
                            maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false, true));
                            maid.addEffect(
                                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, true));
                        } else if (e instanceof LivingEntity living) {
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
