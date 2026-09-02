package com.tlmpersonal.tlmpersonaldimension;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.info.ServerCustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity;
import com.tlmpersonal.tlmpersonaldimension.inventory.PersonalDimensionMenu;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.tlmpersonal.tlmpersonaldimension.accessor.MinecraftServerAccessor;
import com.tlmpersonal.tlmpersonaldimension.world.StructurePlacer;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionSettingsSyncPacket;
import com.tlmpersonal.tlmpersonaldimension.network.TeleporterCooldownSyncPacket;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.common.Tags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.common.extensions.IForgeMenuType;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod(Touhoulittlemaidpersonaldimension.MODID)
public class Touhoulittlemaidpersonaldimension {
    public static final String MODID = "touhoulittlemaidpersonaldimension";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    // Network channel for our packets
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);

    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> STRAIGHT_CHERRY_PARTICLE =
            PARTICLES.register("straight_cherry", () -> new net.minecraft.core.particles.SimpleParticleType(false));

    private static final Map<ResourceKey<Level>, ConcurrentLinkedQueue<BlockPos>> PLACEMENT_QUEUE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> PROCESSED_CHUNKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<BlockPos>> GENERATED_ISLANDS = new HashMap<>();
    private static final int VOID_PROTECTION_ENTITIES_PER_PASS = 32;
    private static final int RULE_ENTITIES_PER_PASS = 64;
    private static final int MAID_TELEPORTS_PER_TICK = 8;
    private static final Map<ResourceKey<Level>, Set<UUID>> TRACKED_PERSONAL_ENTITIES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Queue<UUID>> PERSONAL_ENTITY_QUEUES = new HashMap<>();

    /** Holds the position AND the dimension a maid's light block was placed in. */
    public record MaidLightEntry(BlockPos pos, ResourceKey<Level> dimension) {}

    /** Tracks the last light block placed by each maid (UUID → entry with pos + dim). */
    public static final Map<UUID, MaidLightEntry> MAID_LIGHT_POSITIONS = new HashMap<>();

    /** Active domains are tracked by stable identifiers, never by live entity references. */
    private static final Map<ResourceKey<Level>, Set<UUID>> ACTIVE_DOMAINS = new HashMap<>();

    public static void removeMaidLight(UUID maidId, net.minecraft.server.MinecraftServer server) {
        MaidLightEntry entry = MAID_LIGHT_POSITIONS.remove(maidId);
        if (entry == null) return;
        ServerLevel lightLevel = server.getLevel(entry.dimension());
        if (lightLevel != null && lightLevel.getBlockState(entry.pos()).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
            lightLevel.setBlockAndUpdate(entry.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }

    public static void clearLevelRuntimeState(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        ACTIVE_DOMAINS.remove(dimension);
        TRACKED_PERSONAL_ENTITIES.remove(dimension);
        PERSONAL_ENTITY_QUEUES.remove(dimension);

        Iterator<Map.Entry<UUID, MaidLightEntry>> iterator = MAID_LIGHT_POSITIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            MaidLightEntry light = iterator.next().getValue();
            if (!light.dimension().equals(dimension)) continue;
            if (level.hasChunkAt(light.pos())
                    && level.getBlockState(light.pos()).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
                level.setBlockAndUpdate(light.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            iterator.remove();
        }
    }

    public static void updateMaidLight(EntityMaid maid, ServerLevel level, boolean enabled) {
        UUID maidId = maid.getUUID();
        MaidLightEntry current = MAID_LIGHT_POSITIONS.get(maidId);
        if (!enabled) {
            if (current != null) removeMaidLight(maidId, level.getServer());
            return;
        }

        BlockPos target = maid.blockPosition().above();
        MaidLightEntry targetEntry = new MaidLightEntry(target, level.dimension());
        if (targetEntry.equals(current)) return;

        if (current != null) removeMaidLight(maidId, level.getServer());
        if (level.getBlockState(target).isAir() || level.getBlockState(target).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
            level.setBlockAndUpdate(target, net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState());
            MAID_LIGHT_POSITIONS.put(maidId, targetEntry);
        }
    }

    private static Collection<Entity> getActiveDomains(ServerLevel level) {
        Set<UUID> domainIds = ACTIVE_DOMAINS.get(level.dimension());
        if (domainIds == null || domainIds.isEmpty()) return List.of();

        List<Entity> domains = new ArrayList<>(domainIds.size());
        Iterator<UUID> iterator = domainIds.iterator();
        while (iterator.hasNext()) {
            Entity domain = level.getEntity(iterator.next());
            if (domain == null || domain.isRemoved()) iterator.remove();
            else domains.add(domain);
        }
        if (domainIds.isEmpty()) ACTIVE_DOMAINS.remove(level.dimension());
        return domains;
    }

    private static void registerActiveDomain(Entity entity) {
        ACTIVE_DOMAINS.computeIfAbsent(entity.level().dimension(), unused -> new HashSet<>()).add(entity.getUUID());
    }

    private static void trackPersonalEntity(Entity entity) {
        ResourceKey<Level> dimension = entity.level().dimension();
        Set<UUID> tracked = TRACKED_PERSONAL_ENTITIES.computeIfAbsent(dimension, unused -> new HashSet<>());
        if (tracked.add(entity.getUUID())) {
            PERSONAL_ENTITY_QUEUES.computeIfAbsent(dimension, unused -> new ArrayDeque<>()).add(entity.getUUID());
        }
    }

    private static void processTrackedPersonalEntities(ServerLevel level, int budget,
                                                       java.util.function.Consumer<Entity> processor) {
        ResourceKey<Level> dimension = level.dimension();
        Set<UUID> tracked = TRACKED_PERSONAL_ENTITIES.get(dimension);
        Queue<UUID> queue = PERSONAL_ENTITY_QUEUES.get(dimension);
        if (tracked == null || queue == null) return;

        int processed = 0;
        int attempts = 0;
        while (processed < budget && attempts++ < budget * 4) {
            UUID entityId = queue.poll();
            if (entityId == null) break;
            if (!tracked.contains(entityId)) continue;
            Entity entity = level.getEntity(entityId);
            if (entity == null || entity.isRemoved()) {
                tracked.remove(entityId);
                continue;
            }
            processor.accept(entity);
            processed++;
            if (!entity.isRemoved() && tracked.contains(entityId)) queue.add(entityId);
        }

        if (tracked.isEmpty()) {
            TRACKED_PERSONAL_ENTITIES.remove(dimension);
            PERSONAL_ENTITY_QUEUES.remove(dimension);
        }
    }

    public static final ResourceKey<Level> PERSONAL_DIMENSION_VOID_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(MODID, "personal_dimension"));
    public static final ResourceKey<Level> PERSONAL_DIMENSION_NORMAL_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(MODID, "personal_dimension_normal"));
    public static final ResourceKey<Level> PERSONAL_DIMENSION_CHERRY_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(MODID, "personal_dimension_cherry"));

    public static ResourceKey<Level> getCurrentPersonalDimensionKey() {
        List<CustomDimensionConfig> dimensions = CustomDimensionConfig.loadFromConfig();
        String defaultId = Config.DEFAULT_DIMENSION_TYPE_ID.get();
        CustomDimensionConfig dim = CustomDimensionConfig.findById(defaultId, dimensions);
        if (dim == null) dim = dimensions.isEmpty() ? null : dimensions.get(0);
        if (dim != null) {
            return ResourceKey.create(Registries.DIMENSION, dim.getTemplateDimensionKey());
        }
        return PERSONAL_DIMENSION_VOID_KEY;
    }

    public static boolean isOurDimension(ResourceKey<Level> dim) {
        if (dim == null) return false;
        String path = dim.location().getPath();
        return path.startsWith("personal_dimension") && dim.location().getNamespace().equals(MODID);
    }

    public static boolean isUnderDimensionRules(Entity entity) {
        if (entity == null || entity.level().isClientSide || !(entity.level() instanceof ServerLevel level)) return false;
        for (Entity domain : getActiveDomains(level)) {
            if (domain instanceof com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity expansion) {
                if (expansion.isUsingDimensionRules()) {
                    net.minecraft.world.phys.AABB aabb = expansion.getStructureAABB();
                    if (aabb != null && aabb.contains(entity.position())) {
                        return true;
                    }
                }
            } else if (domain instanceof com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity cherry) {
                if (cherry.isUsingDimensionRules()) {
                    int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
                    int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                    double dx = entity.getX() - cherry.getX();
                    double dy = entity.getY() - cherry.getY();
                    double dz = entity.getZ() - cherry.getZ();
                    if (dx * dx + dz * dz <= hRadius * hRadius && Math.abs(dy) <= vHalf) return true;
                }
            }
        }
        return false;
    }

    public static boolean isUnderDimensionRules(Level level, BlockPos pos) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return false;
        for (Entity domain : getActiveDomains(serverLevel)) {
            if (domain instanceof com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity expansion) {
                if (expansion.isUsingDimensionRules()) {
                    net.minecraft.world.phys.AABB aabb = expansion.getStructureAABB();
                    if (aabb != null && aabb.contains(pos.getX(), pos.getY(), pos.getZ())) return true;
                }
            } else if (domain instanceof com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity cherry) {
                if (cherry.isUsingDimensionRules()) {
                    int hRadius = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get();
                    int vHalf = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                    int dx = pos.getX() - cherry.blockPosition().getX();
                    int dy = pos.getY() - cherry.blockPosition().getY();
                    int dz = pos.getZ() - cherry.blockPosition().getZ();
                    if (dx * dx + dz * dz <= hRadius * hRadius && Math.abs(dy) <= vHalf) return true;
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
        if (DIMENSION_OWNER_CACHE.containsKey(dim)) return DIMENSION_OWNER_CACHE.get(dim);
        String path = dim.location().getPath();
        try {
            String prefix = "personal_dimension_";
            if (path.startsWith(prefix)) {
                String uuidPart = path.substring(prefix.length());
                int lastUnderscore = uuidPart.lastIndexOf('_');
                if (lastUnderscore != -1) {
                    String uuidStr = uuidPart.substring(0, lastUnderscore).replace('_', '-');
                    UUID uuid = UUID.fromString(uuidStr);
                    DIMENSION_OWNER_CACHE.put(dim, uuid);
                    return uuid;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public record TeleportLocation(double x, double y, double z, float yRot, float xRot) {}

    public static final Map<UUID, Map<ResourceKey<Level>, TeleportLocation>> TELEPORT_HISTORY = new HashMap<>();

    public static final RegistryObject<EntityType<com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity>> DOMAIN_EXPANSION_ENTITY =
            ENTITY_TYPES.register("domain_expansion",
                    () -> EntityType.Builder.<com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity>of(
                            com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f).clientTrackingRange(20).build("domain_expansion"));

    public static final RegistryObject<EntityType<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity>> CHERRY_DOMAIN_ENTITY =
            ENTITY_TYPES.register("cherry_domain",
                    () -> EntityType.Builder.<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity>of(
                            com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f).clientTrackingRange(20).build("cherry_domain"));

    public static final RegistryObject<EntityType<com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity>> CAT_FAMILIAR_ENTITY =
            ENTITY_TYPES.register("cat_familiar",
                    () -> EntityType.Builder.<com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity>of(
                            com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 0.7f).clientTrackingRange(20).build("cat_familiar"));

    public static final RegistryObject<net.minecraft.world.effect.MobEffect> CAT_REFLEXES_EFFECT =
            MOB_EFFECTS.register("cat_reflexes", com.tlmpersonal.tlmpersonaldimension.effect.CatReflexesEffect::new);
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> FELINE_GRACE_EFFECT =
            MOB_EFFECTS.register("feline_grace", com.tlmpersonal.tlmpersonaldimension.effect.FelineGraceEffect::new);
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> BAD_LUCK_EFFECT =
            MOB_EFFECTS.register("bad_luck", com.tlmpersonal.tlmpersonaldimension.effect.BadLuckEffect::new);

    public static final RegistryObject<MaidTeleporter> MAID_TELEPORTER =
            ITEMS.register("maid_teleporter", () -> new MaidTeleporter(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TAB_ICON =
            ITEMS.register("tab_icon", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DOMAIN_EXPANSION_BAUBLE =
            ITEMS.register("domain_expansion_bauble",
                    () -> new com.tlmpersonal.tlmpersonaldimension.item.DomainExpansionBaubleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHERRY_DOMAIN_BAUBLE =
            ITEMS.register("cherry_domain_bauble",
                    () -> new com.tlmpersonal.tlmpersonaldimension.item.CherryDomainBaubleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAT_FAMILIAR_BAUBLE =
            ITEMS.register("cat_familiar_bauble",
                    () -> new com.tlmpersonal.tlmpersonaldimension.item.CatFamiliarBaubleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TETHERED_TELEPORT_BAUBLE =
            ITEMS.register("tethered_teleport_bauble",
                    () -> new com.tlmpersonal.tlmpersonaldimension.item.TetheredTeleportBaubleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> PERSONAL_DIMENSION_TAB = CREATIVE_MODE_TABS
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

    public static final RegistryObject<MenuType<PersonalDimensionMenu>> PERSONAL_DIMENSION_MENU =
            MENUS.register("personal_dimension_menu",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new PersonalDimensionMenu(windowId, inv, data.readInt())));

    // Maid teleport queue
    record MaidTeleportData(UUID maidUuid, UUID ownerUuid, ResourceKey<Level> targetDim, int retries) {}
    private static final int MAX_MAID_TELEPORT_RETRIES = 40;
    private static final java.util.Queue<MaidTeleportData> MAIDS_TO_TELEPORT = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> MAIDS_PENDING_TELEPORT = java.util.Collections.synchronizedSet(new HashSet<>());

    public Touhoulittlemaidpersonaldimension() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::addCreative);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        PARTICLES.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register network packets (Forge SimpleChannel pattern)
        int id = 0;
        NETWORK.registerMessage(id++,
                com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket.class,
                com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket::encode,
                com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket::decode,
                com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        NETWORK.registerMessage(id++,
                com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket.class,
                com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket::encode,
                com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket::decode,
                com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        NETWORK.registerMessage(id++,
                PersonalDimensionSettingsSyncPacket.class,
                PersonalDimensionSettingsSyncPacket::encode,
                PersonalDimensionSettingsSyncPacket::decode,
                PersonalDimensionSettingsSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NETWORK.registerMessage(id++,
                TeleporterCooldownSyncPacket.class,
                TeleporterCooldownSyncPacket::encode,
                TeleporterCooldownSyncPacket::decode,
                TeleporterCooldownSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new TouhoulittlemaidpersonaldimensionClient();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Register custom recipe condition
        net.minecraftforge.common.crafting.CraftingHelper.register(
                com.tlmpersonal.tlmpersonaldimension.condition.BaubleCraftableCondition.Serializer.INSTANCE);

        com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid.EXTENSIONS.add(new com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid() {
            @Override
            public void bindMaidBauble(com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager manager) {
                manager.bind(DOMAIN_EXPANSION_BAUBLE.get(), new com.tlmpersonal.tlmpersonaldimension.item.DomainExpansionBauble());
                manager.bind(CHERRY_DOMAIN_BAUBLE.get(), new com.tlmpersonal.tlmpersonaldimension.item.CherryDomainBauble());
                manager.bind(CAT_FAMILIAR_BAUBLE.get(), new com.tlmpersonal.tlmpersonaldimension.item.CatFamiliarBauble());
                manager.bind(TETHERED_TELEPORT_BAUBLE.get(), new com.tlmpersonal.tlmpersonaldimension.item.TetheredTeleportBauble());
            }
        });
    }

    private void registerEntityAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(CAT_FAMILIAR_ENTITY.get(),
                com.tlmpersonal.tlmpersonaldimension.entity.CatFamiliarEntity.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAID_TELEPORTER.get());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        com.tlmpersonal.tlmpersonaldimension.command.DomainCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        StructurePlacer.resetForNewServer();
        PROCESSED_CHUNKS.clear();
        PLACEMENT_QUEUE.clear();
        GENERATED_ISLANDS.clear();
        TRACKED_PERSONAL_ENTITIES.clear();
        PERSONAL_ENTITY_QUEUES.clear();
        DIMENSION_OWNER_CACHE.clear();
        MAIDS_TO_TELEPORT.clear();
        MAIDS_PENDING_TELEPORT.clear();
        ACTIVE_DOMAINS.clear();
        MAID_LIGHT_POSITIONS.clear();
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
        if (finalOwnerId != null && player.getUUID().equals(finalOwnerId)) return true;
        if (settings == null && finalOwnerId != null && level != null)
            settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
        if (settings != null) {
            String playerName = player.getGameProfile().getName();
            String playerUuidStr = player.getUUID().toString();
            for (String allowed : settings.getAllowedPlayers()) {
                if (allowed.equalsIgnoreCase(playerName) || allowed.equals(playerUuidStr)) return true;
            }
        }
        return !Config.PRIVATE_DIMENSION.get() || finalOwnerId == null;
    }

    private static boolean matchesAnyPattern(String entityId, java.util.Collection<? extends String> patterns) {
        for (String pattern : patterns) {
            if (pattern.equals(entityId)) return true;
            if (pattern.endsWith(":*")) {
                String namespace = pattern.substring(0, pattern.length() - 2);
                if (entityId.startsWith(namespace + ":")) return true;
            }
        }
        return false;
    }

    public static void saveEntityPosition(Entity entity, ResourceKey<Level> dimension) {
        UUID uuid = entity.getUUID();
        Map<ResourceKey<Level>, TeleportLocation> entityMap = TELEPORT_HISTORY.computeIfAbsent(uuid, k -> new HashMap<>());
        entityMap.put(dimension, new TeleportLocation(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot()));
    }

    public static TeleportLocation getEntityPosition(UUID uuid, ResourceKey<Level> dimension) {
        Map<ResourceKey<Level>, TeleportLocation> entityMap = TELEPORT_HISTORY.get(uuid);
        return entityMap != null ? entityMap.get(dimension) : null;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static void tryTriggerDomainExpansion(EntityMaid maid) {
        if (maid.level().isClientSide()) return;
        if (maid.getFavorabilityManager().getLevel() < 3) return;
        ServerLevel level = (ServerLevel) maid.level();
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(level.getServer().getLevel(Level.OVERWORLD));
        PersonalDimensionSavedData.PlayerDimensionSettings settings = null;
        if (maid.getOwnerUUID() != null) settings = savedData.getOrCreateSettings(maid.getOwnerUUID());
        if (settings != null) {
            long currentTime = System.currentTimeMillis();
            long cooldownMs = Config.DOMAIN_EXPANSION_COOLDOWN_SECONDS.get() * 1000L;
            if (currentTime - settings.getLastDomainExpansionUse() < cooldownMs) return;
            settings.setLastDomainExpansionUse(currentTime);
            savedData.setDirty();
        }
        if (level.getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                maid.getBoundingBox().inflate(Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()))
                .stream().anyMatch(d -> maid.getUUID().equals(d.getMaidId()))) return;
        BlockPos maidPos = new BlockPos(maid.blockPosition());
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity existing : level.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class,
                new net.minecraft.world.phys.AABB(maidPos).inflate(Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()))) {
            if (existing.blockPosition().distSqr(maidPos) < (double) Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()
                    * Config.DOMAIN_EXPANSION_MIN_DISTANCE.get()) return;
        }
        com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domainEntity = DOMAIN_EXPANSION_ENTITY.get().create(level);
        if (domainEntity != null) {
            domainEntity.moveTo(maid.getX(), maid.getY() - 1, maid.getZ(), 0, 0);
            domainEntity.setMaidId(maid.getUUID());
            if (maid.getOwnerUUID() != null) domainEntity.setOwnerId(maid.getOwnerUUID());
            level.addFreshEntity(domainEntity);
        }
    }

    public static void tickCherryDomain(EntityMaid maid) {
        if (maid.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) maid.level();
        boolean canSustain = true;
        if (Config.CHERRY_DOMAIN_XP_COST_ENABLED.get() && maid.getOwnerUUID() != null) {
            net.minecraft.world.entity.player.Player owner = level.getServer().getPlayerList().getPlayer(maid.getOwnerUUID());
            if (owner != null) {
                int cost = Config.CHERRY_DOMAIN_XP_COST.get();
                if (cost > 0) {
                    int intervalTicks = Config.CHERRY_DOMAIN_XP_COST_INTERVAL_SECONDS.get() * 20;
                    if (owner.experienceLevel < cost) canSustain = false;
                    else if (maid.tickCount % intervalTicks == 0) owner.giveExperienceLevels(-cost);
                }
            } else canSustain = false;
        }
        Optional<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity> existing = level
                .getEntitiesOfClass(com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class, maid.getBoundingBox().inflate(10))
                .stream().filter(d -> maid.getUUID().equals(d.getMaidId())).findFirst();
        if (canSustain) {
            if (existing.isPresent()) existing.get().resetTimeout();
            else {
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domainEntity = CHERRY_DOMAIN_ENTITY.get().create(level);
                if (domainEntity != null) {
                    domainEntity.moveTo(maid.getX(), maid.getY(), maid.getZ(), 0, 0);
                    domainEntity.setMaidId(maid.getUUID());
                    if (maid.getOwnerUUID() != null) domainEntity.setOwnerId(maid.getOwnerUUID());
                    level.addFreshEntity(domainEntity);
                }
            }
        }
    }

    public static boolean isAllowed(Entity entity, UUID ownerId, ServerLevel level,
            PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        if (entity == null) return false;
        
        // Handle players separately
        if (entity instanceof Player player) {
            return isPlayerAllowed(player, ownerId, level, settings);
        }
        
        // Get entity type information
        ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return false; // Safety check for unknown entity types
        String idString = entityId.toString();
        
        // Always allow specific entities like maids and chairs
        if (idString.equals("touhou_little_maid:maid") || idString.equals("touhou_little_maid:chair")) {
            return true;
        }
        
        // Retrieve or create settings if needed
        UUID finalOwnerId = ownerId;
        if (settings == null) {
            if (finalOwnerId == null && level != null) {
                finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
                if (finalOwnerId == null) {
                    finalOwnerId = getOwnerUUIDFromPosition(level, entity.getX(), entity.getZ());
                }
            }
            if (finalOwnerId != null && level != null) {
                settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
            }
        }
        
        // Check blocked entities and hostile entities first (highest priority checks)
        if (settings != null) {
            // Check if entity is explicitly blocked
            if (matchesAnyPattern(idString, settings.getBlockedEntities()) || 
                matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get())) {
                return false;
            }
            // Check if hostile entities are disabled and this is a monster
            if ((settings.isDisableHostileEntities() || Config.DISABLE_HOSTILE_ENTITIES.get()) && 
                entity.getType().getCategory() == MobCategory.MONSTER) {
                return false;
            }
        } else {
            // No settings available, use global config only
            if (matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get())) {
                return false;
            }
            if (Config.DISABLE_HOSTILE_ENTITIES.get() && 
                entity.getType().getCategory() == MobCategory.MONSTER) {
                return false;
            }
        }
        
        // Always allow certain utility entities (items, projectiles, etc.)
        if ((entity instanceof ItemEntity || entity instanceof Projectile) ||
            (entity.getType().getCategory() == MobCategory.MISC && !(entity instanceof LivingEntity))) {
            return true;
        }
        
        // Check if entity is explicitly allowed
        if (settings != null && (settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() ||
            matchesAnyPattern(idString, settings.getAllowedEntities()) || 
            matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get()))) {
            return true;
        }
        
        // Handle private dimension mode
        if (Config.PRIVATE_DIMENSION.get()) {
            if (settings == null) return false;
            return settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() ||
                   matchesAnyPattern(idString, settings.getAllowedEntities()) || 
                   matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get());
        }
        
        // Handle whitelist mode
        if (Config.ENTITY_WHITELIST_MODE.get()) {
            if (matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get())) {
                return true;
            }
            return Config.ALLOW_ALL_ENTITIES.get();
        }
        
        // Default behavior: allow based on global config
        return Config.ALLOW_ALL_ENTITIES.get();
    }

    public static boolean isAllowed(EntityType<?> entityType, UUID ownerId, ServerLevel level,
            PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        if (entityType == null) return false;
        
        // Always allow players
        if (entityType == EntityType.PLAYER) return true;
        
        // Get entity type information
        ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (entityId == null) return false; // Safety check for unknown entity types
        String idString = entityId.toString();
        
        // Always allow specific entities like maids and chairs
        if (idString.equals("touhou_little_maid:maid") || idString.equals("touhou_little_maid:chair")) {
            return true;
        }
        
        // Retrieve or create settings if needed
        UUID finalOwnerId = ownerId;
        if (settings == null) {
            if (finalOwnerId == null && level != null) {
                finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
            }
            if (finalOwnerId != null && level != null) {
                settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
            }
        }
        
        // Check blocked entities and hostile entities first (highest priority checks)
        if (settings != null) {
            // Check if entity is explicitly blocked
            if (matchesAnyPattern(idString, settings.getBlockedEntities()) || 
                matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get())) {
                return false;
            }
            // Check if hostile entities are disabled and this is a monster
            if ((settings.isDisableHostileEntities() || Config.DISABLE_HOSTILE_ENTITIES.get()) && 
                entityType.getCategory() == MobCategory.MONSTER) {
                return false;
            }
        } else {
            // No settings available, use global config only
            if (matchesAnyPattern(idString, Config.BLOCKED_ENTITIES.get())) {
                return false;
            }
            if (Config.DISABLE_HOSTILE_ENTITIES.get() && 
                entityType.getCategory() == MobCategory.MONSTER) {
                return false;
            }
        }
        
        // Always allow certain utility entities (items, projectiles, etc.)
        // Note: We can't use instanceof checks for EntityType, so we check specific IDs
        if ((entityType.getCategory() == MobCategory.MISC && !idString.equals("minecraft:bee") && !idString.equals("minecraft:villager"))
                || idString.equals("minecraft:item") || idString.equals("minecraft:experience_orb")
                || idString.equals("minecraft:arrow") || idString.equals("minecraft:snowball")
                || idString.equals("minecraft:egg") || idString.equals("minecraft:ender_pearl")
                || idString.equals("minecraft:splash_potion") || idString.equals("minecraft:lingering_potion")
                || idString.equals("minecraft:firework_rocket") || idString.equals("minecraft:fireball")
                || idString.equals("minecraft:small_fireball") || idString.equals("minecraft:dragon_fireball")
                || idString.equals("minecraft:wither_skull") || idString.equals("minecraft:llama_spit")
                || idString.equals("minecraft:shulker_bullet") || idString.equals("minecraft:trident")) {
            return true;
        }
        
        // Check if entity is explicitly allowed
        if (settings != null && (settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() ||
            matchesAnyPattern(idString, settings.getAllowedEntities()) || 
            matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get()))) {
            return true;
        }
        
        // Handle private dimension mode
        if (Config.PRIVATE_DIMENSION.get()) {
            if (settings == null) return false;
            return settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() ||
                   matchesAnyPattern(idString, settings.getAllowedEntities()) || 
                   matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get());
        }
        
        // Handle whitelist mode
        if (Config.ENTITY_WHITELIST_MODE.get()) {
            if (matchesAnyPattern(idString, Config.ALLOWED_ENTITIES.get())) {
                return true;
            }
            return Config.ALLOW_ALL_ENTITIES.get();
        }
        
        // Default behavior: allow based on global config
        return Config.ALLOW_ALL_ENTITIES.get();
    }

    public static boolean isBossEntity(Entity entity) {
        if (entity == null) return false;
        EntityType<?> entityType = entity.getType();
        TagKey<EntityType<?>> forgeBossesTag = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses"));
        TagKey<EntityType<?>> cBossesTag = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, new ResourceLocation("c", "bosses"));
        return entityType.is(forgeBossesTag) || entityType.is(cBossesTag);
    }

    public static boolean isMaidTeleporterAllowed(Entity entity) {
        if (entity == null || entity instanceof Player) return false;
        ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        String idString = entityId.toString();
        if (Config.MAID_TELEPORTER_EXCLUDE_BOSSES.get() && isBossEntity(entity)) return false;
        if (matchesAnyPattern(idString, Config.MAID_TELEPORTER_BLOCKED_ENTITIES.get())) return false;
        if (Config.MAID_TELEPORTER_ALLOW_ALL_ENTITIES.get()) return true;
        if (Config.MAID_TELEPORTER_ENTITY_WHITELIST_MODE.get())
            return matchesAnyPattern(idString, Config.MAID_TELEPORTER_ALLOWED_ENTITIES.get());
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity != null && !entity.level().isClientSide() && (entity instanceof com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity
                || entity instanceof com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity)) {
            registerActiveDomain(entity);
        }
        if (entity == null || entity.level().isClientSide() || !isOurDimension(entity.level().dimension())) return;
        trackPersonalEntity(entity);
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null) ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
        if (ownerId == null && !serverLevel.players().isEmpty())
            ownerId = serverLevel.players().iterator().next().getUUID();
        if (ownerId == null) return;
        if (!isAllowed(entity, ownerId, serverLevel, null)) {
            event.setCanceled(true);
            entity.discard();
        }
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        Level departedLevel = event.getLevel();
        if (entity != null && !departedLevel.isClientSide()
                && (entity instanceof com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity
                || entity instanceof com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity)) {
            Set<UUID> domainIds = ACTIVE_DOMAINS.get(departedLevel.dimension());
            if (domainIds != null) {
                domainIds.remove(entity.getUUID());
                if (domainIds.isEmpty()) ACTIVE_DOMAINS.remove(departedLevel.dimension());
            }
        }
        if (entity != null && !departedLevel.isClientSide() && isOurDimension(departedLevel.dimension())) {
            Set<UUID> tracked = TRACKED_PERSONAL_ENTITIES.get(departedLevel.dimension());
            if (tracked != null) {
                tracked.remove(entity.getUUID());
                Queue<UUID> queue = PERSONAL_ENTITY_QUEUES.get(departedLevel.dimension());
                if (queue != null) queue.removeIf(entity.getUUID()::equals);
                if (tracked.isEmpty()) {
                    TRACKED_PERSONAL_ENTITIES.remove(departedLevel.dimension());
                    PERSONAL_ENTITY_QUEUES.remove(departedLevel.dimension());
                }
            }
        }
        if (entity instanceof EntityMaid maid && departedLevel instanceof ServerLevel serverLevel) {
            removeMaidLight(maid.getUUID(), serverLevel.getServer());
        }
    }

    @SubscribeEvent
    public void onMaidJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof EntityMaid maid) || maid.getOwnerUUID() == null)
            return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(serverLevel);
        savedData.getTrackedMaids().put(maid.getUUID(), new PersonalDimensionSavedData.MaidInfo(
                maid.getOwnerUUID(), serverLevel.dimension(), maid.getX(), maid.getY(), maid.getZ()));
        savedData.setDirty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFinalizeSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !isOurDimension(entity.level().dimension())) return;
        if (event.getSpawnType() == MobSpawnType.CONVERSION) return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null) ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
        if (ownerId == null && !serverLevel.players().isEmpty())
            ownerId = serverLevel.players().iterator().next().getUUID();
        if (!isAllowed(entity, ownerId, serverLevel, null)) {
            event.setSpawnCancelled(true);
            entity.discard();
        }
    }

    public static void processRemoval(Entity entity) {
        if (entity == null) return;
        if (Config.REMOVE_BLOCKED_ENTITIES.get()) {
            if (entity instanceof ServerPlayer player) {
                ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    TeleportLocation savedPos = getEntityPosition(player.getUUID(), Level.OVERWORLD);
                    if (savedPos != null)
                        player.teleportTo(overworld, savedPos.x(), savedPos.y(), savedPos.z(), java.util.Set.of(), savedPos.yRot(), savedPos.xRot());
                    else {
                        BlockPos spawn = overworld.getSharedSpawnPos();
                        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot());
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
                if (safeY > serverLevel.getMinBuildHeight()) entity.teleportTo(targetX, safeY, targetZ);
                else entity.teleportTo(targetX, entity.getY(), targetZ);
                if (entity instanceof ServerPlayer player) player.sendSystemMessage(Component.literal("You were expelled!"));
                return;
            }
            ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                TeleportLocation savedPos = getEntityPosition(entity.getUUID(), Level.OVERWORLD);
                if (savedPos == null) {
                    UUID fallbackId = null;
                    if (entity instanceof OwnableEntity ownable) fallbackId = ownable.getOwnerUUID();
                    if (fallbackId == null) fallbackId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
                    if (fallbackId == null) fallbackId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
                    if (fallbackId != null) savedPos = getEntityPosition(fallbackId, Level.OVERWORLD);
                }
                if (savedPos != null)
                    entity.teleportTo(overworld, savedPos.x(), savedPos.y(), savedPos.z(), java.util.Set.of(), savedPos.yRot(), savedPos.xRot());
                else {
                    BlockPos spawn = overworld.getSharedSpawnPos();
                    entity.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5, java.util.Set.of(), entity.getYRot(), entity.getXRot());
                }
                if (entity instanceof ServerPlayer player) player.sendSystemMessage(Component.literal("You are not allowed here!"));
                return;
            }
        }
        entity.discard();
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Level level = event.level;
        
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension().equals(Level.OVERWORLD) && !MAIDS_TO_TELEPORT.isEmpty()) {
                List<MaidTeleportData> retryList = new ArrayList<>();
                for (int processed = 0; processed < MAID_TELEPORTS_PER_TICK; processed++) {
                    MaidTeleportData data = MAIDS_TO_TELEPORT.poll();
                    if (data == null) break;
                    if (data.retries() >= MAX_MAID_TELEPORT_RETRIES) { MAIDS_PENDING_TELEPORT.remove(data.maidUuid()); continue; }
                    ServerLevel targetLevel = serverLevel.getServer().getLevel(data.targetDim());
                    if (targetLevel == null) { MAIDS_PENDING_TELEPORT.remove(data.maidUuid()); continue; }
                    ServerPlayer targetOwner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUuid());
                    if (targetOwner == null) { retryList.add(new MaidTeleportData(data.maidUuid(), data.ownerUuid(), data.targetDim(), data.retries() + 1)); continue; }
                    if (!targetOwner.level().dimension().equals(data.targetDim())) { retryList.add(new MaidTeleportData(data.maidUuid(), data.ownerUuid(), data.targetDim(), data.retries() + 1)); continue; }
                    EntityMaid maid = null;
                    for (ServerLevel lvl : serverLevel.getServer().getAllLevels()) {
                        Entity e = lvl.getEntity(data.maidUuid());
                        if (e instanceof EntityMaid found) { maid = found; break; }
                    }
                    if (maid == null) { MAIDS_PENDING_TELEPORT.remove(data.maidUuid()); continue; }
                    if (maid.level().dimension().equals(data.targetDim())) { MAIDS_PENDING_TELEPORT.remove(data.maidUuid()); continue; }
                    maid.teleportTo(targetLevel, targetOwner.getX(), targetOwner.getY(), targetOwner.getZ(), java.util.Set.of(), targetOwner.getYRot(), targetOwner.getXRot());
                    MAIDS_PENDING_TELEPORT.remove(data.maidUuid());
                }
                MAIDS_TO_TELEPORT.addAll(retryList);
            }
        }
        if (!(level instanceof ServerLevel serverLevel2) || !isOurDimension(level.dimension())) return;
        
        int spawnChance = Config.MAID_SPAWN_CHANCE.get();
        if (spawnChance > 0 && serverLevel2.getGameTime() % 200 == 0) {
            String dimPath = serverLevel2.dimension().location().getPath();
            if (dimPath.contains("normal") || dimPath.contains("cherry")) {
                if (serverLevel2.random.nextInt(spawnChance) == 0 && !serverLevel2.players().isEmpty()) {
                    Player player = serverLevel2.players().iterator().next();
                    BlockPos playerPos = player.blockPosition();
                    int spawnX = playerPos.getX() + serverLevel2.random.nextInt(64) - 32;
                    int spawnZ = playerPos.getZ() + serverLevel2.random.nextInt(64) - 32;
                    int spawnY = findSafeSurfaceY(serverLevel2, spawnX, spawnZ);
                    if (spawnY > serverLevel2.getMinBuildHeight()) {
                        BlockPos spawnPos = new BlockPos(spawnX, spawnY, spawnZ);
                        if (serverLevel2.canSeeSky(spawnPos) && !serverLevel2.getFluidState(spawnPos.below()).isSource()) {
                            EntityMaid maid = InitEntities.MAID.get().create(serverLevel2);
                            if (maid != null) {
                                if (Config.USE_YSM_MODELS.get()) {
                                    List<? extends String> modelIdsList = Config.YSM_MODEL_IDS.get();
                                    if (!modelIdsList.isEmpty()) {
                                        maid.setIsYsmModel(true);
                                        maid.setYsmModel(modelIdsList.get(serverLevel2.random.nextInt(modelIdsList.size())), "", net.minecraft.network.chat.Component.literal(""));
                                    }
                                } else {
                                    int modelSize = ServerCustomPackLoader.SERVER_MAID_MODELS.getModelSize();
                                    if (modelSize > 0) ServerCustomPackLoader.SERVER_MAID_MODELS.getModelIdSet().stream()
                                            .skip(serverLevel2.random.nextInt(modelSize)).findFirst().ifPresent(maid::setModelId);
                                }
                                maid.finalizeSpawn(serverLevel2, serverLevel2.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null, null);
                                maid.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, serverLevel2.random.nextFloat() * 360.0F, 0.0F);
                                serverLevel2.addFreshEntity(maid);
                            }
                        }
                    }
                }
            }
        }
        // Throttled void fall protection check (every 10 ticks / 0.5 seconds)
        if (serverLevel2.getGameTime() % 10 == 0) {
            processTrackedPersonalEntities(serverLevel2, VOID_PROTECTION_ENTITIES_PER_PASS, entity -> {
                if (entity.getY() < Config.FALL_PROTECTION_Y.get()) {
                    BlockPos safePos = findNearestSafeSurface(serverLevel2, (int) entity.getX(), (int) entity.getZ(), 16);
                    if (safePos == null) {
                        TeleportLocation savedPos2 = getEntityPosition(entity.getUUID(), serverLevel2.dimension());
                        if (savedPos2 != null) safePos = findNearestSafeSurface(serverLevel2, (int) savedPos2.x(), (int) savedPos2.z(), 16);
                    }
                    if (safePos == null) safePos = findNearestSafeSurface(serverLevel2, 0, 0, 16);
                    if (safePos != null) entity.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                    else { serverLevel2.setBlockAndUpdate(new BlockPos(0, 99, 0), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()); entity.teleportTo(0.5, 100, 0.5); }
                    entity.fallDistance = 0;
                }
            });
        }
        if ((serverLevel2.getGameTime() + serverLevel2.dimension().hashCode()) % 40 != 0) return;
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel2.dimension());
        if (ownerId == null && !serverLevel2.players().isEmpty())
            ownerId = getOwnerUUIDFromPosition(serverLevel2, serverLevel2.players().iterator().next().getX(), serverLevel2.players().iterator().next().getZ());
        // Fallback for non-private shared dimensions: use the first player in the dimension
        if (ownerId == null && !serverLevel2.players().isEmpty())
            ownerId = serverLevel2.players().iterator().next().getUUID();
        if (ownerId != null) {
            UUID finalOwnerId = ownerId;
            PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get(serverLevel2).getOrCreateSettings(finalOwnerId);
            List<Entity> toRemove = new ArrayList<>();
            processTrackedPersonalEntities(serverLevel2, RULE_ENTITIES_PER_PASS, entity -> {
                boolean allowed = isAllowed(entity, finalOwnerId, serverLevel2, settings);
                if (!allowed) {
                    toRemove.add(entity);
                    return;
                }
                if (entity instanceof Player player && (settings.isDisableHunger() || Config.DISABLE_HUNGER.get()))
                    player.getFoodData().setFoodLevel(20);
                if (entity instanceof LivingEntity living && (settings.isNaturalHealing() || Config.NATURAL_HEALING.get()) && living.getHealth() < living.getMaxHealth())
                    living.heal(1.0f);
            });
            for (Entity entity : toRemove) processRemoval(entity);
            if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                if (settings.isLockDay()) serverLevel2.setDayTime(settings.getLockedDayTime());
                if (settings.isLockWeather()) applyWeather(serverLevel2, settings.isLockedWeatherRain(), settings.isLockedWeatherThunder());
            }
        }
    }

    @SubscribeEvent
    public void onLevelLoad(net.minecraftforge.event.level.LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (isOurDimension(serverLevel.dimension())) {
                for (Entity entity : serverLevel.getAllEntities()) {
                    trackPersonalEntity(entity);
                }
                StructurePlacer.tryPlaceStructure(serverLevel);
            }
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            clearLevelRuntimeState(serverLevel);
        }
    }

    private static void applyWeather(ServerLevel level, boolean rain, boolean thunder) {
        if (thunder) level.setWeatherParameters(0, 1000000, true, true);
        else if (rain) level.setWeatherParameters(0, 1000000, true, false);
        else level.setWeatherParameters(1000000, 0, false, false);
    }

    public static boolean isIsolatedBlock(Level level, BlockPos pos) {
        int solidCount = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (level.getBlockState(pos.offset(dx, 0, dz)).blocksMotion()) solidCount++;
        return solidCount < 3;
    }

    public static BlockPos findNearestSafeSurface(ServerLevel level, int startX, int startZ, int maxRadius) {
        int x = 0, z = 0, dx = 0, dz = -1;
        for (int i = 0; i < (maxRadius * 2 + 1) * (maxRadius * 2 + 1); i++) {
            int currentX = startX + x, currentZ = startZ + z;
            int y = findSafeSurfaceY(level, currentX, currentZ);
            if (y > level.getMinBuildHeight() && !isIsolatedBlock(level, new BlockPos(currentX, y - 1, currentZ)))
                return new BlockPos(currentX, y, currentZ);
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) { int t = dx; dx = -dz; dz = t; }
            x += dx; z += dz;
        }
        return null;
    }

    public static int findSafeSurfaceY(Level level, int x, int z) {
        for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).blocksMotion() && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir())
                return y;
        }
        return level.getMinBuildHeight() - 1;
    }

    public static void expelFromDomain(Entity entity, ServerLevel level, double centerX, double centerZ) {
        double angle = level.random.nextDouble() * 2 * Math.PI;
        double targetX = centerX + Math.cos(angle) * 45.0;
        double targetZ = centerZ + Math.sin(angle) * 45.0;
        int startY = (int) entity.getY(), maxY = level.getMaxBuildHeight() - 2, safeY = startY;
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos((int) targetX, startY, (int) targetZ);
        for (int y = startY; y <= maxY; y++) {
            check.set((int) targetX, y, (int) targetZ);
            if (level.getBlockState(check).isAir() && level.getBlockState(check.above()).isAir()) { safeY = y; break; }
        }
        entity.teleportTo(targetX, safeY, targetZ);
        if (entity instanceof LivingEntity living)
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));
    }

    private static UUID getOwnerIdFromEntityAndLevel(Entity entity, Level level) {
        if (entity != null && level instanceof ServerLevel serverLevel) {
            for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel.getEntitiesOfClass(
                    com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class, entity.getBoundingBox().inflate(32))) {
                if (domain.isUsingDimensionRules()) {
                    net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
                    if (aabb != null && aabb.contains(entity.position()))
                        return domain.getOwnerId();
                }
            }
            for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel.getEntitiesOfClass(
                    com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class, entity.getBoundingBox().inflate(10))) {
                if (domain.isUsingDimensionRules() && entity.position().distanceToSqr(domain.position()) <= 25)
                    return domain.getOwnerId();
            }
        }
        UUID ownerId = level instanceof ServerLevel sv ? getOwnerUUIDFromDimensionKey(level.dimension()) : null;
        if (ownerId == null && level instanceof ServerLevel serverLevel) {
            if (entity != null) ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
            else if (!serverLevel.players().isEmpty())
                ownerId = getOwnerUUIDFromPosition(serverLevel, serverLevel.players().iterator().next().getX(), serverLevel.players().iterator().next().getZ());
        }
        if (ownerId == null && entity != null) {
            if (entity instanceof Player p) ownerId = p.getUUID();
            else if (entity instanceof OwnableEntity ownable) ownerId = ownable.getOwnerUUID();
        }
        return ownerId;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCatReflexesDamage(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(CAT_REFLEXES_EFFECT.get())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity)) return;
        if (entity.level().random.nextFloat() < 0.20f) {
            event.setCanceled(true);
            if (entity instanceof CatFamiliarEntity cat && Config.CAT_FAMILIAR_TELEPORTS_TO_TARGET.get() && !entity.level().isClientSide) {
                ServerLevel lvl = (ServerLevel) entity.level();
                Vec3 oldPos = cat.position();
                double angle = lvl.random.nextDouble() * 2 * Math.PI;
                double distance = 0.5 + lvl.random.nextDouble() * 1.5;
                double tx = oldPos.x + Math.cos(angle) * distance, ty = oldPos.y, tz = oldPos.z + Math.sin(angle) * distance;
                BlockPos target = new BlockPos((int) tx, (int) ty, (int) tz);
                int safeY = cat.findSafeY(new BlockPos.MutableBlockPos(target.getX(), target.getY(), target.getZ()), lvl);
                if (safeY > lvl.getMinBuildHeight()) {
                    BlockPos finalPos = new BlockPos((int) tx, safeY, (int) tz);
                    boolean canFit = lvl.getBlockState(finalPos).getCollisionShape(lvl, finalPos).isEmpty()
                            && lvl.getBlockState(finalPos.above()).getCollisionShape(lvl, finalPos.above()).isEmpty();
                    if (canFit) {
                        CatFamiliarEntity.spawnWitchParticles(oldPos, lvl);
                        cat.teleportTo(tx, safeY, tz); cat.stopNavigation();
                        CatFamiliarEntity.spawnWitchParticles(cat.position(), lvl); return;
                    }
                }
                double fx = Math.floor(oldPos.x) + 0.5, fy = oldPos.y, fz = Math.floor(oldPos.z) + 0.5;
                double jitter = 0.3, fallbackAngle = lvl.random.nextDouble() * Math.PI * 2;
                fx += Math.cos(fallbackAngle) * jitter; fz += Math.sin(fallbackAngle) * jitter;
                CatFamiliarEntity.spawnWitchParticles(oldPos, lvl);
                cat.teleportTo(fx, fy, fz); cat.stopNavigation();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onMaidOrOwnerHurt(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        if (entity instanceof EntityMaid maid) {
            for (CatFamiliarEntity cat : serverLevel.getEntitiesOfClass(CatFamiliarEntity.class, maid.getBoundingBox().inflate(32))) {
                if (maid.getUUID().equals(cat.getMaidId())) {
                    applyCatReflexesToEntity(maid);
                    if (maid.getOwnerUUID() != null) {
                        Player owner = serverLevel.getPlayerByUUID(maid.getOwnerUUID());
                        if (owner != null && owner.distanceToSqr(maid) <= 32 * 32) applyCatReflexesToEntity(owner);
                        if (owner == null || !attacker.getUUID().equals(owner.getUUID())) applyBadLuckToEntity(attacker);
                    } else applyBadLuckToEntity(attacker);
                    break;
                }
            }
        } else if (entity instanceof Player player) {
            for (CatFamiliarEntity cat : serverLevel.getEntitiesOfClass(CatFamiliarEntity.class, player.getBoundingBox().inflate(32))) {
                UUID maidId = cat.getMaidId();
                if (maidId == null) continue;
                Entity maidEntity = serverLevel.getEntity(maidId);
                if (!(maidEntity instanceof EntityMaid maid)) continue;
                if (player.getUUID().equals(maid.getOwnerUUID())) {
                    applyCatReflexesToEntity(player); applyCatReflexesToEntity(maid); applyBadLuckToEntity(attacker); break;
                }
            }
        }
    }

    private void applyBadLuckToEntity(LivingEntity entity) {
        if (!entity.hasEffect(BAD_LUCK_EFFECT.get()))
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(BAD_LUCK_EFFECT.get(), 1200, 0, false, true, true));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBadLuckAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
        if (attacker == null) return;
        if (attacker.hasEffect(BAD_LUCK_EFFECT.get()))
            if (attacker.level().random.nextFloat() < com.tlmpersonal.tlmpersonaldimension.effect.BadLuckEffect.MISS_CHANCE)
                event.setCanceled(true);
    }

    private void applyCatReflexesToEntity(LivingEntity entity) {
        if (!entity.hasEffect(CAT_REFLEXES_EFFECT.get()))
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(CAT_REFLEXES_EFFECT.get(), 1200, 0, false, false, false));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onFelineGraceFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity.hasEffect(FELINE_GRACE_EFFECT.get()))
            event.setDamageMultiplier(event.getDamageMultiplier() * (1.0f - com.tlmpersonal.tlmpersonaldimension.effect.FelineGraceEffect.FALL_DAMAGE_REDUCTION));
    }

    @SubscribeEvent
    public void onLivingDamage(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide
                || !(isOurDimension(event.getEntity().level().dimension()) || isUnderDimensionRules(event.getEntity()))) return;
        LivingEntity target = event.getEntity();
        ServerLevel level = (ServerLevel) target.level();
        Entity attacker = event.getSource().getEntity();
        if (target.getHealth() - event.getAmount() <= 0
                && (Config.ALL_MAID_PROTECTION.get() || Config.WILD_MAID_PROTECTION.get())
                && target instanceof EntityMaid targetMaid && attacker != null) {
            UUID maidOwnerUUID = targetMaid.getOwnerUUID();
            boolean isWildMaid = maidOwnerUUID == null;
            if (isWildMaid || (Config.ALL_MAID_PROTECTION.get() && !attacker.getUUID().equals(maidOwnerUUID))) {
                event.setCanceled(true); target.setHealth(1.0f); processRemoval(attacker); return;
            }
        }
        UUID ownerId = getOwnerIdFromEntityAndLevel(target, level);
        if (ownerId == null) return;
        PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(ownerId);
        boolean isTargetOwner = target instanceof Player && ((Player) target).getUUID().equals(ownerId);
        boolean isTargetMaid = target instanceof EntityMaid;
        UUID maidActualOwner = isTargetMaid ? ((EntityMaid) target).getOwnerUUID() : null;
        boolean isTargetOwnerMaid = maidActualOwner != null && maidActualOwner.equals(ownerId);
        boolean isOwnerNearHisMaid = false;
        double range = Config.TAMED_MAID_PROTECTION_RANGE.get();
        if (target instanceof Player player) {
            for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(range)))
                if (e instanceof EntityMaid maid && player.getUUID().equals(maid.getOwnerUUID())) { isOwnerNearHisMaid = true; break; }
        } else if (isTargetMaid && maidActualOwner != null) {
            ServerPlayer ownerNearby = level.getServer() != null ? level.getServer().getPlayerList().getPlayer(maidActualOwner) : null;
            if (ownerNearby != null && ownerNearby.level() == level && ownerNearby.distanceTo(target) <= range) isOwnerNearHisMaid = true;
        }
        if (target.getHealth() - event.getAmount() <= 0) {
            if (isTargetMaid && (settings.isDisableMaidDeath() || Config.DISABLE_MAID_DEATH.get())) { event.setCanceled(true); target.setHealth(1.0f); return; }
            if (isTargetOwner && (settings.isDisablePlayerDeath() || Config.DISABLE_PLAYER_DEATH.get())) { event.setCanceled(true); target.setHealth(1.0f); return; }
            if (Config.TAMED_MAID_PROTECTION_ENABLED.get() && (settings.isTamedMaidProtection() || Config.TAMED_MAID_PROTECTION.get())
                    && (isOwnerNearHisMaid || isTargetOwnerMaid)) {
                UUID tamedOwnerId = isTargetOwnerMaid ? maidActualOwner : ownerId;
                if (attacker != null && !attacker.getUUID().equals(tamedOwnerId)) {
                    PersonalDimensionSavedData.PlayerDimensionSettings tamedSettings =
                            tamedOwnerId.equals(ownerId) ? settings : PersonalDimensionSavedData.get(level).getOrCreateSettings(tamedOwnerId);
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - tamedSettings.getLastTamedMaidProtectionUse() < Config.TAMED_MAID_PROTECTION_COOLDOWN.get() * 1000L) return;
                    ServerPlayer ownerPlayer = level.getServer() != null ? level.getServer().getPlayerList().getPlayer(tamedOwnerId) : null;
                    EntityMaid nearbyMaid = null;
                    for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(range)))
                        if (e instanceof EntityMaid maid && maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(tamedOwnerId)) { nearbyMaid = maid; break; }
                    if (nearbyMaid == null && isTargetOwnerMaid) nearbyMaid = (EntityMaid) target;
                    double powerCost = Config.TAMED_MAID_PROTECTION_POWER_POINTS_COST.get();
                    if (powerCost > 0.0 && (nearbyMaid == null || nearbyMaid.getExperience() < (int) Math.round(powerCost))) return;
                    int xpCost = Config.TAMED_MAID_PROTECTION_XP_COST.get();
                    if (xpCost > 0 && (ownerPlayer == null || ownerPlayer.experienceLevel < xpCost)) return;
                    if (nearbyMaid != null && powerCost > 0.0) nearbyMaid.setExperience(nearbyMaid.getExperience() - (int) Math.round(powerCost));
                    if (ownerPlayer != null && xpCost > 0) ownerPlayer.giveExperienceLevels(-xpCost);
                    tamedSettings.setLastTamedMaidProtectionUse(currentTime);
                    PersonalDimensionSavedData.get(level).setDirty();
                    event.setCanceled(true); target.setHealth(1.0f); processRemoval(attacker); return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Level level = livingEntity.level();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        
        // Handle maid-specific logic
        if (livingEntity instanceof EntityMaid maid && maid.getOwnerUUID() != null) {
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(serverLevel);
            if (serverLevel.getGameTime() % 100 == 0) {
                PersonalDimensionSavedData.MaidInfo oldInfo = savedData.getTrackedMaids().get(maid.getUUID());
                if (oldInfo == null || !oldInfo.lastLevel.equals(serverLevel.dimension())
                        || Math.abs(oldInfo.lastX - maid.getX()) > 1.0
                        || Math.abs(oldInfo.lastY - maid.getY()) > 1.0
                        || Math.abs(oldInfo.lastZ - maid.getZ()) > 1.0) {
                    savedData.getTrackedMaids().put(maid.getUUID(), new PersonalDimensionSavedData.MaidInfo(
                            maid.getOwnerUUID(), serverLevel.dimension(), maid.getX(), maid.getY(), maid.getZ()));
                    savedData.setDirty();
                }
            }
            
            if (serverLevel.getGameTime() % 10 == 0) {
                UUID ownerId2 = maid.getOwnerUUID();
                PersonalDimensionSavedData.PlayerDimensionSettings lightSettings = savedData.getOrCreateSettings(ownerId2);
                updateMaidLight(maid, serverLevel, lightSettings.isMaidEmitLight() || Config.MAID_EMIT_LIGHT.get());
            }
        }
        
        // Handle entity protection logic in personal dimensions
        if (!(isOurDimension(level.dimension()) || isUnderDimensionRules(livingEntity))) return;
        
        UUID ownerId = getOwnerIdFromEntityAndLevel(livingEntity, level);
        if (ownerId != null) {
            PersonalDimensionSavedData settingsData = PersonalDimensionSavedData.get(serverLevel);
            PersonalDimensionSavedData.PlayerDimensionSettings settings = settingsData.getOrCreateSettings(ownerId);
            
            // Maid attack discard logic
            if (livingEntity instanceof EntityMaid maid && (settings.isMaidAttackDiscard() || Config.MAID_ATTACK_DISCARD.get())) {
                LivingEntity target = maid.getTarget();
                if (target != null && target.isAlive() && !target.getUUID().equals(ownerId) && !(target instanceof EntityMaid)) {
                    target.discard(); 
                    maid.setTarget(null);
                }
            }
            
            // Entity cannot target logic
            if (settings.isEntityCannotTarget() || Config.ENTITY_CANNOT_TARGET.get()) {
                if (livingEntity instanceof Mob mob && !(mob instanceof EntityMaid)) {
                    if (mob.getTarget() != null || mob.getLastHurtByMob() != null) {
                        mob.setTarget(null);
                        mob.targetSelector.getAvailableGoals().removeIf(w -> w.getGoal() instanceof NearestAttackableTargetGoal<?>);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onMobEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        Level level = event.getEntity().level();
        if (level.isClientSide || !(isOurDimension(level.dimension()) || isUnderDimensionRules(event.getEntity()))) return;
        Entity target = event.getEntity();
        UUID ownerId = getOwnerIdFromEntityAndLevel(target, level);
        if (ownerId == null) return;
        PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get((ServerLevel) level).getOrCreateSettings(ownerId);
        if ((settings.isBlockHarmfulEffects() || Config.BLOCK_HARMFUL_EFFECTS.get())
                && (target instanceof EntityMaid || (target instanceof Player && target.getUUID().equals(ownerId)))
                && !event.getEffectInstance().getEffect().isBeneficial())
            // Forge 1.20.1: @HasResult event — DENY blocks the effect
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }

    @SubscribeEvent
    public void onMaidDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        net.minecraft.server.MinecraftServer server = ((ServerLevel) maid.level()).getServer();
        removeMaidLight(maid.getUUID(), server);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide || !(isOurDimension(level.dimension()) || isUnderDimensionRules(event.getEntity()))) return;
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) return;
        UUID ownerId = getOwnerIdFromEntityAndLevel(victim, level);
        if (ownerId == null) return;
        PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get((ServerLevel) level).getOrCreateSettings(ownerId);
        if (attacker instanceof EntityMaid maid && (settings.isMaidAttackDiscard() || Config.MAID_ATTACK_DISCARD.get())) {
            if (victim.isAlive() && !victim.getUUID().equals(ownerId) && !(victim instanceof EntityMaid)) { victim.discard(); maid.setTarget(null); return; }
        }
        if (!(victim instanceof Player) && !(victim instanceof EntityMaid)) return;
        double range = Config.TAMED_MAID_PROTECTION_RANGE.get();
        for (Entity e : victim.level().getEntities(victim, victim.getBoundingBox().inflate(range)))
            if ((settings.isMaidAuthority() || Config.MAID_AUTHORITY.get()) && e instanceof Mob mob && mob != attacker && mob != victim && mob.isAlive())
                mob.setLastHurtByMob(attacker);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpawnPlacementCheck(net.minecraftforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel() instanceof ServerLevel level && isOurDimension(level.dimension())) {
            BlockPos pos = event.getPos();
            UUID ownerId = getOwnerUUIDFromDimensionKey(level.dimension());
            if (ownerId == null) ownerId = getOwnerUUIDFromPosition(level, pos.getX(), pos.getZ());
            if (!isAllowed(event.getEntityType(), ownerId, level, null))
                // Forge 1.20.1: @HasResult event — DENY blocks the spawn
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if ((isOurDimension(event.getEntity().level().dimension()) || isUnderDimensionRules(event.getEntity()))
                && !Config.ALLOW_SET_SPAWN.get()) event.setCanceled(true);
    }

    private static boolean hasTetheredTeleportBauble(EntityMaid maid) {
        if (maid.getMaidBauble() == null) return false;
        for (int i = 0; i < maid.getMaidBauble().getSlots(); i++)
            if (maid.getMaidBauble().getStackInSlot(i).is(TETHERED_TELEPORT_BAUBLE.get())) return true;
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof Player player && isOurDimension(player.level().dimension())) {
            if (!Config.DIMENSION_WHITELIST.get().contains(event.getDimension().location().toString())) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("This dimension is not whitelisted!"));
            }
        }
        if (event.getEntity() instanceof ServerPlayer ownerPlayer) {
            ServerLevel currentLevel = (ServerLevel) ownerPlayer.level();
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(currentLevel.getServer().getLevel(Level.OVERWORLD));
            ResourceKey<Level> targetDim = event.getDimension();
            for (Map.Entry<UUID, PersonalDimensionSavedData.MaidInfo> entry : savedData.getTrackedMaids().entrySet()) {
                PersonalDimensionSavedData.MaidInfo maidInfo = entry.getValue();
                if (!maidInfo.ownerUuid.equals(ownerPlayer.getUUID())) continue;
                UUID maidUuid = entry.getKey();
                if (maidInfo.lastLevel.equals(targetDim)) continue;
                EntityMaid maid = null;
                for (ServerLevel lvl : currentLevel.getServer().getAllLevels()) {
                    Entity e = lvl.getEntity(maidUuid);
                    if (e instanceof EntityMaid found) { maid = found; break; }
                }
                boolean shouldTeleport = Config.MAID_TELEPORT_WITH_OWNER_DIMENSION.get();
                if (!shouldTeleport && maid != null && hasTetheredTeleportBauble(maid) && !maid.isOrderedToSit() && !maid.isHomeModeEnable())
                    shouldTeleport = true;
                if (shouldTeleport && MAIDS_PENDING_TELEPORT.add(maidUuid))
                    MAIDS_TO_TELEPORT.add(new MaidTeleportData(maidUuid, ownerPlayer.getUUID(), targetDim, 0));
            }
        }
    }

    public static void enqueueMaidTeleport(UUID maidUuid, UUID ownerUuid, ResourceKey<Level> targetDim) {
        if (MAIDS_PENDING_TELEPORT.add(maidUuid)) MAIDS_TO_TELEPORT.add(new MaidTeleportData(maidUuid, ownerUuid, targetDim, 0));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        if (isOurDimension(level.dimension()) && !Config.ENABLE_BLOCK_BREAKING.get()) { event.setCanceled(true); return; }
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class, new AABB(pos).inflate(200))) {
            net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
            boolean inDomain = aabb != null && aabb.contains(pos.getX(), pos.getY(), pos.getZ());
            if (inDomain && domain.isUsingDimensionRules() && !Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING.get()) { event.setCanceled(true); return; }
        }
        for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class, new AABB(pos).inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                int hR = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get(), vH = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                int dx = pos.getX() - domain.blockPosition().getX(), dy = pos.getY() - domain.blockPosition().getY(), dz = pos.getZ() - domain.blockPosition().getZ();
                if (dx * dx + dz * dz <= hR * hR && Math.abs(dy) <= vH && !Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING.get()) { event.setCanceled(true); return; }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        if (isOurDimension(level.dimension()) && !Config.ENABLE_BLOCK_BUILDING.get()) { event.setCanceled(true); return; }
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class, new AABB(pos).inflate(200))) {
            net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
            boolean inDomain = aabb != null && aabb.contains(pos.getX(), pos.getY(), pos.getZ());
            if (inDomain && domain.isUsingDimensionRules() && !Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING.get()) { event.setCanceled(true); return; }
        }
        for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel.getEntitiesOfClass(
                com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class, new AABB(pos).inflate(200))) {
            if (domain.isUsingDimensionRules()) {
                int hR = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get(), vH = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                int dx = pos.getX() - domain.blockPosition().getX(), dy = pos.getY() - domain.blockPosition().getY(), dz = pos.getZ() - domain.blockPosition().getZ();
                if (dx * dx + dz * dz <= hR * hR && Math.abs(dy) <= vH && !Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING.get()) { event.setCanceled(true); return; }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel serverLevel)) return;
        UUID playerId = player.getUUID();
        for (ServerLevel lvl : serverLevel.getServer().getAllLevels()) {
            List<com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity> toRestore = new ArrayList<>();
            for (Entity e : lvl.getAllEntities())
                if (e instanceof com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain && playerId.equals(domain.getOwnerId()))
                    toRestore.add(domain);
            toRestore.forEach(com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity::restoreAndDiscard);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (isOurDimension(level.dimension()) && !Config.ENABLE_BLOCK_BREAKING.get()) { event.getAffectedBlocks().clear(); return; }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            event.getAffectedBlocks().removeIf(pos -> {
                for (com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity domain : serverLevel.getEntitiesOfClass(
                        com.tlmpersonal.tlmpersonaldimension.entity.DomainExpansionEntity.class, new AABB(pos).inflate(200))) {
                    if (!domain.isUsingDimensionRules() || Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING.get()) continue;
                    net.minecraft.world.phys.AABB aabb = domain.getStructureAABB();
                    if (aabb != null && aabb.contains(pos.getX(), pos.getY(), pos.getZ())) return true;
                }
                for (com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity domain : serverLevel.getEntitiesOfClass(
                        com.tlmpersonal.tlmpersonaldimension.entity.CherryDomainEntity.class, new AABB(pos).inflate(200))) {
                    if (!domain.isUsingDimensionRules() || Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING.get()) continue;
                    int hR = Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get(), vH = Config.CHERRY_DOMAIN_VERTICAL_HALF.get();
                    int dx = pos.getX() - domain.blockPosition().getX(), dy = pos.getY() - domain.blockPosition().getY(), dz = pos.getZ() - domain.blockPosition().getZ();
                    if (dx * dx + dz * dz <= hR * hR && Math.abs(dy) <= vH) return true;
                }
                return false;
            });
        }
    }
}
