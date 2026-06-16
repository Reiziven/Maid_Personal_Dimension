package com.tlmpersonal.tlmpersonaldimension;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.info.ServerCustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
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
import net.minecraft.world.entity.OwnableEntity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
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
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
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
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    private static final Map<ResourceKey<Level>, ConcurrentLinkedQueue<BlockPos>> PLACEMENT_QUEUE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> PROCESSED_CHUNKS = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<BlockPos>> GENERATED_ISLANDS = new HashMap<>();

    public static final ResourceKey<Level> PERSONAL_DIMENSION_VOID_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "personal_dimension"));
    public static final ResourceKey<Level> PERSONAL_DIMENSION_NORMAL_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "personal_dimension_normal"));
    public static final ResourceKey<Level> PERSONAL_DIMENSION_CHERRY_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "personal_dimension_cherry"));

    public static ResourceKey<Level> getCurrentPersonalDimensionKey() {
        return switch (Config.DIMENSION_TYPE.get()) {
            case VOID -> PERSONAL_DIMENSION_VOID_KEY;
            case NORMAL -> PERSONAL_DIMENSION_NORMAL_KEY;
            case CHERRY -> PERSONAL_DIMENSION_CHERRY_KEY;
        };
    }

    public static boolean isOurDimension(ResourceKey<Level> dim) {
        String path = dim.location().getPath();
        return path.startsWith("personal_dimension") && dim.location().getNamespace().equals(MODID);
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
        } catch (Exception ignored) {}
        return null;
    }

    public record TeleportLocation(double x, double y, double z, float yRot, float xRot) {}
    public static final Map<UUID, Map<ResourceKey<Level>, TeleportLocation>> TELEPORT_HISTORY = new HashMap<>();

    public static final DeferredItem<MaidTeleporter> MAID_TELEPORTER = ITEMS.register("maid_teleporter", () -> new MaidTeleporter(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> TAB_ICON = ITEMS.register("tab_icon", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PERSONAL_DIMENSION_TAB = CREATIVE_MODE_TABS.register("personal_dimension_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID + ".personal_dimension_tab"))
            .icon(() -> new ItemStack(TAB_ICON.get()))
            .displayItems((parameters, output) -> {
                output.accept(MAID_TELEPORTER.get());
            }).build());

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalDimensionMenu>> PERSONAL_DIMENSION_MENU = MENUS.register("personal_dimension_menu", () -> IMenuTypeExtension.create((windowId, inv, data) -> new PersonalDimensionMenu(windowId, inv, data.readInt())));

    public Touhoulittlemaidpersonaldimension(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new TouhoulittlemaidpersonaldimensionClient(modContainer);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket.TYPE, com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket.STREAM_CODEC, com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket::handle);
        registrar.playToServer(com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket.TYPE, com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket.STREAM_CODEC, com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket::handle);
        registrar.playToClient(PersonalDimensionSettingsSyncPacket.TYPE, PersonalDimensionSettingsSyncPacket.STREAM_CODEC, PersonalDimensionSettingsSyncPacket::handle);
        registrar.playToClient(TeleporterCooldownSyncPacket.TYPE, TeleporterCooldownSyncPacket.STREAM_CODEC, TeleporterCooldownSyncPacket::handle);
    }

    private void commonSetup(FMLCommonSetupEvent event) {}

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
        PlayerDimensionManager.clearCache();
        PlayerDimensionManager.preloadPersistedPersonalDimensionState(event.getServer());
    }

    public static boolean isPlayerAllowed(Player player, UUID ownerId, ServerLevel level, PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        UUID finalOwnerId = ownerId;
        if (finalOwnerId == null && level != null) {
            finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
            if (finalOwnerId == null) finalOwnerId = getOwnerUUIDFromPosition(level, player.getX(), player.getZ());
        }
        if (finalOwnerId != null && player.getUUID().equals(finalOwnerId)) return true;
        if (settings == null && finalOwnerId != null && level != null) settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
        if (settings != null) {
            String playerName = player.getGameProfile().getName();
            String playerUuidStr = player.getUUID().toString();
            for (String allowed : settings.getAllowedPlayers()) {
                if (allowed.equalsIgnoreCase(playerName) || allowed.equals(playerUuidStr)) return true;
            }
        }
        return !Config.PRIVATE_DIMENSION.get() || finalOwnerId == null;
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
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static boolean isAllowed(Entity entity, UUID ownerId, ServerLevel level, PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        if (entity instanceof Player player) return isPlayerAllowed(player, ownerId, level, settings);
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String idString = entityId.toString();
        if (idString.equals("touhou_little_maid:maid")) return true;
        if (settings == null) {
            UUID finalOwnerId = ownerId;
            if (finalOwnerId == null && level != null) {
                finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
                if (finalOwnerId == null) finalOwnerId = getOwnerUUIDFromPosition(level, entity.getX(), entity.getZ());
            }
            if (finalOwnerId != null && level != null) settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
        }
        if (settings != null) {
            if (settings.getBlockedEntities().contains(idString) || Config.BLOCKED_ENTITIES.get().contains(idString)) return false;
            if ((settings.isDisableHostileEntities() || Config.DISABLE_HOSTILE_ENTITIES.get()) && entity.getType().getCategory() == MobCategory.MONSTER) return false;
        } else {
            if (Config.BLOCKED_ENTITIES.get().contains(idString)) return false;
            if (Config.DISABLE_HOSTILE_ENTITIES.get() && entity.getType().getCategory() == MobCategory.MONSTER) return false;
        }
        if ((entity instanceof ItemEntity || entity instanceof Projectile) || (entity.getType().getCategory() == MobCategory.MISC && !(entity instanceof LivingEntity))) return true;
        if (settings != null && (settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() || settings.getAllowedEntities().contains(idString) || Config.ALLOWED_ENTITIES.get().contains(idString))) return true;
        if (Config.PRIVATE_DIMENSION.get()) {
            if (settings == null) return false;
            return settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() || settings.getAllowedEntities().contains(idString) || Config.ALLOWED_ENTITIES.get().contains(idString);
        }
        if (Config.ENTITY_WHITELIST_MODE.get()) {
            if (Config.ALLOWED_ENTITIES.get().contains(idString)) return true;
            return Config.ALLOW_ALL_ENTITIES.get();
        }
        return Config.ALLOW_ALL_ENTITIES.get();
    }

    public static boolean isAllowed(EntityType<?> entityType, UUID ownerId, ServerLevel level, PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        if (entityType == EntityType.PLAYER) return true;
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        String idString = entityId.toString();
        if (idString.equals("touhou_little_maid:maid")) return true;
        if (settings == null) {
            UUID finalOwnerId = ownerId;
            if (finalOwnerId == null && level != null) finalOwnerId = getOwnerUUIDFromDimensionKey(level.dimension());
            if (finalOwnerId != null && level != null) settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(finalOwnerId);
        }
        if (settings != null) {
            if (settings.getBlockedEntities().contains(idString) || Config.BLOCKED_ENTITIES.get().contains(idString)) return false;
            if ((settings.isDisableHostileEntities() || Config.DISABLE_HOSTILE_ENTITIES.get()) && entityType.getCategory() == MobCategory.MONSTER) return false;
        } else {
            if (Config.BLOCKED_ENTITIES.get().contains(idString)) return false;
            if (Config.DISABLE_HOSTILE_ENTITIES.get() && entityType.getCategory() == MobCategory.MONSTER) return false;
        }
        if ((entityType.getCategory() == MobCategory.MISC && !idString.equals("minecraft:bee") && !idString.equals("minecraft:villager")) || idString.equals("minecraft:item") || idString.equals("minecraft:experience_orb") || idString.equals("minecraft:arrow") || idString.equals("minecraft:snowball") || idString.equals("minecraft:egg") || idString.equals("minecraft:ender_pearl") || idString.equals("minecraft:splash_potion") || idString.equals("minecraft:lingering_potion") || idString.equals("minecraft:firework_rocket") || idString.equals("minecraft:fireball") || idString.equals("minecraft:small_fireball") || idString.equals("minecraft:dragon_fireball") || idString.equals("minecraft:wither_skull") || idString.equals("minecraft:llama_spit") || idString.equals("minecraft:shulker_bullet") || idString.equals("minecraft:trident")) return true;
        if (settings != null && (settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() || settings.getAllowedEntities().contains(idString) || Config.ALLOWED_ENTITIES.get().contains(idString))) return true;
        if (Config.PRIVATE_DIMENSION.get()) {
            if (settings == null) return false;
            return settings.isAllowAllEntities() || Config.ALLOW_ALL_ENTITIES.get() || settings.getAllowedEntities().contains(idString) || Config.ALLOWED_ENTITIES.get().contains(idString);
        }
        if (Config.ENTITY_WHITELIST_MODE.get()) {
            if (Config.ALLOWED_ENTITIES.get().contains(idString)) return true;
            return Config.ALLOW_ALL_ENTITIES.get();
        }
        return Config.ALLOW_ALL_ENTITIES.get();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !isOurDimension(entity.level().dimension())) return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null) ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
        if (!isAllowed(entity, ownerId, serverLevel, null)) {
            event.setCanceled(true);
            entity.discard();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !isOurDimension(entity.level().dimension())) return;
        if (event.getSpawnType() == MobSpawnType.CONVERSION) return;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null) ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
        if (!isAllowed(entity, ownerId, serverLevel, null)) {
            event.setSpawnCancelled(true);
            entity.discard();
        }
    }

    public static void processRemoval(Entity entity) {
        if (Config.REMOVE_BLOCKED_ENTITIES.get()) {
            if (entity instanceof ServerPlayer player) {
                ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    TeleportLocation savedPos = getEntityPosition(player.getUUID(), Level.OVERWORLD);
                    if (savedPos != null) player.teleportTo(overworld, savedPos.x(), savedPos.y(), savedPos.z(), Set.of(), savedPos.yRot(), savedPos.xRot());
                    else {
                        BlockPos spawn = overworld.getSharedSpawnPos();
                        player.teleportTo(overworld, (double)spawn.getX() + 0.5, (double)spawn.getY() + 1, (double)spawn.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot());
                    }
                    player.sendSystemMessage(Component.literal("You are not allowed here!"));
                }
            } else {
                entity.discard();
            }
            return;
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
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
                if (savedPos != null) entity.teleportTo(overworld, savedPos.x(), savedPos.y(), savedPos.z(), Set.of(), savedPos.yRot(), savedPos.xRot());
                else {
                    BlockPos spawn = overworld.getSharedSpawnPos();
                    entity.teleportTo(overworld, (double)spawn.getX() + 0.5, (double)spawn.getY() + 1, (double)spawn.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot());
                }
                if (entity instanceof ServerPlayer player) player.sendSystemMessage(Component.literal("You are not allowed here!"));
                return;
            }
        }
        entity.discard();
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(serverLevel.getServer().getLevel(Level.OVERWORLD));
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof EntityMaid maid && maid.getOwnerUUID() != null) {
                    savedData.getTrackedMaids().put(maid.getUUID(), new PersonalDimensionSavedData.MaidInfo(maid.getOwnerUUID(), maid.level().dimension(), maid.getX(), maid.getY(), maid.getZ()));
                }
            }
            savedData.setDirty();
            while (!MAIDS_TO_TELEPORT.isEmpty()) {
                MaidTeleportData data = MAIDS_TO_TELEPORT.poll();
                if (data == null) continue;
                ServerLevel targetLevel = serverLevel.getServer().getLevel(data.targetDim());
                if (targetLevel == null) continue;
                ServerPlayer targetOwner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUuid());
                if (targetOwner == null) continue;
                if (!targetOwner.level().dimension().equals(data.targetDim())) {
                    MAIDS_TO_TELEPORT.add(data);
                    continue;
                }
                EntityMaid maid = null;
                for (ServerLevel levelIter : serverLevel.getServer().getAllLevels()) {
                    Entity entity = levelIter.getEntity(data.maidUuid());
                    if (entity instanceof EntityMaid foundMaid) {
                        maid = foundMaid;
                        break;
                    }
                }
                if (maid == null) continue;
                maid.teleportTo(targetLevel, targetOwner.getX(), targetOwner.getY(), targetOwner.getZ(), Set.of(), targetOwner.getYRot(), targetOwner.getXRot());
            }
        }
        if (!(level instanceof ServerLevel serverLevel) || !isOurDimension(level.dimension())) return;
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
                        if (serverLevel.canSeeSky(spawnPos) && !serverLevel.getFluidState(spawnPos.below()).isSource()) {
                            EntityMaid maid = InitEntities.MAID.get().create(serverLevel);
                            if (maid != null) {
                                if (Config.USE_YSM_MODELS.get()) {
                                    List<? extends String> modelIdsList = Config.YSM_MODEL_IDS.get();
                                    if (!modelIdsList.isEmpty()) {
                                        maid.setIsYsmModel(true);
                                        maid.setYsmModel(modelIdsList.get(serverLevel.random.nextInt(modelIdsList.size())), "", net.minecraft.network.chat.Component.literal(""));
                                    }
                                } else {
                                    int modelSize = ServerCustomPackLoader.SERVER_MAID_MODELS.getModelSize();
                                    if (modelSize > 0) ServerCustomPackLoader.SERVER_MAID_MODELS.getModelIdSet().stream().skip(serverLevel.random.nextInt(modelSize)).findFirst().ifPresent(maid::setModelId);
                                }
                                maid.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.NATURAL, null);
                                maid.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, serverLevel.random.nextFloat() * 360.0F, 0.0F);
                                serverLevel.addFreshEntity(maid);
                            }
                        }
                    }
                }
            }
        }
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity.getY() < Config.FALL_PROTECTION_Y.get()) {
                int safeY = findSafeSurfaceY(serverLevel, (int) entity.getX(), (int) entity.getZ());
                if (safeY <= serverLevel.getMinBuildHeight()) {
                    BlockPos spawn = serverLevel.getSharedSpawnPos();
                    entity.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                } else entity.teleportTo(entity.getX(), safeY, entity.getZ());
                entity.fallDistance = 0;
            }
        }
        if ((serverLevel.getGameTime() + serverLevel.dimension().hashCode()) % 40 != 0) return;
        UUID ownerId = getOwnerUUIDFromDimensionKey(serverLevel.dimension());
        if (ownerId == null && !serverLevel.players().isEmpty()) ownerId = getOwnerUUIDFromPosition(serverLevel, serverLevel.players().iterator().next().getX(), serverLevel.players().iterator().next().getZ());
        if (ownerId != null) {
            PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get(serverLevel).getOrCreateSettings(ownerId);
            List<Entity> toRemove = new ArrayList<>();
            for (Entity entity : serverLevel.getAllEntities()) {
                if (!isAllowed(entity, ownerId, serverLevel, settings)) {
                    toRemove.add(entity);
                    if (toRemove.size() > 100) break;
                    continue;
                }
                if (entity instanceof Player player && (settings.isDisableHunger() || Config.DISABLE_HUNGER.get())) player.getFoodData().setFoodLevel(20);
                if (entity instanceof LivingEntity living && (settings.isNaturalHealing() || Config.NATURAL_HEALING.get()) && living.getHealth() < living.getMaxHealth()) living.heal(1.0f);
                if (entity instanceof EntityMaid maid && (settings.isMaidEmitLight() || Config.MAID_EMIT_LIGHT.get())) {
                    BlockPos pos = maid.blockPosition();
                    if (level.getBlockState(pos).isAir()) level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState());
                }
            }
            for (Entity entity : toRemove) Touhoulittlemaidpersonaldimension.processRemoval(entity);
            if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                if (settings.isLockDay()) serverLevel.setDayTime(settings.getLockedDayTime());
                if (settings.isLockWeather()) applyWeather(serverLevel, settings.isLockedWeatherRain(), settings.isLockedWeatherThunder());
            }
        }
    }

    private static void applyWeather(ServerLevel level, boolean rain, boolean thunder) {
        if (thunder) level.setWeatherParameters(0, 1000000, true, true);
        else if (rain) level.setWeatherParameters(0, 1000000, true, false);
        else level.setWeatherParameters(1000000, 0, false, false);
    }

    public static int findSafeSurfaceY(Level level, int x, int z) {
        int maxY = level.getMaxBuildHeight() - 1;
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, maxY, z);
        while (mutablePos.getY() >= minY) {
            if (level.getBlockState(mutablePos).blocksMotion() && !level.getBlockState(mutablePos).is(BlockTags.LEAVES) && level.getBlockState(mutablePos.above()).isAir() && level.getBlockState(mutablePos.above(2)).isAir()) return mutablePos.getY() + 1;
            mutablePos.move(0, -1, 0);
        }
        return minY - 1;
    }

    private static UUID getOwnerIdFromEntityAndLevel(Entity entity, Level level) {
        UUID ownerId = level instanceof ServerLevel ? getOwnerUUIDFromDimensionKey(level.dimension()) : null;
        if (ownerId == null && level instanceof ServerLevel serverLevel) {
            if (entity != null) ownerId = getOwnerUUIDFromPosition(serverLevel, entity.getX(), entity.getZ());
            else if (!serverLevel.players().isEmpty()) ownerId = getOwnerUUIDFromPosition(serverLevel, serverLevel.players().iterator().next().getX(), serverLevel.players().iterator().next().getZ());
        }
        if (ownerId == null && entity != null) {
            if (entity instanceof Player p) ownerId = p.getUUID();
            else if (entity instanceof OwnableEntity ownable) ownerId = ownable.getOwnerUUID();
        }
        return ownerId;
    }

    @SubscribeEvent
    public void onLivingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !isOurDimension(event.getEntity().level().dimension())) return;
        LivingEntity target = event.getEntity();
        ServerLevel level = (ServerLevel) target.level();
        UUID ownerId = getOwnerIdFromEntityAndLevel(target, level);
        if (ownerId == null) return;
        PersonalDimensionSavedData.PlayerDimensionSettings settings = PersonalDimensionSavedData.get(level).getOrCreateSettings(ownerId);
        boolean isTargetOwner = target instanceof Player && ((Player)target).getUUID().equals(ownerId);
        boolean isTargetOwnerMaid = target instanceof EntityMaid maid && maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(ownerId);
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
            if (Config.TAMED_MAID_PROTECTION_ENABLED.get() && (settings.isTamedMaidProtection() || Config.TAMED_MAID_PROTECTION.get()) && (isOwnerNearHisMaid || isTargetOwnerMaid)) {
                if (attacker != null && !attacker.getUUID().equals(ownerId)) {
                    long currentTime = System.currentTimeMillis();
                    long cooldownMs = Config.TAMED_MAID_PROTECTION_COOLDOWN.get() * 1000L;
                    if (currentTime - settings.getLastTamedMaidProtectionUse() < cooldownMs) return;
                    ServerPlayer ownerPlayer = level.getServer() != null ? level.getServer().getPlayerList().getPlayer(ownerId) : null;
                    EntityMaid nearbyMaid = null;
                    double range = Config.TAMED_MAID_PROTECTION_RANGE.get();
                    for (Entity entity : level.getEntities(target, target.getBoundingBox().inflate(range))) {
                        if (entity instanceof EntityMaid maid && maid.getOwnerUUID() != null && maid.getOwnerUUID().equals(ownerId)) {
                            nearbyMaid = maid;
                            break;
                        }
                    }
                    if (nearbyMaid == null && isTargetOwnerMaid) nearbyMaid = (EntityMaid) target;
                    double powerCost = Config.TAMED_MAID_PROTECTION_POWER_POINTS_COST.get();
                    if (powerCost > 0.0 && (nearbyMaid == null || nearbyMaid.getExperience() < (int) Math.round(powerCost))) return;
                    int xpCost = Config.TAMED_MAID_PROTECTION_XP_COST.get();
                    if (xpCost > 0 && (ownerPlayer == null || ownerPlayer.experienceLevel < xpCost)) return;
                    if (nearbyMaid != null && powerCost > 0.0) nearbyMaid.setExperience(nearbyMaid.getExperience() - (int) Math.round(powerCost));
                    if (ownerPlayer != null && xpCost > 0) ownerPlayer.giveExperienceLevels(-xpCost);
                    settings.setLastTamedMaidProtectionUse(currentTime);
                    PersonalDimensionSavedData.get(level).setDirty();
                    event.setCanceled(true);
                    target.setHealth(1.0f);
                    Touhoulittlemaidpersonaldimension.processRemoval(attacker);
                    return;
                }
            }
            if ((Config.ALL_MAID_PROTECTION.get() || Config.WILD_MAID_PROTECTION.get()) && isTargetMaid && !isTargetOwnerMaid && attacker != null && !attacker.getUUID().equals(ownerId)) {
                event.setCanceled(true);
                target.setHealth(1.0f);
                Touhoulittlemaidpersonaldimension.processRemoval(attacker);
            }
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Level level = event.getEntity().level();
        Entity entity = event.getEntity();
        if (level instanceof ServerLevel serverLevel && entity instanceof EntityMaid maid && maid.getOwnerUUID() != null) {
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(serverLevel);
            savedData.getTrackedMaids().put(maid.getUUID(), new PersonalDimensionSavedData.MaidInfo(maid.getOwnerUUID(), serverLevel.dimension(), maid.getX(), maid.getY(), maid.getZ()));
            savedData.setDirty();
        }
        if (level.isClientSide || !isOurDimension(level.dimension())) return;
        UUID ownerId = getOwnerIdFromEntityAndLevel(entity, level);
        if (ownerId != null) {
            PersonalDimensionSavedData settingsData = PersonalDimensionSavedData.get((ServerLevel) level);
            PersonalDimensionSavedData.PlayerDimensionSettings settings = settingsData.getOrCreateSettings(ownerId);
            if (entity instanceof EntityMaid maid && (settings.isMaidAttackDiscard() || Config.MAID_ATTACK_DISCARD.get())) {
                LivingEntity target = maid.getTarget();
                if (target != null && target.isAlive() && !target.getUUID().equals(ownerId) && !(target instanceof EntityMaid)) {
                    target.discard();
                    maid.setTarget(null);
                }
            }
            if (settings.isEntityCannotTarget() || Config.ENTITY_CANNOT_TARGET.get()) {
                if (entity instanceof Mob mob && !(mob instanceof EntityMaid)) {
                    if (mob.getTarget() != null || mob.getLastHurtByMob() != null) {
                        mob.setTarget(null);
                        mob.targetSelector.getAvailableGoals().removeIf(wrapped -> wrapped.getGoal() instanceof NearestAttackableTargetGoal<?>);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        Level level = event.getEntity().level();
        if (level.isClientSide || !isOurDimension(level.dimension())) return;
        Entity target = event.getEntity();
        UUID ownerId = getOwnerIdFromEntityAndLevel(target, level);
        if (ownerId == null) return;
        PersonalDimensionSavedData settingsData = PersonalDimensionSavedData.get((ServerLevel)level);
        PersonalDimensionSavedData.PlayerDimensionSettings settings = settingsData.getOrCreateSettings(ownerId);
        if ((settings.isBlockHarmfulEffects() || Config.BLOCK_HARMFUL_EFFECTS.get()) && (target instanceof EntityMaid || (target instanceof Player && target.getUUID().equals(ownerId))) && !event.getEffectInstance().getEffect().value().isBeneficial()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingDamageEvent.Post event) {
        Level level = event.getEntity().level();
        if (level.isClientSide || !isOurDimension(level.dimension())) return;
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) return;
        UUID ownerId = getOwnerIdFromEntityAndLevel(victim, level);
        if (ownerId == null) return;
        PersonalDimensionSavedData settingsData = PersonalDimensionSavedData.get((ServerLevel) level);
        PersonalDimensionSavedData.PlayerDimensionSettings settings = settingsData.getOrCreateSettings(ownerId);
        if (attacker instanceof EntityMaid maid && (settings.isMaidAttackDiscard() || Config.MAID_ATTACK_DISCARD.get())) {
            if (victim.isAlive() && !victim.getUUID().equals(ownerId) && !(victim instanceof EntityMaid)) {
                victim.discard();
                maid.setTarget(null);
                return;
            }
        }
        if (!(victim instanceof Player) && !(victim instanceof EntityMaid)) return;
        double range = Config.TAMED_MAID_PROTECTION_RANGE.get();
        for (Entity entity : victim.level().getEntities(victim, victim.getBoundingBox().inflate(range))) {
            if ((settings.isMaidAuthority() || Config.MAID_AUTHORITY.get()) && entity instanceof Mob mob && mob != attacker && mob != victim && mob.isAlive()) mob.setTarget(attacker);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel() instanceof ServerLevel level && isOurDimension(level.dimension())) {
            BlockPos pos = event.getPos();
            UUID ownerId = getOwnerUUIDFromDimensionKey(level.dimension());
            if (ownerId == null) ownerId = getOwnerUUIDFromPosition(level, pos.getX(), pos.getZ());
            if (!isAllowed(event.getEntityType(), ownerId, level, null)) event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (isOurDimension(event.getEntity().level().dimension()) && !Config.ALLOW_SET_SPAWN.get()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof Player player && isOurDimension(player.level().dimension())) {
            if (!Config.DIMENSION_WHITELIST.get().contains(event.getDimension().location().toString())) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendSystemMessage(Component.literal("This dimension is not whitelisted!"));
            }
        }
        if (Config.MAID_TELEPORT_WITH_OWNER_DIMENSION.get() && event.getEntity() instanceof ServerPlayer ownerPlayer) {
            ServerLevel currentLevel = (ServerLevel) ownerPlayer.level();
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(currentLevel.getServer().getLevel(Level.OVERWORLD));
            for (Map.Entry<UUID, PersonalDimensionSavedData.MaidInfo> entry : savedData.getTrackedMaids().entrySet()) {
                if (entry.getValue().ownerUuid.equals(ownerPlayer.getUUID())) MAIDS_TO_TELEPORT.add(new MaidTeleportData(entry.getKey(), ownerPlayer.getUUID(), event.getDimension()));
            }
        }
    }

    private record MaidTeleportData(UUID maidUuid, UUID ownerUuid, ResourceKey<Level> targetDim) {}
    private static final Queue<MaidTeleportData> MAIDS_TO_TELEPORT = new ConcurrentLinkedQueue<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isOurDimension(((Level) event.getLevel()).dimension()) && !Config.ENABLE_BLOCK_BREAKING.get()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (isOurDimension(((Level) event.getLevel()).dimension()) && !Config.ENABLE_BLOCK_BUILDING.get()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (isOurDimension(event.getLevel().dimension()) && !Config.ENABLE_BLOCK_BREAKING.get()) event.getAffectedBlocks().clear();
    }
}
