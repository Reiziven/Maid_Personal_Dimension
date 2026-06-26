package com.tlmpersonal.tlmpersonaldimension.entity;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class CatFamiliarEntity extends Cat {
    private static final EntityDataAccessor<Optional<UUID>> MAID_ID = SynchedEntityData
            .defineId(CatFamiliarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> EFFECT_COOLDOWN = SynchedEntityData
            .defineId(CatFamiliarEntity.class, EntityDataSerializers.INT);

    private static final int FOLLOW_DISTANCE = 8;
    private static final int TELEPORT_DISTANCE = 24;
    private static final int EFFECT_CHECK_INTERVAL = 20; // 1 second
    private static final int HARMFUL_EFFECT_CLEANUP_INTERVAL = 100; // 5 seconds
    private static final int HOSTILE_DETECT_RANGE = 128;
    private static final int HOSTILE_DETECT_INTERVAL = 100; // check every 5 seconds
    private static final int HOSTILE_GLOW_DURATION = 600; // 30 seconds in ticks
    private static final int HOSTILE_REDETECT_COOLDOWN = 1200; // 1 minute before same mob reported again

    private int lastHarmfulEffectCleanup = 0;
    private int hostileDetectTimer = 0;
    // Track last report game-time per mob UUID — re-detect after 1 minute cooldown
    private final java.util.Map<UUID, Long> reportedHostiles = new java.util.HashMap<>();

    public CatFamiliarEntity(EntityType<? extends Cat> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // Clear default cat goals to prevent conflicts
        this.goalSelector.getAvailableGoals().clear();
        this.targetSelector.getAvailableGoals().clear();
        
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FollowOwnerGoal(this, 1.2D, FOLLOW_DISTANCE, 2.0F));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.4D, true));
        
        if (Config.CAT_FAMILIAR_ATTACKS_ENTITIES.get()) {
            // Custom target goal that attacks what the maid attacks
            this.targetSelector.addGoal(1, new AttackMaidTargetGoal(this));
            this.targetSelector.addGoal(2, new DefendMaidGoal(this));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_DISTANCE)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
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
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("MaidId"))
            setMaidId(compound.getUUID("MaidId"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        UUID maidId = getMaidId();
        if (maidId != null)
            compound.putUUID("MaidId", maidId);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide)
            return;

        ServerLevel serverLevel = (ServerLevel) this.level();
        UUID maidId = getMaidId();

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

            // Follow and teleport to maid if too far
            double distanceSq = this.distanceToSqr(maid);

            if (distanceSq > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                this.teleportTo(maid.getX(), maid.getY(), maid.getZ());
                this.navigation.stop();
            } else if (distanceSq > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
                // Move towards maid if beyond follow distance but within teleport range
                this.getNavigation().moveTo(maid, 1.2D);
            }

            // Apply beneficial effects with cooldown
            if (Config.CAT_FAMILIAR_EFFECT_COOLDOWN.get()) {
                int cooldown = this.entityData.get(EFFECT_COOLDOWN);
                if (cooldown > 0) {
                    this.entityData.set(EFFECT_COOLDOWN, cooldown - 1);
                } else {
                    applyBeneficialEffects(serverLevel, maid);
                    this.entityData.set(EFFECT_COOLDOWN, EFFECT_CHECK_INTERVAL);
                }
            } else {
                applyBeneficialEffects(serverLevel, maid);
            }

            // Clean harmful effects periodically
            lastHarmfulEffectCleanup++;
            if (lastHarmfulEffectCleanup >= HARMFUL_EFFECT_CLEANUP_INTERVAL) {
                cleanHarmfulEffects(maid);
                lastHarmfulEffectCleanup = 0;
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

    private void syncAttributesWithMaid(EntityMaid maid) {
        // Sync attack damage
        double maidDamage = maid.getAttributeValue(Attributes.ATTACK_DAMAGE);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(maidDamage);
        
        // Sync max health
        double maidMaxHealth = maid.getAttributeValue(Attributes.MAX_HEALTH);
        double currentMaxHealth = this.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
        if (currentMaxHealth != maidMaxHealth) {
            // Calculate health percentage before changing max health
            float healthPercentage = this.getHealth() / this.getMaxHealth();
            
            // Set new max health
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maidMaxHealth);
            
            // Restore health percentage
            this.setHealth(this.getMaxHealth() * healthPercentage);
        }
        
        // Sync armor (defense)
        double maidArmor = maid.getAttributeValue(Attributes.ARMOR);
        this.getAttribute(Attributes.ARMOR).setBaseValue(maidArmor);
        
        // Sync armor toughness
        double maidArmorToughness = maid.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(maidArmorToughness);
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
        return super.hurt(source, amount);
    }

    private EntityMaid getMaidEntity(ServerLevel serverLevel) {
        UUID maidId = getMaidId();
        if (maidId == null)
            return null;

        net.minecraft.world.entity.Entity entity = serverLevel.getEntity(maidId);
        if (entity instanceof EntityMaid maid && maid.isAlive()) {
            return maid;
        }
        return null;
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

            return cat.distanceToSqr(maidTarget) < 32 * 32;
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
                return cat.distanceToSqr(maidTarget) < 48 * 48;
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

            return cat.distanceToSqr(attacker) < 32 * 32;
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

            return cat.distanceToSqr(attacker) < 48 * 48;
        }
    }
}
