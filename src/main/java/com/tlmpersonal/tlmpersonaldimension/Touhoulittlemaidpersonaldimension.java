package com.tlmpersonal.tlmpersonaldimension;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.info.ServerCustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity;
import com.tlmpersonal.tlmpersonaldimension.inventory.PersonalDimensionMenu;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import com.mojang.logging.LogUtils;
import com.tlmpersonal.tlmpersonaldimension.accessor.MinecraftServerAccessor;
import com.tlmpersonal.tlmpersonaldimension.world.StructurePlacer;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionSettingsSyncPacket;
import com.tlmpersonal.tlmpersonaldimension.network.TeleporterCooldownSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod(Touhoulittlemaidpersonaldimension.MODID)
public class Touhoulittlemaidpersonaldimension {
    public static final String MODID = "touhoulittlemaidpersonaldimension";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MODID);
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> MOB_EFFECTS = DeferredRegister
            .create(Registries.MOB_EFFECT, MODID);
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.conditions.ICondition>> CONDITIONS = DeferredRegister
            .create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.conditions.ICondition>, com.mojang.serialization.MapCodec<com.tlmpersonal.tlmpersonaldimension.condition.BaubleCraftableCondition>> BAUBLE_CRAFTABLE_CONDITION = CONDITIONS
            .register("bauble_craftable",
                    () -> com.tlmpersonal.tlmpersonaldimension.condition.BaubleCraftableCondition.CODEC);

    private static final Map<ResourceKey<Level>, ConcurrentLinkedQueue<BlockPos>> PLACEMENT_QUEUE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> PROCESSED_CHUNKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<BlockPos>> GENERATED_ISLANDS = new HashMap<>();
    // Tracks the last block pos where each maid placed a light block, keyed by maid
    // UUID
    public static final Map<UUID, BlockPos> MAID_LIGHT_POSITIONS = new HashMap<>();

    public static final ResourceKey<Level> PERSONAL_DIMENSION_VOID_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MODID, "personal_dimension"));
    public static final ResourceKey<Level> PERSONAL_DIMENSION_NORMAL_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MODID, "personal_dimension_normal"));
    public static final ResourceKey<Level> PERSONAL_DIMENSION_CHERRY_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MODID, "personal_dimension_cherry"));

    public static ResourceKey<Level> getCurrentPersonalDimensionKey() {
        return switch (Config.DIMENSION_TYPE.get()) {
            case VOID -> PERSONAL_DIMENSION_VOID_KEY;
            case NORMAL -> PERSONAL_DIMENSION_NORMAL_KEY;
            case CHERRY -> PERSONAL_DIMENSION_CHERRY_KEY;
        };
    }

    public static boolean isOurDimension(ResourceKey<Level> dim) {
        if (dim == null)
            return false;
        String path = dim.location().getPath();
        return path.startsWith("personal_dimension") && dim.location().getNamespace().equals(MODID);
    }

    public static boolean isUnderDimensionRules(Entity entity) {
        if (entity == null || entity.level().isClientSide)
            return false;
        ServerLevel level = (ServerLevel) entity.level();
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : level.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                entity.getBoundingBox().inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
                if (aabb != null ? aabb.contains(entity.position())
                        : entity.position().distanceToSqr(domain.position()) <= 32 * 32) {
                    return true;
                }
            }
        }
        for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : level.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class,
                entity.getBoundingBox().inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
                int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                double dx = entity.getX() - domain.getX();
                double dy = entity.getY() - domain.getY();
                double dz = entity.getZ() - domain.getZ();
                if (Math.abs(dx) <= hRadius && Math.abs(dy) <= vHalf && Math.abs(dz) <= hRadius) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isUnderDimensionRules(Level level, BlockPos pos) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel))
            return false;
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class, new AABB(pos).inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
                if (aabb != null ? aabb.contains(pos.getX(), pos.getY(), pos.getZ())
                        : pos.distSqr(domain.blockPosition()) <= 32 * 32) {
                    return true;
                }
            }
        }
        for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class, new AABB(pos).inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
                int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                int dx = pos.getX() - domain.blockPosition().getX();
                int dy = pos.getY() - domain.blockPosition().getY();
                int dz = pos.getZ() - domain.blockPosition().getZ();
                if (Math.abs(dx) <= hRadius && Math.abs(dy) <= vHalf && Math.abs(dz) <= hRadius) {
                    return true;
                }
            }
        }
        return false;
    }

    public static UUID getOwnerUUIDFromPosition(ServerLevel level, double x, double z) {
        int gridX = (int) Math.floor((x - 0.5) / 10000.0);
        int gridZ = (int) Math.floor((z - 0.5) / 10000.0);
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(level);
        return savedData.getPlayerAtGrid(gridX, gridZ);
    }

    private static final Map<ResourceKey<Level>, UUID> DIMENSION_OWNER_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static UUID getOwnerUUIDFromDimensionKey(ResourceKey<Level> dim) {
        if (DIMENSION_OWNER_CACHE.containsKey(dim)) {
            return DIMENSION_OWNER_CACHE.get(dim);
        }
        String path = dim.location().getPath();
        try {
            String prefix = "personal_dimension_";
            if (path.startsWith(prefix)) {
                String uuidPart = path.substring(prefix.length());
                int lastUnderscore = uuidPart.lastIndexOf('_');
                if (lastUnderscore != -1) {
                    String uuidStrWithUnderscores = uuidPart.substring(0, lastUnderscore);
                    String uuidStr = uuidStrWithUnderscores.replace('_', '-');
                    UUID uuid = UUID.fromString(uuidStr);
                    DIMENSION_OWNER_CACHE.put(dim, uuid);
                    return uuid;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public record TeleportLocation(double x, double y, double z, float yRot, float xRot) {
    }

    public static final Map<UUID, Map<ResourceKey<Level>, TeleportLocation>> TELEPORT_HISTORY = new HashMap<>();

    public static final DeferredHolder<EntityType<?>, EntityType<com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity>> DOMAIN_EXPANSION_ENTITY = ENTITY_TYPES
            .register("domain_expansion",
                    () -> EntityType.Builder.<com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity>of(
                            com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(20)
                            .build("domain_expansion"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity>> CHERRY_DOMAIN_ENTITY = ENTITY_TYPES
            .register("cherry_domain",
                    () -> EntityType.Builder.<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity>of(
                            com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(20)
                            .build("cherry_domain"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity>> CAT_FAMILIAR_ENTITY = ENTITY_TYPES
            .register("cat_familiar",
                    () -> EntityType.Builder.<com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity>of(
                            com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 0.7f)
                            .clientTrackingRange(20)
                            .build("cat_familiar"));

    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect> CAT_REFLEXES_EFFECT = MOB_EFFECTS
            .register("cat_reflexes", com.tlmpersonal.tlmpersonaldimension.effect.CatReflexesEffect::new);

    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect> FELINE_GRACE_EFFECT = MOB_EFFECTS
            .register("feline_grace", com.tlmpersonal.tlmpersonaldimension.effect.FelineGraceEffect::new);

    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect> BAD_LUCK_EFFECT = MOB_EFFECTS
            .register("bad_luck", com.tlmpersonal.tlmpersonaldimension.effect.BadLuckEffect::new);

    public static final DeferredItem<MaidTeleporter> MAID_TELEPORTER = ITEMS.register("maid_teleporter",
            () -> new MaidTeleporter(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> TAB_ICON = ITEMS.register("tab_icon", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOMAIN_EXPANSION_BAUBLE = ITEMS.register("domain_expansion_bauble",
            () -> new com.tlmpersonal.tlmpersonaldimension.item.DomainExpansionBaubleItem(
                    new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CHERRY_DOMAIN_BAUBLE = ITEMS.register("cherry_domain_bauble",
            () -> new com.tlmpersonal.tlmpersonaldimension.item.CherryDomainBaubleItem(
                    new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CAT_FAMILIAR_BAUBLE = ITEMS.register("cat_familiar_bauble",
            () -> new com.tlmpersonal.tlmpersonaldimension.item.CatFamiliarBaubleItem(
                    new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> TETHERED_TELEPORT_BAUBLE = ITEMS.register("tethered_teleport_bauble",
            () -> new com.tlmpersonal.tlmpersonaldimension.item.TetheredTeleportBaubleItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PERSONAL_DIMENSION_TAB = CREATIVE_MODE_TABS
            .register("personal_dimension_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".personal_dimension_tab"))
                    .icon(() -> new ItemStack(TAB_ICON.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(MAID_TELEPORTER.get());
                        output.accept(DOMAIN_EXPANSION_BAUBLE.get());
                        output.accept(CHERRY_DOMAIN_BAUBLE.get());
                        output.accept(CAT_FAMILIAR_BAUBLE.get());
                        output.accept(TETHERED_TELEPORT_BAUBLE.get());
                    }).build());

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalDimensionMenu>> PERSONAL_DIMENSION_MENU = MENUS
            .register("personal_dimension_menu", () -> IMenuTypeExtension
                    .create((windowId, inv, data) -> new PersonalDimensionMenu(windowId, inv, data.readInt())));

    public Touhoulittlemaidpersonaldimension(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerEntityAttributes);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        CONDITIONS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new TouhoulittlemaidpersonaldimensionClient(modContainer);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket.TYPE,
                com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket.STREAM_CODEC,
                com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket::handle);
        registrar.playToServer(com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket.TYPE,
                com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket.STREAM_CODEC,
                com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket::handle);
        registrar.playToClient(PersonalDimensionSettingsSyncPacket.TYPE,
                PersonalDimensionSettingsSyncPacket.STREAM_CODEC, PersonalDimensionSettingsSyncPacket::handle);
        registrar.playToClient(TeleporterCooldownSyncPacket.TYPE, TeleporterCooldownSyncPacket.STREAM_CODEC,
                TeleporterCooldownSyncPacket::handle);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void registerEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(CAT_FAMILIAR_ENTITY.get(),
                com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAID_TELEPORTER);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        StructurePlacer.resetForNewServer();
        PROCESSED_CHUNKS.clear();
        PLACEMENT_QUEUE.clear();
        GENERATED_ISLANDS.clear();
        DIMENSION_OWNER_CACHE.clear();
        MAIDS_TO_TELEPORT.clear();
        MAIDS_PENDING_TELEPORT.clear();
        PlayerDimensionManager.clearCache();
        PlayerDimensionManager.preloadPersistedPersonalDimensionState(event.getServer());
    }

    public static boolean isPlayerAllowed(Player player, UUID ownerId, ServerLevel level,
            PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        UUID finalOwnerId = ownerId;
        if (finalOwnerId == null && level != null) {
            finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
            if (finalOwnerId == null)
                finalOwnerId = getOwnerUUIDFromPosition(level, player.getX(), player.getZ());
        }
        if (finalOwnerId != null && player.getUUID().equals(finalOwnerId))
            return true;
        if (settings == null && finalOwnerId != null && level != null)
            settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
        if (settings != null) {
            String playerName = player.getGameProfile().getName();
            String playerUuidStr = player.getUUID().toString();
            for (String allowed : settings.getAllowedPlayers()) {
                if (allowed.equalsIgnoreCase(playerName) || allowed.equals(playerUuidStr))
                    return true;
            }
        }
        return !Config.PRIVATE_DIMENSION.get() || finalOwnerId == null;
    }

    /**
     * Check if an entity ID matches any pattern in the list (supports wildcards
     * like "minecraft:*")
     */
    private static boolean matchesAnyPattern(String entityId, java.util.Collection<? extends String> patterns) {
        for (String pattern : patterns) {
            // Direct match
            if (pattern.equals(entityId)) {
                return true;
            }
            // Wildcard match (e.g., "minecraft:*")
            if (pattern.endsWith(":*")) {
                String namespace = pattern.substring(0, pattern.length() - 2);
                if (entityId.startsWith(namespace + ":")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void saveEntityPosition(Entity entity, ResourceKey<Level> dimension) {
        UUID uuid = entity.getUUID();
        Map<ResourceKey<Level>, TeleportLocation> entityMap = TELEPORT_HISTORY.computeIfAbsent(uuid,
                k -> new HashMap<>());
        entityMap.put(dimension,
                new TeleportLocation(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot()));
    }

    public static TeleportLocation getEntityPosition(UUID uuid, ResourceKey<Level> dimension) {
        Map<ResourceKey<Level>, TeleportLocation> entityMap = TELEPORT_HISTORY.get(uuid);
        return entityMap != null ? entityMap.get(dimension) : null;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static void tryTriggerDomainExpansion(EntityMaid maid) {
        if (maid.level().isClientSide())
            return;
        if (maid.getFavorabilityManager().getLevel() < 3)
            return;

        // Check cooldown
        ServerLevel level = (ServerLevel) maid.level();
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                .get(level.getServer().getLevel(Level.OVERWORLD));
        PersonalDimensionSavedData.PlayerDimensionSettings settings = null;
        if (maid.getOwnerUUID() != null) {
            settings = savedData.getOrCreateSettings(maid.getOwnerUUID());
        }

        if (settings != null) {
            long currentTime = System.currentTimeMillis();
            long cooldownMs = Config.DOMAIN_EXPANSION_COOLDOWN_SECONDS.get() * 1000L;
            if (currentTime - settings.getLastDomainExpansionUse() < cooldownMs) {
                return;
            }
            settings.setLastDomainExpansionUse(currentTime);
            savedData.setDirty();
        }

        // Prevent spawning if maid already has an active domain
        if (level
                .getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                        maid.getBoundingBox().inflate(Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()))
                .stream().anyMatch(d -> maid.getUUID().equals(d.getMaidId()))) {
            return;
        }
        // Prevent overlapping domains from other maids
        BlockPos maidPos = new BlockPos(maid.blockPosition());
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity existing : level.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                new net.minecraft.world.phys.AABB(maidPos).inflate(Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()))) {
            if (existing.blockPosition().distSqr(maidPos) < (double) Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()
                    * Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()) {
                return;
            }
        }
        com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domainEntity = DOMAIN_EXPANSION_ENTITY.get()
                .create(level);
        if (domainEntity != null) {
            domainEntity.moveTo(maid.getX(), maid.getY() - 1, maid.getZ(), 0, 0);
            domainEntity.setMaidId(maid.getUUID());
            if (maid.getOwnerUUID() != null) {
                domainEntity.setOwnerId(maid.getOwnerUUID());
            }
            level.addFreshEntity(domainEntity);
        }
    }

    public static void tickCherryDomain(EntityMaid maid) {
        if (maid.level().isClientSide())
            return;
        ServerLevel level = (ServerLevel) maid.level();

        boolean canSustain = true;
        if (Config.CHERRY_DOMAIN_XP_COST_ENABLED.get() && maid.getOwnerUUID() != null) {
            net.minecraft.world.entity.player.Player owner = level.getServer().getPlayerList().getPlayer(maid.getOwnerUUID());
            if (owner != null) {
                int cost = Config.CHERRY_DOMAIN_XP_COST.get();
                if (cost > 0) {
                    int intervalTicks = Config.CHERRY_DOMAIN_XP_COST_INTERVAL_SECONDS.get() * 20;
                    if (owner.experienceLevel < cost) {
                        canSustain = false;
                    } else if (maid.tickCount % intervalTicks == 0) {
                        owner.giveExperienceLevels(-cost);
                    }
                }
            } else {
                canSustain = false;
            }
        }

        // Check if the maid already has a Cherry Domain aura
        Optional<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity> existing = level
                .getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class,
                        maid.getBoundingBox().inflate(10))
                .stream().filter(d -> maid.getUUID().equals(d.getMaidId())).findFirst();

        if (canSustain) {
            if (existing.isPresent()) {
                existing.get().resetTimeout();
            } else {
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domainEntity = CHERRY_DOMAIN_ENTITY.get()
                        .create(level);
                if (domainEntity != null) {
                    domainEntity.moveTo(maid.getX(), maid.getY(), maid.getZ(), 0, 0);
                    domainEntity.setMaidId(maid.getUUID());
                    if (maid.getOwnerUUID() != null) {
                        domainEntity.setOwnerId(maid.getOwnerUUID());
                    }
                    level.addFreshEntity(domainEntity);
                }
            }
        }
    }

    public static boolean isAllowed(Entity entity, UUID ownerId, ServerLevel level,
            PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        if (entity instanceof Player player)
            return isPlayerAllowed(player, ownerId, level, settings);
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String idString = entityId.toString();
        if (idString.equals("touhou_little_maid:maid"))
            return true;
        if (idString.equals("touhou_little_maid:chair"))
            return true;
        if (settings == null) {
            UUID finalOwnerId = ownerId;
            if (finalOwnerId == null && level != null) {
                finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
                if (finalOwnerId == null)
                    finalOwnerId = getOwnerUUIDFromPosition(level, entity.getX(), entity.getZ());
            }
            if (finalOwnerId != null && level != null)
                settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
        }
        if (settings != null) {
            if (matchesAnyPattern(idString, settings.getBlockedEntities())
                    || matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get()))
                return false;
            if ((settings.isDisableHostileEntities() || Config.DISABLE_HOSTILE_ENTITIES.get())
                    && entity.getType().getCategory() == MobCategory.MONSTER)
                return false;
        } else {
            if (matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get()))
                return false;
            if (Config.DISABLE_HOSTILE_ENTITIES.get() && entity.getType().getCategory() == MobCategory.MONSTER)
                return false;
        }
        if ((entity instanceof ItemEntity || entity instanceof Projectile)
                || (entity.getType().getCategory() == MobCategory.MISC && !(entity instanceof LivingEntity)))
            return true;
        if (settings != null && (settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get()
                || matchesAnyPattern(idString, settings.getAllowedEntities())
                || matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get())))
            return true;
        if (Config.PRIVATE_DIMENSION.get()) {
            if (settings == null)
                return false;
            return settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get()
                    || matchesAnyPattern(idString, settings.getAllowedEntities())
                    || matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get());
        }
        if (Config.ENTITY_WHITELIST_MODE.get()) {
            if (matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get()))
                return true;
            return Config.ALLOW_ALL_ENTITIES.get();
        }
        return Config.ALLOW_ALL_ENTITIES.get();
    }

    public static boolean isAllowed(EntityType<?> entityType, UUID ownerId, ServerLevel level,
            PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        if (entityType == EntityType.PLAYER)
            return true;
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        String idString = entityId.toString();
        if (idString.equals("touhou_little_maid:maid"))
            return true;
        if (idString.equals("touhou_little_maid:chair"))
            return true;
        if (settings == null) {
            UUID finalOwnerId = ownerId;
            if (finalOwnerId == null && level != null)
                finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
            if (finalOwnerId != null && level != null)
                settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
        }
        if (settings != null) {
            if (matchesAnyPattern(idString, settings.getBlockedEntities())
                    || matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get()))
                return false;
            if ((settings.isDisableHostileEntities() || Config.DISABLE_HOSTILE_ENTITIES.get())
                    && entityType.getCategory() == MobCategory.MONSTER)
                return false;
        } else {
            if (matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get()))
                return false;
            if (Config.DISABLE_HOSTILE_ENTITIES.get() && entityType.getCategory() == MobCategory.MONSTER)
                return false;
        }
        if ((entityType.getCategory() == MobCategory.MISC && !idString.equals("minecraft:bee")
                && !idString.equals("minecraft:villager")) || idString.equals("minecraft:item")
                || idString.equals("minecraft:experience_orb") || idString.equals("minecraft:arrow")
                || idString.equals("minecraft:snowball") || idString.equals("minecraft:egg")
                || idString.equals("minecraft:ender_pearl") || idString.equals("minecraft:splash_potion")
                || idString.equals("minecraft:lingering_potion") || idString.equals("minecraft:firework_rocket")
                || idString.equals("minecraft:fireball") || idString.equals("minecraft:small_fireball")
                || idString.equals("minecraft:dragon_fireball") || idString.equals("minecraft:wither_skull")
                || idString.equals("minecraft:llama_spit") || idString.equals("minecraft:shulker_bullet")
                || idString.equals("minecraft:trident"))
            return true;
        if (settings != null && (settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get()
                || matchesAnyPattern(idString, settings.getAllowedEntities())
                || matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get())))
            return true;
        if (Config.PRIVATE_DIMENSION.get()) {
            if (settings == null)
                return false;
            return settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get()
                    || matchesAnyPattern(idString, settings.getAllowedEntities())
                    || matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get());
        }
        if (Config.ENTITY_WHITELIST_MODE.get()) {
            if (matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get()))
                return true;
            return Config.ALLOW_ALL_ENTITIES.get();
        }
        return Config.ALLOW_ALL_ENTITIES.get();
    }

    public static boolean isBossEntity(Entity entity) {
        if (entity == null) return false;
        EntityType<?> entityType = entity.getType();
        TagKey<EntityType<?>> bossesTag = Tags.EntityTypes.BOSSES; // #c:bosses
        TagKey<EntityType<?>> neoforgeBossesTag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("neoforge", "bosses"));
        TagKey<EntityType<?>> forgeBossesTag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
        return entityType.is(bossesTag) || entityType.is(neoforgeBossesTag) || entityType.is(forgeBossesTag);
    }

    public static boolean isMaidTeleporterAllowed(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof Player) return false; // Don't allow teleporting players with shift-right-click
        
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String idString = entityId.toString();
        
        // Exclude bosses first if enabled
        if (Config.MAID_TELEPORTER_EXCLUDE_BOSSES.get() && isBossEntity(entity)) {
            return false;
        }
        
        // Check blacklist first (higher priority)
        if (matchesAnyPattern(idString, Config.MAID_TELEPORTER_BLOCKED_ENTITIES.get())) {
            return false;
        }
        
        // Check whitelist / allow all
        if (Config.MAID_TELEPORTER_ALLOW_ALL_ENTITIES.get()) {
            return true;
        }
        
        if (Config.MAID_TELEPORTER_ENTITY_WHITELIST_MODE.get()) {
            return matchesAnyPattern(idString, Config.MAID_TELEPORTER_ALLOWED_ENTITIES.get());
        }
        
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !isOurDimension(entity.level().dimension()))
            return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null)
            ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
        if (ownerId == null)
            return;
        if (!isAllowed(entity, ownerId, serverLevel, null)) {
            event.setCanceled(true);
            entity.discard();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !isOurDimension(entity.level().dimension()))
            return;
        if (event.getSpawnType() == MobSpawnType.CONVERSION)
            return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null)
            ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
        if (!isAllowed(entity, ownerId, serverLevel, null)) {
            event.setSpawnCancelled(true);
            entity.discard();
        }
    }

    public static void processRemoval(Entity entity) {
        if (entity == null)
            return;
        if (Config.REMOVE_BLOCKED_ENTITIES.get()) {
            if (entity instanceof ServerPlayer player) {
                ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    TeleportLocation savedPos = getEntityPosition(player.getUUID(), Level.OVERWORLD);
                    if (savedPos != null)
                        player.teleportTo(overworld, savedPos.x(), savedPos.y(), savedPos.z(), Set.of(),
                                savedPos.yRot(), savedPos.xRot());
                    else {
                        BlockPos spawn = overworld.getSharedSpawnPos();
                        player.teleportTo(overworld, (double) spawn.getX() + 0.5, (double) spawn.getY() + 1,
                                (double) spawn.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot());
                    }
                    player.sendSystemMessage(Component.literal("You are not allowed here!"));
                }
            } else {
                entity.discard();
            }
            return;
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            if (!isOurDimension(serverLevel.dimension())) {
                double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                double distance = 30.0;
                double targetX = entity.getX() + Math.cos(angle) * distance;
                double targetZ = entity.getZ() + Math.sin(angle) * distance;
                int safeY = findSafeSurfaceY(serverLevel, (int) targetX, (int) targetZ);
                if (safeY > serverLevel.getMinBuildHeight()) {
                    entity.teleportTo(targetX, safeY, targetZ);
                } else {
                    entity.teleportTo(targetX, entity.getY(), targetZ);
                }
                if (entity instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.literal("You were expelled!"));
                }
                return;
            }

            ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                TeleportLocation savedPos = getEntityPosition(entity.getUUID(), Level.OVERWORLD);
                if (savedPos == null) {
                    UUID fallbackId = null;
                    if (entity instanceof OwnableEntity ownable)
                        fallbackId = ownable.getOwnerUUID();
                    if (fallbackId == null)
                        fallbackId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
                    if (fallbackId == null)
                        fallbackId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
                    if (fallbackId != null)
                        savedPos = getEntityPosition(fallbackId, Level.OVERWORLD);
                }
                if (savedPos != null)
                    entity.teleportTo(overworld, savedPos.x(), savedPos.y(), savedPos.z(), Set.of(), savedPos.yRot(),
                            savedPos.xRot());
                else {
                    BlockPos spawn = overworld.getSharedSpawnPos();
                    entity.teleportTo(overworld, (double) spawn.getX() + 0.5, (double) spawn.getY() + 1,
                            (double) spawn.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot());
                }
                if (entity instanceof ServerPlayer player)
                    player.sendSystemMessage(Component.literal("You are not allowed here!"));
                return;
            }
        }
        entity.discard();
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                    .get(serverLevel.getServer().getLevel(Level.OVERWORLD));
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof EntityMaid maid && maid.getOwnerUUID() != null) {
                    savedData.getTrackedMaids().put(maid.getUUID(), new PersonalDimensionSavedData.MaidInfo(
                            maid.getOwnerUUID(), maid.level().dimension(), maid.getX(), maid.getY(), maid.getZ()));
                }
            }
            savedData.setDirty();

            // Only process the maid teleport queue from the overworld tick to avoid
            // processing the same queue multiple times per game tick (once per loaded
            // level).
            // This prevents the ghost-entity / freeze bug caused by repeated re-queuing.
            if (serverLevel.dimension().equals(Level.OVERWORLD) && !MAIDS_TO_TELEPORT.isEmpty()) {
                List<MaidTeleportData> retryList = new ArrayList<>();
                while (!MAIDS_TO_TELEPORT.isEmpty()) {
                    MaidTeleportData data = MAIDS_TO_TELEPORT.poll();
                    if (data == null)
                        continue;

                    // Drop stale entries that have exceeded the retry limit
                    if (data.retries() >= MAX_MAID_TELEPORT_RETRIES) {
                        MAIDS_PENDING_TELEPORT.remove(data.maidUuid());
                        continue;
                    }

                    ServerLevel targetLevel = serverLevel.getServer().getLevel(data.targetDim());
                    if (targetLevel == null) {
                        MAIDS_PENDING_TELEPORT.remove(data.maidUuid());
                        continue;
                    }

                    ServerPlayer targetOwner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUuid());
                    if (targetOwner == null) {
                        // Owner offline — defer until retry limit
                        retryList.add(new MaidTeleportData(data.maidUuid(), data.ownerUuid(), data.targetDim(),
                                data.retries() + 1));
                        continue;
                    }

                    // Owner has not yet arrived at the target dimension — wait
                    if (!targetOwner.level().dimension().equals(data.targetDim())) {
                        retryList.add(new MaidTeleportData(data.maidUuid(), data.ownerUuid(), data.targetDim(),
                                data.retries() + 1));
                        continue;
                    }

                    // Find the maid across all loaded levels
                    EntityMaid maid = null;
                    for (ServerLevel levelIter : serverLevel.getServer().getAllLevels()) {
                        Entity entity = levelIter.getEntity(data.maidUuid());
                        if (entity instanceof EntityMaid foundMaid) {
                            maid = foundMaid;
                            break;
                        }
                    }

                    if (maid == null) {
                        // Maid no longer exists (may have already been teleported or removed)
                        MAIDS_PENDING_TELEPORT.remove(data.maidUuid());
                        continue;
                    }

                    // Skip if the maid is already in the target dimension
                    if (maid.level().dimension().equals(data.targetDim())) {
                        MAIDS_PENDING_TELEPORT.remove(data.maidUuid());
                        continue;
                    }

                    // Perform the actual cross-dimension teleport.
                    // Entity.teleportTo(ServerLevel, ...) calls changeDimension() which correctly
                    // removes the entity from the source level and spawns it in the target level.
                    // We do NOT manually discard the old entity — changeDimension handles that.
                    maid.teleportTo(targetLevel, targetOwner.getX(), targetOwner.getY(), targetOwner.getZ(), Set.of(),
                            targetOwner.getYRot(), targetOwner.getXRot());
                    MAIDS_PENDING_TELEPORT.remove(data.maidUuid());
                }
                // Re-add entries that still need retrying
                MAIDS_TO_TELEPORT.addAll(retryList);
            }
        }
        if (!(level instanceof ServerLevel serverLevel) || !isOurDimension(level.dimension()))
            return;
        StructurePlacer.tryPlaceStructure(serverLevel);
        int spawnChance = Config.MAID_SPAWN_CHANCE.get();
        if (spawnChance > 0 && serverLevel.getGameTime() % 200 == 0) {
            String dimPath = serverLevel.dimension().location().getPath();
            if (dimPath.contains("normal") || dimPath.contains("cherry")) {
                if (serverLevel.random.nextInt(spawnChance) == 0 && !serverLevel.players().isEmpty()) {
                    Player player = serverLevel.players().iterator().next();
                    BlockPos playerPos = player.blockPosition();
                    int spawnX = playerPos.getX() + serverLevel.random.nextInt(64) - 32;
                    int spawnZ = playerPos.getZ() + serverLevel.random.nextInt(64) - 32;
                    int spawnY = findSafeSurfaceY(serverLevel, spawnX, spawnZ);
                    if (spawnY > serverLevel.getMinBuildHeight()) {
                        BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);
                        if (serverLevel.canSeeSky(spawnPos)
                                && !serverLevel.getFluidState(spawnPos.below()).isSource()) {
                            EntityMaid maid = InitEntities.MAID.get().create(serverLevel);
                            if (maid != null) {
                                if (Config.USE_YSM_MODELS.get()) {
                                    List<? extends String> modelIdsList = Config.YSM_MODEL_IDS.get();
                                    if (!modelIdsList.isEmpty()) {
                                        maid.setIsYsmModel(true);
                                        maid.setYsmModel(
                                                modelIdsList.get(serverLevel.random.nextInt(modelIdsList.size())), "",
                                                net.minecraft.network.chat.Component.literal(""));
                                    }
                                } else {
                                    int modelSize = ServerCustomPackLoader.SERVER_MAID_MODELS.getModelSize();
                                    if (modelSize > 0)
                                        ServerCustomPackLoader.SERVER_MAID_MODELS.getModelIdSet().stream()
                                                .skip(serverLevel.random.nextInt(modelSize)).findFirst()
                                                .ifPresent(maid::setModelId);
                                }
                                maid.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                                        MobSpawnType.NATURAL, null);
                                maid.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                        serverLevel.random.nextFloat() * 360.0F, 0.0F);
                                serverLevel.addFreshEntity(maid);
                            }
                        }
                    }
                }
            }
        }
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity.getY() < Config.FALL_PROTECTION_Y.get()) {
                BlockPos safePos = findNearestSafeSurface(serverLevel, (int) entity.getX(), (int) entity.getZ(), 16);
                if (safePos == null) {
                    TeleportLocation savedPos = getEntityPosition(entity.getUUID(), serverLevel.dimension());
                    if (savedPos != null) {
                        safePos = findNearestSafeSurface(serverLevel, (int) savedPos.x(), (int) savedPos.z(), 16);
                    }
                }
                if (safePos == null) {
                    safePos = findNearestSafeSurface(serverLevel, 0, 0, 16);
                }

                if (safePos != null) {
                    entity.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                } else {
                    BlockPos platformPos = new BlockPos(0, 100, 0);
                    serverLevel.setBlockAndUpdate(platformPos.below(),
                            net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
                    entity.teleportTo(0.5, 100, 0.5);
                }
                entity.fallDistance = 0;
            }
        }
        if ((serverLevel.getGameTime() + serverLevel.dimension().hashCode()) % 40 != 0)
            return;
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null && !serverLevel.players().isEmpty())
            ownerId = getOwnerUUIDFromPosition(serverLevel, serverLevel.players().iterator().next().getX(),
                    serverLevel.players().iterator().next().getZ());
        if (ownerId != null) {
            PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get(serverLevel)
                    .getOrCreateSettings(ownerId);
            List<Entity> toRemove = new ArrayList<>();
            Set<UUID> activeMaidUUIDs = new java.util.HashSet<>();
            for (Entity entity : serverLevel.getAllEntities()) {
                if (!isAllowed(entity, ownerId, serverLevel, settings)) {
                    toRemove.add(entity);
                    if (toRemove.size() > 100)
                        break;
                    continue;
                }
                if (entity instanceof Player player && (settings.isDisableHunger() || Config.DISABLE_HUNGER.get()))
                    player.getFoodData().setFoodLevel(20);
                if (entity instanceof LivingEntity living
                        && (settings.isNaturalHealing() || Config.NATURAL_HEALING.get())
                        && living.getHealth() < living.getMaxHealth())
                    living.heal(1.0f);
                if (entity instanceof EntityMaid maid && (settings.isMaidEmitLight() || Config.MAID_EMIT_LIGHT.get())) {
                    activeMaidUUIDs.add(maid.getUUID());
                    BlockPos newLightPos = maid.blockPosition().above();
                    BlockPos lastLightPos = MAID_LIGHT_POSITIONS.get(maid.getUUID());
                    net.minecraft.world.level.block.state.BlockState atNew = level.getBlockState(newLightPos);
                    if (atNew.isAir() || atNew.is(net.minecraft.world.level.block.Blocks.LIGHT)) {
                        level.setBlockAndUpdate(newLightPos,
                                net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState());
                        MAID_LIGHT_POSITIONS.put(maid.getUUID(), newLightPos);
                    }
                    if (lastLightPos != null && !lastLightPos.equals(newLightPos)
                            && level.getBlockState(lastLightPos).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
                        level.setBlockAndUpdate(lastLightPos,
                                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }
            // Clean up lights for maids that are no longer in this dimension
            MAID_LIGHT_POSITIONS.entrySet().removeIf(entry -> {
                if (!activeMaidUUIDs.contains(entry.getKey())) {
                    BlockPos lp = entry.getValue();
                    if (level.getBlockState(lp).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
                        level.setBlockAndUpdate(lp, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                    return true;
                }
                return false;
            });
            for (Entity entity : toRemove)
                Touhoulittlemaidpersonaldimension.processRemoval(entity);
            if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                if (settings.isLockDay())
                    serverLevel.setDayTime(settings.getLockedDayTime());
                if (settings.isLockWeather())
                    applyWeather(serverLevel, settings.isLockedWeatherRain(), settings.isLockedWeatherThunder());
            }
        }
    }

    private static void applyWeather(ServerLevel level, boolean rain, boolean thunder) {
        if (thunder)
            level.setWeatherParameters(0, 1000000, true, true);
        else if (rain)
            level.setWeatherParameters(0, 1000000, true, false);
        else
            level.setWeatherParameters(1000000, 0, false, false);
    }

    public static boolean isIsolatedBlock(Level level, BlockPos pos) {
        int solidCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getBlockState(pos.offset(dx, 0, dz)).blocksMotion()) {
                    solidCount++;
                }
            }
        }
        return solidCount < 3;
    }

    public static BlockPos findNearestSafeSurface(ServerLevel level, int startX, int startZ, int maxRadius) {
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;
        for (int i = 0; i < (maxRadius * 2 + 1) * (maxRadius * 2 + 1); i++) {
            int currentX = startX + x;
            int currentZ = startZ + z;
            int safeY = findSafeSurfaceY(level, currentX, currentZ);
            if (safeY > level.getMinBuildHeight()) {
                return new BlockPos(currentX, safeY, currentZ);
            }
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int t = dx;
                dx = -dz;
                dz = t;
            }
            x += dx;
            z += dz;
        }
        return null;
    }

    public static int findSafeSurfaceY(Level level, int x, int z) {
        int maxY = level.getMaxBuildHeight() - 1;
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, maxY, z);
        while (mutablePos.getY() >= minY) {
            if (level.getBlockState(mutablePos).blocksMotion() && !level.getBlockState(mutablePos).is(BlockTags.LEAVES)
                    && level.getBlockState(mutablePos.above()).isAir()
                    && level.getBlockState(mutablePos.above(2)).isAir()
                    && !isIsolatedBlock(level, mutablePos))
                return mutablePos.getY() + 1;
            mutablePos.move(0, -1, 0);
        }
        return minY - 1;
    }

    /**
     * Expels an entity 45 blocks away from (centerX, centerZ) at a random angle,
     * scanning upward from its current Y for a safe 2-air-block gap.
     * Applies Glowing for 10 seconds after teleport.
     */
    public static void expelFromDomain(Entity entity, ServerLevel level, double centerX, double centerZ) {
        double angle = level.random.nextDouble() * 2 * Math.PI;
        double targetX = centerX + Math.cos(angle) * 45.0;
        double targetZ = centerZ + Math.sin(angle) * 45.0;

        // Scan upward from entity's current Y for 2 consecutive air blocks
        int startY = (int) entity.getY();
        int maxY = level.getMaxBuildHeight() - 2;
        int safeY = startY;
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos((int) targetX, startY, (int) targetZ);
        for (int y = startY; y <= maxY; y++) {
            check.set((int) targetX, y, (int) targetZ);
            if (level.getBlockState(check).isAir() && level.getBlockState(check.above()).isAir()) {
                safeY = y;
                break;
            }
        }

        entity.teleportTo(targetX, safeY, targetZ);
        if (entity instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));
        }
    }

    private static UUID getOwnerIdFromEntityAndLevel(Entity entity, Level level) {
        if (entity != null && level instanceof ServerLevel serverLevel) {
            for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel
                    .getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                            entity.getBoundingBox().inflate(32))) {
                if (domain.isUsingDimensionRules()) {
                    net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
                    if (aabb != null ? aabb.contains(entity.position())
                            : entity.position().distanceToSqr(domain.position()) <= 32 * 32) {
                        return domain.getOwnerId();
                    }
                }
            }
            for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel
                    .getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class,
                            entity.getBoundingBox().inflate(10))) {
                if (domain.isUsingDimensionRules() && entity.position().distanceToSqr(domain.position()) <= 25) {
                    return domain.getOwnerId();
                }
            }
        }
        UUID ownerId = level instanceof ServerLevel ? getOwnerUUIDFromDimensionKey(level.dimension()) : null;
        if (ownerId == null && level instanceof ServerLevel serverLevel) {
            if (entity != null)
                ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
            else if (!serverLevel.players().isEmpty())
                ownerId = getOwnerUUIDFromPosition(serverLevel, serverLevel.players().iterator().next().getX(),
                        serverLevel.players().iterator().next().getZ());
        }
        if (ownerId == null && entity != null) {
            if (entity instanceof Player p)
                ownerId = p.getUUID();
            else if (entity instanceof OwnableEntity ownable)
                ownerId = ownable.getOwnerUUID();
        }
        return ownerId;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCatReflexesDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        // Check if entity has Cat Reflexes effect
        if (!entity.hasEffect(CAT_REFLEXES_EFFECT))
            return;

        // Only reduce damage from entity attacks
        if (!(event.getSource().getEntity() instanceof LivingEntity))
            return;

        // 20% dodge chance
        if (entity.level().random.nextFloat() < 0.20f) {
            event.setCanceled(true);

            if (entity instanceof com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity cat &&
                    com.tlmpersonal.tlmpersonaldimension.Config.CAT_FAMILIAR_TELEPORTS_TO_TARGET.get() &&
                    !entity.level().isClientSide) {

                ServerLevel level = (ServerLevel) entity.level();
                Vec3 oldPos = cat.position();

                // random offset target
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double distance = 0.5 + level.random.nextDouble() * 1.5;

                double tx = oldPos.x + Math.cos(angle) * distance;
                double ty = oldPos.y;
                double tz = oldPos.z + Math.sin(angle) * distance;

                BlockPos target = new BlockPos((int) tx, (int) ty, (int) tz);

                // your existing safe Y search
                int safeY = cat.findSafeY(new BlockPos.MutableBlockPos(target.getX(), target.getY(), target.getZ()), level);

                if (safeY > level.getMinBuildHeight()) {

                    BlockPos finalPos = new BlockPos((int) tx, safeY, (int) tz);

                    // IMPORTANT FIX: real space validation (snow/carpets/modded blocks fix)
                    boolean canFit =
                            level.getBlockState(finalPos).getCollisionShape(level, finalPos).isEmpty() &&
                                    level.getBlockState(finalPos.above()).getCollisionShape(level, finalPos.above()).isEmpty();

                    if (canFit) {
                        CatFamiliarEntity.spawnWitchParticles(oldPos, level);

                        cat.teleportTo(tx, safeY, tz);
                        cat.stopNavigation();

                        CatFamiliarEntity.spawnWitchParticles(cat.position(), level);
                        return;
                    }
                }

                // FALLBACK: center in current block (always works)
                double fx = Math.floor(oldPos.x) + 0.5;
                double fy = oldPos.y;
                double fz = Math.floor(oldPos.z) + 0.5;

                double jitter = 0.3;
                double fallbackAngle = level.random.nextDouble() * Math.PI * 2;

                fx += Math.cos(fallbackAngle) * jitter;
                fz += Math.sin(fallbackAngle) * jitter;

                CatFamiliarEntity.spawnWitchParticles(oldPos, level);

                cat.teleportTo(fx, fy, fz);
                cat.stopNavigation();
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onMaidOrOwnerHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide)
            return;
        // Only care about entity attacks (not fall, fire, etc.)
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker))
            return;

        ServerLevel serverLevel = (ServerLevel) entity.level();

        // If a maid is hurt, apply Cat Reflexes to it and look for a cat familiar
        if (entity instanceof EntityMaid maid) {
            for (com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity cat : serverLevel.getEntitiesOfClass(
                    com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity.class,
                    maid.getBoundingBox().inflate(32))) {
                if (maid.getUUID().equals(cat.getMaidId())) {
                    applyCatReflexesToEntity(maid);
                    // Also apply to owner if nearby
                    if (maid.getOwnerUUID() != null) {
                        Player owner = serverLevel.getPlayerByUUID(maid.getOwnerUUID());
                        if (owner != null && owner.distanceToSqr(maid) <= 32 * 32) {
                            applyCatReflexesToEntity(owner);
                        }
                        // Apply Bad Luck to attacker if it's not the owner
                        if (owner == null || !attacker.getUUID().equals(owner.getUUID())) {
                            applyBadLuckToEntity(attacker);
                        }
                    } else {
                        applyBadLuckToEntity(attacker);
                    }
                    break;
                }
            }
        }
        // If the owner player is hurt, apply Cat Reflexes to them and their maid
        else if (entity instanceof Player player) {
            for (com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity cat : serverLevel.getEntitiesOfClass(
                    com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity.class,
                    player.getBoundingBox().inflate(32))) {
                UUID maidId = cat.getMaidId();
                if (maidId == null)
                    continue;
                net.minecraft.world.entity.Entity maidEntity = serverLevel.getEntity(maidId);
                if (!(maidEntity instanceof EntityMaid maid))
                    continue;
                if (player.getUUID().equals(maid.getOwnerUUID())) {
                    applyCatReflexesToEntity(player);
                    applyCatReflexesToEntity(maid);
                    // Apply Bad Luck to attacker — it's definitely not the owner (it attacked the
                    // owner)
                    applyBadLuckToEntity(attacker);
                    break;
                }
            }
        }
    }

    private void applyBadLuckToEntity(LivingEntity entity) {
        if (!entity.hasEffect(BAD_LUCK_EFFECT)) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    BAD_LUCK_EFFECT, 1200, 0, false, true, true)); // 1 minute, visible particles + icon
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBadLuckAttack(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide)
            return;
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
        if (attacker == null)
            return;
        if (attacker.hasEffect(BAD_LUCK_EFFECT)) {
            if (attacker.level().random
                    .nextFloat() < com.tlmpersonal.tlmpersonaldimension.effect.BadLuckEffect.MISS_CHANCE) {
                event.setCanceled(true); // Attack misses
            }
        }
    }

    private void applyCatReflexesToEntity(LivingEntity entity) {
        // Only apply if not already active, to prevent duration accumulation
        if (!entity.hasEffect(CAT_REFLEXES_EFFECT)) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    CAT_REFLEXES_EFFECT, 1200, 0, false, false, false)); // exactly 1 minute, no particles
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onFelineGraceFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide)
            return;
        if (entity.hasEffect(FELINE_GRACE_EFFECT)) {
            event.setDamageMultiplier(event.getDamageMultiplier()
                    * (1.0f - com.tlmpersonal.tlmpersonaldimension.effect.FelineGraceEffect.FALL_DAMAGE_REDUCTION));
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide
                || !(isOurDimension(event.getEntity().level().dimension()) || isUnderDimensionRules(event.getEntity())))
            return;
        LivingEntity target = event.getEntity();
        ServerLevel level = (ServerLevel) target.level();
        UUID ownerId = getOwnerIdFromEntityAndLevel(target, level);
        if (ownerId == null)
            return;
        PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get(level)
                .getOrCreateSettings(ownerId);
        boolean isTargetOwner = target instanceof Player && ((Player) target).getUUID().equals(ownerId);
        boolean isTargetOwnerMaid = target instanceof EntityMaid maid && maid.getOwnerUUID() != null
                && maid.getOwnerUUID().equals(ownerId);
        boolean isTargetMaid = target instanceof EntityMaid;
        boolean isOwnerNearHisMaid = false;
        if (target instanceof Player player) {
            double range = Config.TAMED_MAID_PROTECTION_RANGE.get();
            for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(range))) {
                if (e instanceof EntityMaid maid && player.getUUID().equals(maid.getOwnerUUID())) {
                    isOwnerNearHisMaid = true;
                    break;
                }
            }
        }
        Entity attacker = event.getSource().getEntity();
        if (target.getHealth() - event.getAmount() <= 0) {
            if (isTargetMaid && (settings.isDisableMaidDeath() || Config.DISABLE_MAID_DEATH.get())) {
                event.setCanceled(true);
                target.setHealth(1.0f);
                return;
            }
            if (isTargetOwner && (settings.isDisablePlayerDeath() || Config.DISABLE_PLAYER_DEATH.get())) {
                event.setCanceled(true);
                target.setHealth(1.0f);
                return;
            }
            if (Config.TAMED_MAID_PROTECTION_ENABLED.get()
                    && (settings.isTamedMaidProtection() || Config.TAMED_MAID_PROTECTION.get())
                    && (isOwnerNearHisMaid || isTargetOwnerMaid)) {
                if (attacker != null && !attacker.getUUID().equals(ownerId)) {
                    long currentTime = System.currentTimeMillis();
                    long cooldownMs = Config.TAMED_MAID_PROTECTION_COOLDOWN.get() * 1000L;
                    if (currentTime - settings.getLastTamedMaidProtectionUse() < cooldownMs)
                        return;
                    ServerPlayer ownerPlayer = level.getServer() != null
                            ? level.getServer().getPlayerList().getPlayer(ownerId)
                            : null;
                    EntityMaid nearbyMaid = null;
                    double range = Config.TAMED_MAID_PROTECTION_RANGE.get();
                    for (Entity entity : level.getEntities(target, target.getBoundingBox().inflate(range))) {
                        if (entity instanceof EntityMaid maid && maid.getOwnerUUID() != null
                                && maid.getOwnerUUID().equals(ownerId)) {
                            nearbyMaid = maid;
                            break;
                        }
                    }
                    if (nearbyMaid == null && isTargetOwnerMaid)
                        nearbyMaid = (EntityMaid) target;
                    double powerCost = Config.TAMED_MAID_PROTECTION_POWER_POINTS_COST.get();
                    if (powerCost > 0.0
                            && (nearbyMaid == null || nearbyMaid.getExperience() < (int) Math.round(powerCost)))
                        return;
                    int xpCost = Config.TAMED_MAID_PROTECTION_XP_COST.get();
                    if (xpCost > 0 && (ownerPlayer == null || ownerPlayer.experienceLevel < xpCost))
                        return;
                    if (nearbyMaid != null && powerCost > 0.0)
                        nearbyMaid.setExperience(nearbyMaid.getExperience() - (int) Math.round(powerCost));
                    if (ownerPlayer != null && xpCost > 0)
                        ownerPlayer.giveExperienceLevels(-xpCost);
                    settings.setLastTamedMaidProtectionUse(currentTime);
                    PersonalDimensionSavedData.get(level).setDirty();
                    event.setCanceled(true);
                    target.setHealth(1.0f);
                    Touhoulittlemaidpersonaldimension.processRemoval(attacker);
                    return;
                }
            }
            if ((Config.ALL_MAID_PROTECTION.get() || Config.WILD_MAID_PROTECTION.get()) && isTargetMaid
                    && !isTargetOwnerMaid && attacker != null) {
                boolean isWildMaid = ((EntityMaid) target).getOwnerUUID() == null;
                if (isWildMaid || !attacker.getUUID().equals(ownerId)) {
                    event.setCanceled(true);
                    target.setHealth(1.0f);
                    Touhoulittlemaidpersonaldimension.processRemoval(attacker);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Level level = event.getEntity().level();
        Entity entity = event.getEntity();
        if (level instanceof ServerLevel serverLevel && entity instanceof EntityMaid maid
                && maid.getOwnerUUID() != null) {
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(serverLevel);
            savedData.getTrackedMaids().put(maid.getUUID(), new PersonalDimensionSavedData.MaidInfo(maid.getOwnerUUID(),
                    serverLevel.dimension(), maid.getX(), maid.getY(), maid.getZ()));
            savedData.setDirty();
        }
        if (level.isClientSide || !(isOurDimension(level.dimension()) || isUnderDimensionRules(entity)))
            return;
        UUID ownerId = getOwnerIdFromEntityAndLevel(entity, level);
        if (ownerId != null) {
            PersonalDimensionSavedData settingsData = PersonalDimensionSavedData.get((ServerLevel) level);
            PersonalDimensionSavedData.PlayerDimensionSettings settings = settingsData.getOrCreateSettings(ownerId);
            if (entity instanceof EntityMaid maid
                    && (settings.isMaidAttackDiscard() || Config.MAID_ATTACK_DISCARD.get())) {
                LivingEntity target = maid.getTarget();
                if (target != null && target.isAlive() && !target.getUUID().equals(ownerId)
                        && !(target instanceof EntityMaid)) {
                    target.discard();
                    maid.setTarget(null);
                }
            }
            if (settings.isEntityCannotTarget() || Config.ENTITY_CANNOT_TARGET.get()) {
                if (entity instanceof Mob mob && !(mob instanceof EntityMaid)) {
                    if (mob.getTarget() != null || mob.getLastHurtByMob() != null) {
                        mob.setTarget(null);
                        mob.targetSelector.getAvailableGoals()
                                .removeIf(wrapped -> wrapped.getGoal() instanceof NearestAttackableTargetGoal<?>);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        Level level = event.getEntity().level();
        if (level.isClientSide || !(isOurDimension(level.dimension()) || isUnderDimensionRules(event.getEntity())))
            return;
        Entity target = event.getEntity();
        UUID ownerId = getOwnerIdFromEntityAndLevel(target, level);
        if (ownerId == null)
            return;
        PersonalDimensionSavedData settingsData = PersonalDimensionSavedData.get((ServerLevel) level);
        PersonalDimensionSavedData.PlayerDimensionSettings settings = settingsData.getOrCreateSettings(ownerId);
        if ((settings.isBlockHarmfulEffects() || Config.BLOCK_HARMFUL_EFFECTS.get())
                && (target instanceof EntityMaid || (target instanceof Player && target.getUUID().equals(ownerId)))
                && !event.getEffectInstance().getEffect().value().isBeneficial()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingDamageEvent.Post event) {
        Level level = event.getEntity().level();
        if (level.isClientSide || !(isOurDimension(level.dimension()) || isUnderDimensionRules(event.getEntity())))
            return;
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker))
            return;
        UUID ownerId = getOwnerIdFromEntityAndLevel(victim, level);
        if (ownerId == null)
            return;
        PersonalDimensionSavedData settingsData = PersonalDimensionSavedData.get((ServerLevel) level);
        PersonalDimensionSavedData.PlayerDimensionSettings settings = settingsData.getOrCreateSettings(ownerId);
        if (attacker instanceof EntityMaid maid
                && (settings.isMaidAttackDiscard() || Config.MAID_ATTACK_DISCARD.get())) {
            if (victim.isAlive() && !victim.getUUID().equals(ownerId) && !(victim instanceof EntityMaid)) {
                victim.discard();
                maid.setTarget(null);
                return;
            }
        }
        if (!(victim instanceof Player) && !(victim instanceof EntityMaid))
            return;
        double range = Config.TAMED_MAID_PROTECTION_RANGE.get();
        for (Entity entity : victim.level().getEntities(victim, victim.getBoundingBox().inflate(range))) {
            if ((settings.isMaidAuthority() || Config.MAID_AUTHORITY.get()) && entity instanceof Mob mob
                    && mob != attacker && mob != victim && mob.isAlive())
                mob.setLastHurtByMob(attacker);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel() instanceof ServerLevel level && isOurDimension(level.dimension())) {
            BlockPos pos = event.getPos();
            UUID ownerId = getOwnerUUIDFromDimensionKey(level.dimension());
            if (ownerId == null)
                ownerId = getOwnerUUIDFromPosition(level, pos.getX(), pos.getZ());
            if (!isAllowed(event.getEntityType(), ownerId, level, null))
                event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if ((isOurDimension(event.getEntity().level().dimension()) || isUnderDimensionRules(event.getEntity()))
                && !Config.ALLOW_SET_SPAWN.get())
            event.setCanceled(true);
    }

    private static boolean hasTetheredTeleportBauble(EntityMaid maid) {
        if (maid.getMaidBauble() == null) return false;
        for (int i = 0; i < maid.getMaidBauble().getSlots(); i++) {
            if (maid.getMaidBauble().getStackInSlot(i).is(TETHERED_TELEPORT_BAUBLE.get())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof Player player && isOurDimension(player.level().dimension())) {
            if (!Config.DIMENSION_WHITELIST.get().contains(event.getDimension().location().toString())) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer serverPlayer)
                    serverPlayer.sendSystemMessage(Component.literal("This dimension is not whitelisted!"));
            }
        }
        if (event.getEntity() instanceof ServerPlayer ownerPlayer) {
            ServerLevel currentLevel = (ServerLevel) ownerPlayer.level();
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData
                    .get(currentLevel.getServer().getLevel(Level.OVERWORLD));
            ResourceKey<Level> targetDim = event.getDimension();
            for (Map.Entry<UUID, PersonalDimensionSavedData.MaidInfo> entry : savedData.getTrackedMaids().entrySet()) {
                PersonalDimensionSavedData.MaidInfo maidInfo = entry.getValue();
                if (!maidInfo.ownerUuid.equals(ownerPlayer.getUUID()))
                    continue;
                UUID maidUuid = entry.getKey();
                // Skip if the maid is already in the target dimension
                if (maidInfo.lastLevel.equals(targetDim))
                    continue;
                // Find the maid entity to check if it has the bauble
                EntityMaid maid = null;
                for (ServerLevel levelIter : currentLevel.getServer().getAllLevels()) {
                    Entity entity = levelIter.getEntity(maidUuid);
                    if (entity instanceof EntityMaid foundMaid) {
                        maid = foundMaid;
                        break;
                    }
                }
                // Check if either the global config is on OR the maid has the bauble
                boolean shouldTeleport = Config.MAID_TELEPORT_WITH_OWNER_DIMENSION.get();
                if (!shouldTeleport && maid != null) {
                    shouldTeleport = hasTetheredTeleportBauble(maid);
                }
                if (shouldTeleport) {
                    // Deduplicate: only queue if not already pending
                    if (MAIDS_PENDING_TELEPORT.add(maidUuid)) {
                        MAIDS_TO_TELEPORT.add(new MaidTeleportData(maidUuid, ownerPlayer.getUUID(), targetDim, 0));
                    }
                }
            }
        }
    }

    private record MaidTeleportData(UUID maidUuid, UUID ownerUuid, ResourceKey<Level> targetDim, int retries) {
    }

    private static final Queue<MaidTeleportData> MAIDS_TO_TELEPORT = new ConcurrentLinkedQueue<>();
    // Track maid UUIDs currently pending teleport to prevent duplicate queue
    // entries
    private static final Set<UUID> MAIDS_PENDING_TELEPORT = Collections.synchronizedSet(new HashSet<>());
    private static final int MAX_MAID_TELEPORT_RETRIES = 100; // ~5 seconds at 20 tps

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        // Personal dimension block breaking
        if (isOurDimension(level.dimension()) && !Config.ENABLE_BLOCK_BREAKING.get()) {
            event.setCanceled(true);
            return;
        }

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel))
            return;

        // Domain Expansion block breaking
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                new AABB(pos).inflate(200))) {
            net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
            boolean inDomain = aabb != null ? aabb.contains(pos.getX(), pos.getY(), pos.getZ())
                    : pos.distSqr(domain.blockPosition()) <= 32 * 32;
            if (inDomain) {
                boolean useDimRules = domain.isUsingDimensionRules();
                if (useDimRules && !Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING.get()) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        // Cherry Domain block breaking
        for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class,
                new AABB(pos).inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
                int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                int dx = pos.getX() - domain.blockPosition().getX();
                int dy = pos.getY() - domain.blockPosition().getY();
                int dz = pos.getZ() - domain.blockPosition().getZ();
                if (Math.abs(dx) <= hRadius && Math.abs(dy) <= vHalf && Math.abs(dz) <= hRadius) {
                    if (!Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING.get()) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();

        // Personal dimension block building
        if (isOurDimension(level.dimension()) && !Config.ENABLE_BLOCK_BUILDING.get()) {
            event.setCanceled(true);
            return;
        }

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel))
            return;

        // Domain Expansion block building (shares block breaking config)
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                new AABB(pos).inflate(200))) {
            net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
            boolean inDomain = aabb != null ? aabb.contains(pos.getX(), pos.getY(), pos.getZ())
                    : pos.distSqr(domain.blockPosition()) <= 32 * 32;
            if (inDomain) {
                if (domain.isUsingDimensionRules() && !Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING.get()) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        // Cherry Domain block building
        for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class,
                new AABB(pos).inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
                int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                int dx = pos.getX() - domain.blockPosition().getX();
                int dy = pos.getY() - domain.blockPosition().getY();
                int dz = pos.getZ() - domain.blockPosition().getZ();
                if (Math.abs(dx) <= hRadius && Math.abs(dy) <= vHalf && Math.abs(dz) <= hRadius) {
                    if (!Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING.get()) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel serverLevel2))
            return;
        UUID playerId = player.getUUID();
        for (ServerLevel lvl : serverLevel2.getServer().getAllLevels()) {
            List<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity> toRestore = new ArrayList<>();
            for (Entity e : lvl.getAllEntities()) {
                if (e instanceof com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain
                        && playerId.equals(domain.getOwnerId())) {
                    toRestore.add(domain);
                }
            }
            for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : toRestore) {
                domain.restoreAndDiscard();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (isOurDimension(level.dimension()) && !Config.ENABLE_BLOCK_BREAKING.get()) {
            event.getAffectedBlocks().clear();
            return;
        }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Domain Expansion: remove blocks only when dimension rules are active and block breaking is off
            event.getAffectedBlocks().removeIf(pos -> {
                for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel
                        .getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                                new AABB(pos).inflate(200))) {
                    if (!domain.isUsingDimensionRules() || Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING.get())
                        continue;
                    net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
                    boolean inDomain = aabb != null ? aabb.contains(pos.getX(), pos.getY(), pos.getZ())
                            : pos.distSqr(domain.blockPosition()) <= 32 * 32;
                    if (inDomain) return true;
                }
                // Cherry Domain: remove blocks only when dimension rules are active and block breaking is off
                for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel
                        .getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class,
                                new AABB(pos).inflate(200))) {
                    if (!domain.isUsingDimensionRules() || Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING.get())
                        continue;
                    int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
                    int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                    int dx = pos.getX() - domain.blockPosition().getX();
                    int dy = pos.getY() - domain.blockPosition().getY();
                    int dz = pos.getZ() - domain.blockPosition().getZ();
                    if (Math.abs(dx) <= hRadius && Math.abs(dy) <= vHalf && Math.abs(dz) <= hRadius)
                        return true;
                }
                return false;
            });
        }
    }
}
