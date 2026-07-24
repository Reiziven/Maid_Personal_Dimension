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
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.core.Direction;
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

    /** Immediately restores all converted blocks and removes this entity. */
    public void restoreAndDiscard() {
        if (!this.level().isClientSide) {
            restoreDomain();
        }
        this.discard();
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

        if (remaining % 20 == 0) {
            applyEffects();
        }

        applyMaidLight(serverLevel, maid);

        if (Config.CHERRY_DOMAIN_ENABLE_CHERRYFICATION.get()) {
            spawnCherryParticles(serverLevel, maid);
            if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == serverLevel) {
                spawnCherryParticles(serverLevel, owner);
            }
        } else {
            spawnTornadoParticles(serverLevel, maid);
            if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == serverLevel) {
                spawnTornadoParticles(serverLevel, owner);
            }
        }

        if (remaining % 20 == 0) {
            // If any DomainExpansion is active, pause block transformation and restore
            // all cherry blocks immediately so DE snapshots clean originals.
            if (isDomainExpansionActive(serverLevel)) {
                if (!savedBlocks.isEmpty()) {
                    restoreDomain();
                }
            } else if (Config.CHERRY_DOMAIN_ENABLE_CHERRYFICATION.get()) {
                updateBlocks(serverLevel, maid, owner);
            }
            if (Config.CHERRY_DOMAIN_ENABLE_CHERRYFICATION.get()) {
                updateEntities(serverLevel, maid, owner);
            }
        }
    }

    // ======================== BLOCK REPLACEMENT LOGIC ========================

    private void updateBlocks(ServerLevel level, EntityMaid maid, Player owner) {
        List<BlockPos> toRestore = new ArrayList<>();
        for (BlockPos pos : savedBlocks.keySet()) {
            boolean inRange = false;
            if (maid != null && isPosInRange(pos, maid)) {
                inRange = true;
            } else if (Config.CHERRY_DOMAIN_AFFECTS_OWNER.get() && owner != null && owner.level() == level && isPosInRange(pos, owner)) {
                inRange = true;
            }

            if (!inRange || !Config.CHERRY_DOMAIN_GENERATE_PINK_PETALS.get()) {
                int count = outOfRangeCounter.getOrDefault(pos, 0) + 1;
                if (count >= 6 || !Config.CHERRY_DOMAIN_GENERATE_PINK_PETALS.get()) {
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

    private boolean isPosInRange(BlockPos pos, Entity entity) {
        BlockPos center = entity.blockPosition();
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= HORIZONTAL_RADIUS * HORIZONTAL_RADIUS &&
               pos.getY() >= center.getY() - VERTICAL_HALF &&
               pos.getY() <= center.getY() + VERTICAL_HALF - 1;
    }

    private void transformAroundEntity(Entity entity, ServerLevel level) {
        BlockPos center = entity.blockPosition();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        // 1) Floor petals: 5x5 at feet level, only on grass blocks
        if (Config.CHERRY_DOMAIN_GENERATE_PINK_PETALS.get()) {
            BlockPos.MutableBlockPos petalPos = new BlockPos.MutableBlockPos();
            for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
                for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                    if (x * x + z * z > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) continue;
                    petalPos.setWithOffset(center, x, 0, z);

                    if (savedBlocks.containsKey(petalPos))
                        continue;

                    mPos.setWithOffset(center, x, -1, z);
                    BlockState floorState = level.getBlockState(mPos);
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

                    // Deterministic coordinate-based hash to scatter petals (approx 10% chance)
                    int hash = petalPos.getX() * 31337 + petalPos.getY() * 104395301 + petalPos.getZ() * 83492791;
                    if (Math.abs(hash) % 100 > 10)
                        continue;

                    // Amount 2 to 4, based on hash (as requested)
                    int amount = (Math.abs(hash / 100) % 3) + 2;
                    
                    // Facing direction based on hash (optimized without streams)
                    Direction[] directions = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                    Direction facing = directions[Math.abs(hash / 400) % 4];

                    BlockState petalState = Blocks.PINK_PETALS.defaultBlockState();
                    if (petalState.hasProperty(PinkPetalsBlock.AMOUNT)) {
                        petalState = petalState.setValue(PinkPetalsBlock.AMOUNT, amount);
                    }
                    if (petalState.hasProperty(PinkPetalsBlock.FACING)) {
                        petalState = petalState.setValue(PinkPetalsBlock.FACING, facing);
                    }

                    BlockPos immutablePetalPos = petalPos.immutable();
                    savedBlocks.put(immutablePetalPos, aboveState);
                    level.setBlock(immutablePetalPos, petalState, SET_BLOCK_FLAGS);
                }
            }
        }

        // 2) Block replacement: 5x5 XZ, 20Y vertical
        for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
            for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                if (x * x + z * z > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) continue;
                for (int y = -VERTICAL_HALF; y <= VERTICAL_HALF - 1; y++) {
                    mPos.setWithOffset(center, x, y, z);
                    if (savedBlocks.containsKey(mPos))
                        continue;

                    BlockState currentState = level.getBlockState(mPos);
                    Block replacement = getCherryReplacementCached(currentState);
                    if (replacement != null) {
                        saveAndReplace(level, mPos.immutable(), currentState, replacement);
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

        // Clean up maid light block when domain ends (uses stored dimension, not just serverLevel)
        UUID maidId = getMaidId();
        if (maidId != null) {
            Touhoulittlemaidpersonaldimension.removeMaidLight(maidId, serverLevel.getServer());
        }
    }

    // ======================== CHERRY/PINK BLOCK MAPPING ========================

    private static final Map<Block, Block> REPLACEMENT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private Block getCherryReplacementCached(BlockState state) {
        Block block = state.getBlock();
        Block cached = REPLACEMENT_CACHE.get(block);
        if (cached != null) {
            return cached == Blocks.AIR ? null : cached;
        }
        Block replacement = getCherryReplacement(state);
        REPLACEMENT_CACHE.put(block, replacement == null ? Blocks.AIR : replacement);
        return replacement;
    }

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
        if (entity == null) return;

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();

        double hRange = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
        double vRange = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();

        // Inside ambient particles
        int insideParticles = (int) (hRange * 0.5);
        if (insideParticles < 1) insideParticles = 1;

        for (int i = 0; i < insideParticles; i++) {
            if (level.random.nextFloat() < 0.25f) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(level.random.nextDouble()) * hRange;
                double x = cx + Math.cos(angle) * r;
                double y = cy + (level.random.nextDouble() - 0.5) * (vRange * 2);
                double z = cz + Math.sin(angle) * r;

                level.sendParticles(com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.STRAIGHT_CHERRY_PARTICLE.get(), x, y, z, 1, 0.0, -0.06, 0.0, 0.01);
            }
        }

        spawnTornadoParticles(level, entity);
    }

    private void spawnTornadoParticles(ServerLevel level, Entity entity) {
        if (entity == null || !Config.CHERRY_DOMAIN_ENABLE_TORNADO.get()) return;

        double cx = entity.getX();
        double cy = entity.getY();
        double cz = entity.getZ();

        double hRange = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
        double vRange = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();

        // density 0.1–10.0: map to particlesPerArm and tick interval
        double density = Config.CHERRY_DOMAIN_TORNADO_DENSITY.get();
        int particlesPerArm = Math.max(1, (int) Math.round(density * 2.0));
        // At low density, skip ticks (density 0.5 = every 10 ticks, density 5.0+ = every tick)
        int interval = Math.max(1, (int) Math.round(6.0 - density));
        if (this.tickCount % interval != 0) return;

        int ticks = this.tickCount;
        double speed = 0.018;
        int numArms = 3;

        for (int slice = 0; slice < 3; slice++) {
            double timeOffset = (slice / 3.0) * (2 * Math.PI / numArms);

            for (int arm = 0; arm < numArms; arm++) {
                double armOffset = (Math.PI * 2.0 / numArms) * arm;

                for (int i = 0; i < particlesPerArm; i++) {
                    double yProgress = (double) i / particlesPerArm;
                    double heightTwist = yProgress * Math.PI * 4.0;
                    double baseAngle = (ticks * speed) + armOffset + heightTwist + timeOffset;
                    double angle = baseAngle + (level.random.nextDouble() - 0.5) * 0.2;

                    double cos = Math.cos(angle);
                    double sin = Math.sin(angle);
                    double currentRange = hRange + (level.random.nextDouble() - 0.5) * 0.3;

                    double x = cx + cos * currentRange;
                    double z = cz + sin * currentRange;
                    double y = (cy - vRange) + (yProgress * vRange * 2.0);
                    y += (level.random.nextDouble() - 0.5) * 0.4;

                    double xSpeed = -sin * 0.04;
                    double zSpeed = cos * 0.04;

                    level.sendParticles(com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.STRAIGHT_CHERRY_PARTICLE.get(),
                            x, y, z, 1, xSpeed, -0.02, zSpeed, 0.0);
                }
            }
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
                    double dx = e.getX() - center.getX();
                    double dz = e.getZ() - center.getZ();
                    if (dx * dx + dz * dz <= HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) {
                        item.discard();
                    }
                }
            }
        }
    }

    private void collectEntitiesInRange(ServerLevel level, Entity source, Set<UUID> result) {
        for (Entity e : level.getEntities(source,
                source.getBoundingBox().inflate(HORIZONTAL_RADIUS + 1, VERTICAL_HALF, HORIZONTAL_RADIUS + 1))) {
            if (e instanceof Sheep || e instanceof Boat) {
                double dx = e.getX() - source.getX();
                double dz = e.getZ() - source.getZ();
                if (dx * dx + dz * dz <= HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) {
                    result.add(e.getUUID());
                }
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
        return Config.CHERRY_DOMAIN_USE_DIMENSION_RULES.get();
    }

    public boolean isUsingEntityProtection() {
        if (this.level().isClientSide)
            return true;
        return Config.CHERRY_DOMAIN_USE_ENTITY_PROTECTION.get();
    }

    public boolean isUsingEntityFiltering() {
        if (this.level().isClientSide)
            return true;
        return Config.CHERRY_DOMAIN_USE_ENTITY_FILTERING.get();
    }

    // ======================== MAID LIGHT ========================

    private void applyMaidLight(ServerLevel serverLevel, EntityMaid maid) {
        UUID maidId = getMaidId();
        if (maid == null) {
            // Maid gone — clean up its light using the stored dimension, not just serverLevel
            if (maidId != null) {
                Touhoulittlemaidpersonaldimension.removeMaidLight(maidId, serverLevel.getServer());
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
            // Setting turned off — remove via helper so we hit the right level
            Touhoulittlemaidpersonaldimension.removeMaidLight(maid.getUUID(), serverLevel.getServer());
            return;
        }

        BlockPos newLightPos = maid.blockPosition().above();
        Touhoulittlemaidpersonaldimension.MaidLightEntry lastEntry =
                Touhoulittlemaidpersonaldimension.MAID_LIGHT_POSITIONS.get(maid.getUUID());
        BlockPos lastLightPos = lastEntry != null ? lastEntry.pos() : null;

        // Always remove old light first if maid has moved (hits the right level via entry)
        if (lastLightPos != null && !lastLightPos.equals(newLightPos)) {
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
            if (dx * dx + dz * dz <= hRadius * hRadius && Math.abs(dy) <= vHalf) {
                if (isUsingEntityFiltering() && !Touhoulittlemaidpersonaldimension.isAllowed(e, ownerId, serverLevel, settings)) {
                    if (Config.REMOVE_BLOCKED_ENTITIES.get() && !(e instanceof Player) && !(e instanceof EntityMaid)) {
                        e.discard();
                    } else {
                        Touhoulittlemaidpersonaldimension.expelFromDomain(e, serverLevel, this.getX(), this.getZ());
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
                    // Apply combat buffs/debuffs only when entity protection effects are enabled
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
