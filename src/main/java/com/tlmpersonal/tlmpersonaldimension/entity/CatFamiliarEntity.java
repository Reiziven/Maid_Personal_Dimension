package com.tlmpersonal.tlmpersonaldimension.entity;

import com.github.tartaricacid.touhoulittlemaid.block.BlockMaidBed;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.CatLieOnBedGoal;
import net.minecraft.world.entity.ai.goal.CatSitOnBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class CatFamiliarEntity extends Cat {
    private static final EntityDataAccessor<Optional<UUID>> MAID_ID = SynchedEntityData
            .defineId(CatFamiliarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> EFFECT_COOLDOWN = SynchedEntityData
            .defineId(CatFamiliarEntity.class, EntityDataSerializers.INT);

    private static final int FOLLOW_DISTANCE = 10;
    private static final int TELEPORT_DISTANCE = 24;
    private static final int ATTACK_TELEPORT_RANGE = 3; // 3 blocks
    private static final int ATTACK_TELEPORT_COOLDOWN = 20; // 1 second in ticks
    
    private int attackTeleportCooldown = 0;
    private static final int FOLLOW_NUDGE_INTERVAL = 15;
    private static final int EFFECT_CHECK_INTERVAL = 20; // 1 second
    private static final int HARMFUL_EFFECT_CLEANUP_INTERVAL = 100; // 5 seconds
    private static final int HOSTILE_DETECT_RANGE = 128;
    private static final int HOSTILE_DETECT_INTERVAL = 100; // check every 5 seconds
    private static final int HOSTILE_GLOW_DURATION = 600; // 30 seconds in ticks
    private static final int HOSTILE_REDETECT_COOLDOWN = 1200; // 1 minute before same mob reported again

    private int lastHarmfulEffectCleanup = 0;
    private int hostileDetectTimer = 0;
    private int followNudgeTimer = 0;
    // Track last report game-time per mob UUID — re-detect after 1 minute cooldown
    private final java.util.Map<UUID, Long> reportedHostiles = new java.util.HashMap<>();
    
    // New fields for cat-maid interaction
    public long lastInteractionTime = 0;
    // Absolute game-time when the next interaction is allowed (computed once on end)
    public long nextInteractionTime = 0;
    private int interactionTicks = 0;
    private boolean isInteracting = false;

    public CatFamiliarEntity(EntityType<? extends Cat> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // Clear default cat goals to prevent conflicts
        this.goalSelector.getAvailableGoals().clear();
        this.targetSelector.getAvailableGoals().clear();

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SleepWithMaidGoal(this));
        this.goalSelector.addGoal(2, new CatFamiliarFollowOwnerGoal(this, 1.2D, FOLLOW_DISTANCE, 2.0F));
        this.goalSelector.addGoal(5, new CatFamiliarLieOnBedGoal(this, Cat.WALK_SPEED_MOD, 8));
        this.goalSelector.addGoal(7, new CatFamiliarSitOnBlockGoal(this, Cat.WALK_SPEED_MOD));
        this.goalSelector.addGoal(8, new InteractWithMaidGoal(this)); // Low priority
        this.goalSelector.addGoal(10, new RandomStrollGoal(this, 0.6D));

        if (Config.CAT_FAMILIAR_CAN_ATTACK.get()) {
              this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.4D, true));
            this.targetSelector.addGoal(1, new DefendSelfGoal(this));
            this.targetSelector.addGoal(2, new AttackMaidTargetGoal(this));
            this.targetSelector.addGoal(3, new DefendMaidGoal(this));
            if (Config.CAT_FAMILIAR_ATTACKS_PLAYER_TARGETS.get()) {
                this.targetSelector.addGoal(4, new AttackOwnerTargetGoal(this));
                this.targetSelector.addGoal(5, new DefendOwnerGoal(this));
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 128.0) // Set to larger range for attacking, FollowOwnerGoal uses startDistance
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MAID_ID, Optional.empty());
        builder.define(EFFECT_COOLDOWN, 0);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType spawnReason, net.minecraft.world.entity.SpawnGroupData spawnData) {
        // Set cat variant to all black
        net.minecraft.core.Holder<net.minecraft.world.entity.animal.CatVariant> allBlackVariant =
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CAT_VARIANT)
                        .getHolderOrThrow(net.minecraft.world.entity.animal.CatVariant.ALL_BLACK);
        this.setVariant(allBlackVariant);
        this.setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
    }

    public void setMaidId(UUID maidId) {
        this.entityData.set(MAID_ID, Optional.of(maidId));

        // Set the maid as the owner so follow goal works
        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            EntityMaid maid = getMaidEntity(serverLevel);
            if (maid != null) {
                this.setOwnerUUID(maid.getUUID());
                this.setTame(true, true);
            }
        }
    }

    public UUID getMaidId() {
        return this.entityData.get(MAID_ID).orElse(null);
    }

    @Override
    public LivingEntity getOwner() {
        if (this.level() instanceof ServerLevel serverLevel) {
            EntityMaid maid = getMaidEntity(serverLevel);
            if (maid != null) {
                return maid;
            }
        }
        return super.getOwner();
    }

    boolean isNearMaidForIdleBehavior() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        EntityMaid maid = getMaidEntity(serverLevel);
        return maid != null && this.distanceToSqr(maid) <= (double) FOLLOW_DISTANCE * FOLLOW_DISTANCE;
    }

    boolean isLinkedMaidSleeping() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        EntityMaid maid = getMaidEntity(serverLevel);
        return maid != null && maid.isSleeping();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("MaidId"))
            setMaidId(compound.getUUID("MaidId"));
        if (compound.contains("LastInteractionTime"))
            this.lastInteractionTime = compound.getLong("LastInteractionTime");
        if (compound.contains("NextInteractionTime"))
            this.nextInteractionTime = compound.getLong("NextInteractionTime");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        UUID maidId = getMaidId();
        if (maidId != null)
            compound.putUUID("MaidId", maidId);
        compound.putLong("LastInteractionTime", this.lastInteractionTime);
        compound.putLong("NextInteractionTime", this.nextInteractionTime);
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        UUID maidId = getMaidId();
        
        // Handle attack teleport cooldown
        if (attackTeleportCooldown > 0) {
            attackTeleportCooldown--;
        }
        
        // Try to teleport to target if attacking and far (if config is enabled)
        LivingEntity target = this.getTarget();
        boolean hasTarget = target != null && target.isAlive();
        boolean ignoreFollowRange = Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get();
        if (Config.CAT_FAMILIAR_TELEPORTS_TO_TARGET.get() && hasTarget) {
            if (attackTeleportCooldown <= 0) {
                double distSqToTarget = this.distanceToSqr(target);
                // Check if we ignore follow range OR target is within follow range
                boolean shouldAttackTarget = ignoreFollowRange 
                        || distSqToTarget <= (double)FOLLOW_DISTANCE * FOLLOW_DISTANCE;
                // If ignoreFollowRange is true, no need to check FOLLOW_DISTANCE, just check if more than ATTACK_TELEPORT_RANGE
                if (shouldAttackTarget && distSqToTarget > ATTACK_TELEPORT_RANGE * ATTACK_TELEPORT_RANGE) {
                    teleportToTarget(target, serverLevel);
                }
            }
        }

        if (maidId != null) {
            EntityMaid maid = getMaidEntity(serverLevel);

            if (maid == null || !maid.isAlive()) {
                this.discard();
                return;
            }

            // Duplicate guard — if another cat for the same maid already exists, discard this one
            for (CatFamiliarEntity other : serverLevel.getEntitiesOfClass(CatFamiliarEntity.class,
                    this.getBoundingBox().inflate(256), e -> e != this && maidId.equals(e.getMaidId()))) {
                this.discard();
                return;
            }

            // Sync attributes with maid
            syncAttributesWithMaid(maid);

            // Cat always has Cat Reflexes on itself
            if (!this.hasEffect(com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.CAT_REFLEXES_EFFECT)) {
                this.addEffect(new MobEffectInstance(
                        com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.CAT_REFLEXES_EFFECT,
                        1200, 0, false, false, false));
            }

            // Teleport logic: if ignoring follow range → prioritize attacking, else normal
            if (!maid.isSleeping()) {
                // If ignoring follow range, don't teleport back while attacking
                boolean shouldTeleportToMaid = !ignoreFollowRange || !hasTarget;
                
                double distanceSqToMaid = this.distanceToSqr(maid);

                if (shouldTeleportToMaid) {
                    if (distanceSqToMaid > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                        Vec3 oldPos = this.position();
                        spawnWitchParticles(oldPos, serverLevel);
                        this.teleportTo(maid.getX(), maid.getY(), maid.getZ());
                        spawnWitchParticles(this.position(), serverLevel);
                        this.navigation.stop();
                    } else if (distanceSqToMaid > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
                        followNudgeTimer++;
                        if (followNudgeTimer >= FOLLOW_NUDGE_INTERVAL && this.getNavigation().isDone()) {
                            this.getNavigation().moveTo(maid, 1.2D);
                            followNudgeTimer = 0;
                        }
                    } else {
                        followNudgeTimer = 0;
                    }
                }
            }

            // Clear sleep pose when no longer standing on a bed
            if (this.isLying() && !isStandingOnBed()) {
                this.setLying(false);
            }

            // Apply beneficial effects with cooldown
            if (Config.CAT_FAMILIAR_EFFECT_COOLDOWN.get()) {
                int cooldown = this.entityData.get(EFFECT_COOLDOWN);
                if (cooldown > 100) {
                    this.entityData.set(EFFECT_COOLDOWN, cooldown - 1);
                } else {
                    applyBeneficialEffects(serverLevel, maid);
                    this.entityData.set(EFFECT_COOLDOWN, EFFECT_CHECK_INTERVAL);
                }
            } else {
                applyBeneficialEffects(serverLevel, maid);
            }

            // Detect hostile mobs periodically
            if (Config.CAT_FAMILIAR_DETECT_HOSTILES.get()) {
                hostileDetectTimer++;
                if (hostileDetectTimer >= HOSTILE_DETECT_INTERVAL) {
                    detectAndReportHostiles(serverLevel, maid);
                    hostileDetectTimer = 0;
                }
            }
        }
    }
    
    private void teleportToTarget(LivingEntity target, ServerLevel level) {
        Vec3 oldPos = this.position();
        // Find a safe spot near the target
        Vec3 targetPos = target.position();
        double angle = level.random.nextDouble() * 2 * Math.PI;
        double distance = 1.5; // 1.5 blocks from target
        double tx = targetPos.x + Math.cos(angle) * distance;
        double ty = targetPos.y;
        double tz = targetPos.z + Math.sin(angle) * distance;
        
        // Find safe Y
        BlockPos.MutableBlockPos safePos = new BlockPos.MutableBlockPos((int)tx, (int)ty, (int)tz);
        int safeY = findSafeY(safePos, level);
        if (safeY > level.getMinBuildHeight()) {
            spawnWitchParticles(oldPos, level);
            this.teleportTo(tx, safeY, tz);
            this.navigation.stop();
            spawnWitchParticles(this.position(), level);
            attackTeleportCooldown = ATTACK_TELEPORT_COOLDOWN;
        }
    }
    
    public int findSafeY(BlockPos.MutableBlockPos pos, Level level) {
        int y = pos.getY();
        // Search down and up
        for (int offset = 0; offset <= 10; offset++) {
            // Check up
            pos.setY(y + offset);
            if (isSafe(pos, level)) {
                return pos.getY();
            }
            // Check down
            if (offset > 0) {
                pos.setY(y - offset);
                if (isSafe(pos, level)) {
                    return pos.getY();
                }
            }
        }
        return level.getMinBuildHeight() - 1; // No safe spot
    }
    
    private boolean isSafe(BlockPos pos, Level level) {
        BlockPos head = pos.above();
        BlockPos ground = pos.below();
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(head);
        BlockState groundState = level.getBlockState(ground);
        // Feet and head must be passable (air or non-solid like snow layers)
        boolean feetClear = !feetState.isSolid() || feetState.isAir();
        boolean headClear = !headState.isSolid() || headState.isAir();
        // Ground must have a solid top face — covers full blocks, snow, slabs, etc.
        boolean groundStandsOn = groundState.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP);
        return feetClear && headClear && groundStandsOn;
    }
    
    public static void spawnWitchParticles(Vec3 pos, Level level) {
        if (!Config.CAT_FAMILIAR_PARTICLES.get()) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            // Exactly matching your example
            serverLevel.sendParticles(ParticleTypes.WITCH, 
                pos.x, pos.y + 0.15, pos.z, 
                2, 0.25, 0.05, 0.25, 0.0);
        }
    }

    public void stopNavigation() {
        this.navigation.stop();
    }

    @Override
    public void setTarget(LivingEntity pTarget) {
        // Never target the maid!
        if (pTarget != null) {
            EntityMaid maid = this.getMaidEntity((ServerLevel) this.level());
            if (maid != null && pTarget == maid) {
                return;
            }
        }
        super.setTarget(pTarget);
    }

    private void syncAttributesWithMaid(EntityMaid maid) {
        // Sync attack damage if config enabled
        if (Config.CAT_FAMILIAR_MIRROR_ATTACK.get()) {
            double maidDamage = maid.getAttributeValue(Attributes.ATTACK_DAMAGE);
            // To mirror total, we need to set base value to maid's total value
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(maidDamage);
        }

        // Sync max health if config enabled
        if (Config.CAT_FAMILIAR_MIRROR_HEALTH.get()) {
            double maidMaxHealth = maid.getAttributeValue(Attributes.MAX_HEALTH);
            double currentMaxHealth = this.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            if (currentMaxHealth != maidMaxHealth) {
                float currentHealth = this.getHealth();
                // Update max health without touching current health
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maidMaxHealth);
                // Clamp current health to new max in case it somehow exceeds it
                this.setHealth(Math.min(currentHealth, this.getMaxHealth()));
            }
        }

        // Sync armor (defense) and toughness if config enabled
        if (Config.CAT_FAMILIAR_MIRROR_DEFENCE.get()) {
            double maidArmor = maid.getAttributeValue(Attributes.ARMOR);
            this.getAttribute(Attributes.ARMOR).setBaseValue(maidArmor);

            double maidArmorToughness = maid.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(maidArmorToughness);
        }
    }

    private void applyBeneficialEffects(ServerLevel serverLevel, EntityMaid maid) {
        Player owner = null;
        if (maid.getOwnerUUID() != null) {
            owner = serverLevel.getPlayerByUUID(maid.getOwnerUUID());
        }

        // Check if owner is nearby (within 32 blocks)
        boolean ownerNearby = owner != null && owner.distanceToSqr(maid) <= 32 * 32;

        // ALWAYS extend beneficial effect durations by 15%
        extendBeneficialEffectDurations(maid);
        if (ownerNearby)
            extendBeneficialEffectDurations(owner);

        // Cat Reflexes is applied via onMaidOrOwnerHurt event, not here

        // Luck applied periodically (always on with cooldown system)
        applyLuck(maid);
        if (ownerNearby)
            applyLuck(owner);

        // Emergency: Redirect attacker to cat when maid/owner is low health (25% or less)
        float maidHealthPercentage = maid.getHealth() / maid.getMaxHealth();
        if (maidHealthPercentage <= 0.25f) {
            redirectAttackerToCat(maid);
        }
        if (ownerNearby) {
            float ownerHealthPercentage = owner.getHealth() / owner.getMaxHealth();
            if (ownerHealthPercentage <= 0.25f) {
                redirectAttackerToCat(owner);
            }
        }

        // Falling prevention — apply Feline Grace based on fall distance
        if (!maid.onGround() && maid.getDeltaMovement().y < 0 && maid.fallDistance > 2.0f) {
            applySlowFalling(maid);
        }
        if (ownerNearby && !owner.onGround() && owner.getDeltaMovement().y < 0 && owner.fallDistance > 2.0f) {
            applySlowFalling(owner);
        }
    }


    private void applyLuck(LivingEntity entity) {
        if (!entity.hasEffect(MobEffects.LUCK)) {
            entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 2400, 4, false, false, false)); // 2 minutes
        }
    }

    private void redirectAttackerToCat(LivingEntity victim) {
        LivingEntity attacker = victim.getLastHurtByMob();
        if (attacker != null && attacker instanceof Mob mob) {
            // Make the attacker target the cat instead
            mob.setTarget(this);
            // Clear the victim from the attacker's memory
            mob.setLastHurtByMob(null);
        }
    }

    private void applySlowFalling(LivingEntity entity) {
        if (!entity.hasEffect(com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.FELINE_GRACE_EFFECT)) {
            entity.addEffect(new MobEffectInstance(
                    com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.FELINE_GRACE_EFFECT,
                    600, 0, false, false, false)); // 30 seconds
        }
    }

    private void extendBeneficialEffectDurations(LivingEntity entity) {
        // Create a copy of the effects list to avoid ConcurrentModificationException
        java.util.List<MobEffectInstance> effectsCopy = new java.util.ArrayList<>(entity.getActiveEffects());

        for (MobEffectInstance effect : effectsCopy) {
            if (isBeneficialEffect(effect.getEffect().value())) {
                int newDuration = (int) (effect.getDuration() * 1.15); // 15% extension
                entity.removeEffect(effect.getEffect());
                entity.addEffect(new MobEffectInstance(effect.getEffect(), newDuration, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }
    }

    private boolean isBeneficialEffect(net.minecraft.world.effect.MobEffect effect) {
        // Explicitly exclude effects with their own fixed duration managed separately
        if (effect == com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.CAT_REFLEXES_EFFECT.get())
            return false;
        if (effect == com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.FELINE_GRACE_EFFECT.get())
            return false;
        return effect == MobEffects.LUCK
                || effect == MobEffects.MOVEMENT_SPEED || effect == MobEffects.ABSORPTION
                || effect == MobEffects.DAMAGE_RESISTANCE
                || effect == MobEffects.FIRE_RESISTANCE || effect == MobEffects.WATER_BREATHING;
    }

    private void detectAndReportHostiles(ServerLevel serverLevel, EntityMaid maid) {
        Player owner = maid.getOwnerUUID() != null ? serverLevel.getPlayerByUUID(maid.getOwnerUUID()) : null;
        if (owner == null) return;

        long now = serverLevel.getGameTime();

        // Clean up dead/gone mobs from the map
        reportedHostiles.entrySet().removeIf(entry -> {
            net.minecraft.world.entity.Entity e = serverLevel.getEntity(entry.getKey());
            return e == null || !e.isAlive();
        });

        java.util.List<Mob> hostiles = serverLevel.getEntitiesOfClass(
                Mob.class,
                this.getBoundingBox().inflate(HOSTILE_DETECT_RANGE),
                mob -> mob.isAlive()
                        && mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER);

        for (Mob mob : hostiles) {
            Long lastSeen = reportedHostiles.get(mob.getUUID());

            if (lastSeen == null) {
                // First time seeing this mob — register it silently, no highlight yet
                reportedHostiles.put(mob.getUUID(), now);
                continue;
            }

            long timeSinceFirstSeen = now - lastSeen;
            if (timeSinceFirstSeen < HOSTILE_REDETECT_COOLDOWN) {
                // Still within cooldown window, skip
                continue;
            }

            // Cooldown passed — mob has been lurking, report and reset timer
            reportedHostiles.put(mob.getUUID(), now);

            // Apply Glowing for 30 seconds
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, HOSTILE_GLOW_DURATION, 0, false, false, false));

            // Send whisper only if chat reporting is enabled
            if (Config.CAT_FAMILIAR_DETECT_HOSTILES_CHAT.get()) {
                int x = (int) mob.getX();
                int y = (int) mob.getY();
                int z = (int) mob.getZ();
                String mobName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                        .getKey(mob.getType()).toString();
                net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal(
                        "§d[Cat Familiar] §7Detected §c" + mobName + "§7 at §e" + x + ", " + y + ", " + z);
                owner.sendSystemMessage(msg);
            }
        }
    }

    private void cleanHarmfulEffects(LivingEntity entity) {
        entity.removeEffect(MobEffects.POISON);
        entity.removeEffect(MobEffects.WITHER);
        entity.removeEffect(MobEffects.BLINDNESS);
        entity.removeEffect(MobEffects.WEAKNESS);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            applyBadLuckToAttacker(attacker);
        }
        return super.hurt(source, amount);
    }

    private void applyBadLuckToAttacker(LivingEntity attacker) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        EntityMaid maid = getMaidEntity(serverLevel);
        if (maid != null && attacker == maid) {
            return;
        }

        if (maid != null && maid.getOwnerUUID() != null && attacker instanceof Player player) {
            if (player.getUUID().equals(maid.getOwnerUUID())) {
                return;
            }
        }

        var badLuck = com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.BAD_LUCK_EFFECT;
        if (!attacker.hasEffect(badLuck)) {
            attacker.addEffect(new MobEffectInstance(badLuck, 1200, 0, false, true, true));
        }
    }

    EntityMaid getMaidEntity(ServerLevel serverLevel) {
        UUID maidId = getMaidId();
        if (maidId == null)
            return null;

        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(maidId);
        if (entity instanceof EntityMaid maid && maid.isAlive()) {
            return maid;
        }
        return null;
    }

    private boolean isStandingOnBed() {
        BlockPos pos = this.blockPosition();
        BlockState state = this.level().getBlockState(pos);
        if (state.is(BlockTags.BEDS) || isMaidBed(state)) {
            return true;
        }
        BlockState below = this.level().getBlockState(pos.below());
        return below.is(BlockTags.BEDS) || isMaidBed(below);
    }

    private static boolean isMaidBed(BlockState state) {
        return state.getBlock() instanceof BlockMaidBed;
    }

    private static BlockPos getOtherBedPart(BlockPos sleepingPos, BlockState bedState) {
        if (!(bedState.getBlock() instanceof BlockMaidBed)) {
            return null;
        }

        Direction facing = bedState.getValue(HorizontalDirectionalBlock.FACING);
        BedPart part = bedState.getValue(BlockMaidBed.PART);
        return part == BedPart.HEAD ? sleepingPos.relative(facing.getOpposite()) : sleepingPos.relative(facing);
    }

    private static class CatFamiliarFollowOwnerGoal extends FollowOwnerGoal {
        private final CatFamiliarEntity cat;

        CatFamiliarFollowOwnerGoal(CatFamiliarEntity cat, double speed, float startDistance, float stopDistance) {
            super(cat, speed, startDistance, stopDistance);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = cat.getTarget();
            // Don't follow if we have a target and we ignore follow range
            boolean shouldNotFollow = Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get()
                    && target != null && target.isAlive();
            return !cat.isLinkedMaidSleeping() && !shouldNotFollow && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = cat.getTarget();
            boolean shouldNotFollow = Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get()
                    && target != null && target.isAlive();
            return !cat.isLinkedMaidSleeping() && !shouldNotFollow && super.canContinueToUse();
        }
    }

    private static class SleepWithMaidGoal extends MoveToBlockGoal {
        private final CatFamiliarEntity cat;
        private EntityMaid maid;

        SleepWithMaidGoal(CatFamiliarEntity cat) {
            super(cat, Cat.WALK_SPEED_MOD, 8);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (!cat.isTame() || cat.isOrderedToSit() || cat.level().isClientSide) {
                return false;
            }

            maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null || !maid.isSleeping() || cat.distanceToSqr(maid) > 100.0) {
                return false;
            }

            BlockPos sleepingPos = maid.getSleepingPos().orElse(null);
            if (sleepingPos == null || !isMaidBed(cat.level().getBlockState(sleepingPos))) {
                return false;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return maid != null && maid.isAlive() && maid.isSleeping()
                    && blockPos != null && isValidTarget(cat.level(), blockPos);
        }

        @Override
        protected boolean findNearestBlock() {
            BlockPos sleepingPos = maid.getSleepingPos().orElse(null);
            if (sleepingPos == null) {
                return false;
            }

            BlockState bedState = cat.level().getBlockState(sleepingPos);
            BlockPos otherPart = getOtherBedPart(sleepingPos, bedState);
            if (otherPart != null && isValidTarget(cat.level(), otherPart)) {
                blockPos = otherPart;
                return true;
            }

            if (isValidTarget(cat.level(), sleepingPos)) {
                blockPos = sleepingPos;
                return true;
            }

            return false;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            if (!isMaidBed(level.getBlockState(pos)) || !level.isEmptyBlock(pos.above())) {
                return false;
            }

            for (Cat other : cat.level().getEntitiesOfClass(Cat.class, new AABB(pos), Cat::isLying)) {
                if (other != cat) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void start() {
            cat.setInSittingPose(false);
            super.start();
        }

        @Override
        public void stop() {
            super.stop();
            cat.setLying(false);
            maid = null;
        }

        @Override
        public void tick() {
            super.tick();
            cat.setInSittingPose(false);
            if (!isReachedTarget()) {
                cat.setLying(false);
            } else if (!cat.isLying()) {
                cat.getNavigation().stop();
                cat.setLying(true);
            }
        }
    }

    private static class CatFamiliarLieOnBedGoal extends CatLieOnBedGoal {
        private final CatFamiliarEntity cat;

        CatFamiliarLieOnBedGoal(CatFamiliarEntity cat, double speed, int range) {
            super(cat, speed, range);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            return !cat.isLinkedMaidSleeping() && cat.isNearMaidForIdleBehavior() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !cat.isLinkedMaidSleeping() && cat.isNearMaidForIdleBehavior() && super.canContinueToUse();
        }
    }

    private static class CatFamiliarSitOnBlockGoal extends CatSitOnBlockGoal {
        private final CatFamiliarEntity familiar;

        CatFamiliarSitOnBlockGoal(CatFamiliarEntity cat, double speed) {
            super(cat, speed);
            this.familiar = cat;
        }

        @Override
        public boolean canUse() {
            return !familiar.isLinkedMaidSleeping() && familiar.isNearMaidForIdleBehavior() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !familiar.isLinkedMaidSleeping() && familiar.isNearMaidForIdleBehavior() && super.canContinueToUse();
        }
    }

    private static class DefendSelfGoal extends HurtByTargetGoal {
        private final CatFamiliarEntity cat;

        DefendSelfGoal(CatFamiliarEntity cat) {
            super(cat);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }

            LivingEntity attacker = cat.getLastHurtByMob();
            if (attacker == null) {
                return false;
            }

              if (!Config.CAT_FAMILIAR_CAN_ATTACK.get()) {
                 return false;
            }

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid != null && attacker == maid) {
                return false;
            }

            if (maid != null && maid.getOwnerUUID() != null && attacker instanceof Player player) {
                if (player.getUUID().equals(maid.getOwnerUUID())) {
                    return false;
                }
            }

            double distSq = cat.distanceToSqr(attacker);
            return Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() || distSq < (double) FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }
    }

    // Custom goal to attack the maid's target
    private static class AttackMaidTargetGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final CatFamiliarEntity cat;
        private LivingEntity maidTarget;
        private int timestamp;

        public AttackMaidTargetGoal(CatFamiliarEntity cat) {
            super(cat, false);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (cat.level().isClientSide)
                return false;

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null)
                return false;

            maidTarget = maid.getTarget();
            if (maidTarget == null)
                return false;

            // Don't attack the maid or its owner
            if (maidTarget == maid)
                return false;

            if (maid.getOwnerUUID() != null && maidTarget instanceof Player player) {
                if (player.getUUID().equals(maid.getOwnerUUID()))
                    return false;
            }

            double distSq = cat.distanceToSqr(maidTarget);
            return Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() || distSq < (double) FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }

        @Override
        public void start() {
            cat.setTarget(maidTarget);
            timestamp = cat.tickCount;
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            if (cat.level().isClientSide)
                return false;

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null)
                return false;

            LivingEntity currentMaidTarget = maid.getTarget();

            // Continue if the maid still has the same target
            if (currentMaidTarget == maidTarget && maidTarget != null && maidTarget.isAlive()) {
                double distSq = cat.distanceToSqr(maidTarget);
                // For continue use, a little extra range (like 12 instead of 10) so it doesn't stop immediately
                double allowedDist = Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() ? 128 : FOLLOW_DISTANCE + 2;
                return distSq < allowedDist * allowedDist;
            }

            return false;
        }
    }

    // Custom goal to defend the maid from attackers
    private static class DefendMaidGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final CatFamiliarEntity cat;
        private LivingEntity attacker;
        private int timestamp;

        public DefendMaidGoal(CatFamiliarEntity cat) {
            super(cat, false);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (cat.level().isClientSide)
                return false;

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null)
                return false;

            attacker = maid.getLastHurtByMob();
            if (attacker == null)
                return false;

            // Check if the attack was recent (within last 5 seconds)
            int timeSinceAttack = maid.tickCount - maid.getLastHurtByMobTimestamp();
            if (timeSinceAttack > 100)
                return false;

            // Don't attack the owner
            if (maid.getOwnerUUID() != null && attacker instanceof Player player) {
                if (player.getUUID().equals(maid.getOwnerUUID()))
                    return false;
            }

            double distSq = cat.distanceToSqr(attacker);
            return Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() || distSq < (double) FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }

        @Override
        public void start() {
            cat.setTarget(attacker);
            timestamp = cat.tickCount;
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            if (attacker == null || !attacker.isAlive())
                return false;

            if (cat.level().isClientSide)
                return false;

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null)
                return false;

            // Continue for a limited time even after maid is no longer being attacked
            int timeElapsed = cat.tickCount - timestamp;
            if (timeElapsed > 200) // 10 seconds
                return false;

            double distSq = cat.distanceToSqr(attacker);
            // For continue use, a little extra range (like 12 instead of 10) so it doesn't stop immediately
            double allowedDist = Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() ? 128 : FOLLOW_DISTANCE + 2;
            return distSq < allowedDist * allowedDist;
        }
    }

    // Custom goal to attack the owner's target
    private static class AttackOwnerTargetGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final CatFamiliarEntity cat;
        private LivingEntity ownerTarget;
        private int timestamp;

        public AttackOwnerTargetGoal(CatFamiliarEntity cat) {
            super(cat, false);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (cat.level().isClientSide)
                return false;

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null || maid.getOwnerUUID() == null)
                return false;

            Player owner = ((ServerLevel) cat.level()).getPlayerByUUID(maid.getOwnerUUID());
            if (owner == null)
                return false;

            ownerTarget = owner.getLastHurtMob();
            if (ownerTarget == null || !ownerTarget.isAlive())
                return false;

            // Don't attack the maid or the owner
            if (ownerTarget == maid || ownerTarget == owner)
                return false;

            double distSq = cat.distanceToSqr(ownerTarget);
            return Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() || distSq < (double) FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }

        @Override
        public void start() {
            cat.setTarget(ownerTarget);
            timestamp = cat.tickCount;
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            if (cat.level().isClientSide)
                return false;

            if (ownerTarget == null || !ownerTarget.isAlive())
                return false;

            // Continue for a limited time
            int timeElapsed = cat.tickCount - timestamp;
            if (timeElapsed > 200) // 10 seconds
                return false;

            double distSq = cat.distanceToSqr(ownerTarget);
            double allowedDist = Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() ? 128 : FOLLOW_DISTANCE + 2;
            return distSq < allowedDist * allowedDist;
        }
    }

    // Custom goal to defend the owner from attackers
    private static class DefendOwnerGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final CatFamiliarEntity cat;
        private LivingEntity attacker;
        private int timestamp;

        public DefendOwnerGoal(CatFamiliarEntity cat) {
            super(cat, false);
            this.cat = cat;
        }

        @Override
        public boolean canUse() {
            if (cat.level().isClientSide)
                return false;

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null || maid.getOwnerUUID() == null)
                return false;

            Player owner = ((ServerLevel) cat.level()).getPlayerByUUID(maid.getOwnerUUID());
            if (owner == null)
                return false;

            attacker = owner.getLastHurtByMob();
            if (attacker == null)
                return false;

            // Check if the attack was recent (within last 5 seconds)
            int timeSinceAttack = owner.tickCount - owner.getLastHurtByMobTimestamp();
            if (timeSinceAttack > 100)
                return false;

            // Don't attack the maid or the owner
            if (attacker == maid || attacker == owner)
                return false;

            double distSq = cat.distanceToSqr(attacker);
            return Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() || distSq < (double) FOLLOW_DISTANCE * FOLLOW_DISTANCE;
        }

        @Override
        public void start() {
            cat.setTarget(attacker);
            timestamp = cat.tickCount;
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            if (attacker == null || !attacker.isAlive())
                return false;

            if (cat.level().isClientSide)
                return false;

            EntityMaid maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null || maid.getOwnerUUID() == null)
                return false;

            Player owner = ((ServerLevel) cat.level()).getPlayerByUUID(maid.getOwnerUUID());
            if (owner == null)
                return false;

            // Continue for a limited time even after owner is no longer being attacked
            int timeElapsed = cat.tickCount - timestamp;
            if (timeElapsed > 200) // 10 seconds
                return false;

            double distSq = cat.distanceToSqr(attacker);
            double allowedDist = Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get() ? 128 : FOLLOW_DISTANCE + 2;
            return distSq < allowedDist * allowedDist;
        }
    }

    // New goal for cat to interact with maid
    private static class InteractWithMaidGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final CatFamiliarEntity cat;
        private EntityMaid maid;

        public InteractWithMaidGoal(CatFamiliarEntity cat) {
            this.cat = cat;
            this.setFlags(EnumSet.of(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE, net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cat.level().isClientSide)
                return false;

            maid = cat.getMaidEntity((ServerLevel) cat.level());
            if (maid == null || !maid.isAlive())
                return false;

            // Check cooldown using pre-computed next interaction time
            long currentTime = cat.level().getGameTime();
            if (currentTime < cat.nextInteractionTime)
                return false;

            // Don't interact if maid is busy
            if (maid.getTarget() != null || maid.isSleeping() || maid.isBegging())
                return false;

            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return maid != null && maid.isAlive() && cat.isInteracting
                    && maid.getTarget() == null && !maid.isSleeping();
        }

        @Override
        public void start() {
            cat.isInteracting = true;
            cat.interactionTicks = 0;
        }

        @Override
        public void stop() {
            cat.isInteracting = false;
            cat.interactionTicks = 0;
            cat.getNavigation().stop();
            if (maid != null) {
                // Release maid brain lock
                maid.setBegging(false);
                maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
                maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH);
                maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.LOOK_TARGET);
            }
        }

        @Override
        public void tick() {
            if (maid == null)
                return;

            // --- Cat look at maid (normal, cat uses classic goal-based look) ---
            cat.getLookControl().setLookAt(maid, 10.0F, cat.getMaxHeadXRot());

            double distSq = cat.distanceToSqr(maid);

            if (distSq > 2.0D) {
                // Approaching: walk toward maid slowly, suppress maid walk/look while closing in
                cat.getNavigation().moveTo(maid, 0.6D);
                suppressMaidMovement();
            } else {
                // Close enough: proper interaction phase
                cat.interactionTicks++;
                cat.getNavigation().stop();

                // Lock maid in place and make her look at cat every tick via Brain memory
                suppressMaidMovement();
                lockMaidLookAtCat();

                // Trigger the maid's begging pose — the only built-in arm animation
                // that works without an item in hand and doesn't require isSwingingArms
                maid.setBegging(true);

                // Heart particles around cat
                if (cat.interactionTicks % 5 == 0 && cat.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            cat.getX() + (cat.random.nextDouble() - 0.5) * 0.5,
                            cat.getY() + 0.8 + (cat.random.nextDouble() - 0.5) * 0.3,
                            cat.getZ() + (cat.random.nextDouble() - 0.5) * 0.5,
                            1, 0, 0, 0, 0.05);
                }

                // Cat purr sound
                if (cat.interactionTicks % 20 == 0) {
                    cat.playSound(net.minecraft.sounds.SoundEvents.CAT_PURR, 1.0F, 1.0F);
                }

                // End after ~3 seconds
                if (cat.interactionTicks >= 60) {
                    cat.heal(cat.getMaxHealth() * 0.5f);
                    maid.getFavorabilityManager().add(1);
                    cat.lastInteractionTime = cat.level().getGameTime();
                    // Roll next interaction time once, so canUse() doesn't re-roll every tick
                    cat.nextInteractionTime = cat.lastInteractionTime + 2400 + cat.random.nextInt(15600);
                    cat.isInteracting = false;
                }
            }
        }

        /**
         * Erases the maid's WALK_TARGET and PATH brain memories every tick so the
         * Brain's MoveToTargetSink / MaidFollowOwnerTask cannot steer her while
         * the cat is interacting.
         */
        private void suppressMaidMovement() {
            net.minecraft.world.entity.ai.Brain<?> brain = maid.getBrain();
            brain.eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH);
            maid.getNavigation().stop();
        }

        /**
         * Sets the maid's LOOK_TARGET brain memory to the cat every tick.
         * This feeds directly into LookAtTargetSink (a core Brain task) so the
         * maid's look controller locks onto the cat without jitter — no direct
         * lookAt() call needed.
         */
        private void lockMaidLookAtCat() {
            maid.getBrain().setMemory(
                    net.minecraft.world.entity.ai.memory.MemoryModuleType.LOOK_TARGET,
                    new net.minecraft.world.entity.ai.behavior.EntityTracker(cat, true));
        }
    }
}
