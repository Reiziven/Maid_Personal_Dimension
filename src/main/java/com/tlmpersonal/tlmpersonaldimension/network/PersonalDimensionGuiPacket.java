package com.tlmpersonal.tlmpersonaldimension.network;

import com.github.tartaricacid.touhoulittlemaid.capability.MaidNumCapabilityProvider;
import com.github.tartaricacid.touhoulittlemaid.capability.PowerCapabilityProvider;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncCapabilityMessage;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.MaidTeleporter;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.PlayerDimensionManager;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PersonalDimensionGuiPacket {

    private final Action action;
    private final String data;
    private final int maidId;

    public PersonalDimensionGuiPacket(Action action, String data, int maidId) {
        this.action = action;
        this.data = data;
        this.maidId = maidId;
    }

    public enum Action {
        ADD_ALLOWED_ENTITY, REMOVE_ALLOWED_ENTITY, ADD_BLOCKED_ENTITY, REMOVE_BLOCKED_ENTITY,
        ADD_ALLOWED_PLAYER, REMOVE_ALLOWED_PLAYER, SET_DISABLE_HUNGER, SET_DISABLE_MAID_DEATH,
        SET_DISABLE_PLAYER_DEATH, SET_NATURAL_HEALING, SET_BLOCK_HARMFUL_EFFECTS, SET_MAID_EMIT_LIGHT,
        SET_LOCK_DAY, SET_LOCKED_DAY_TIME, SET_LOCK_WEATHER, SET_LOCKED_WEATHER_RAIN,
        SET_LOCKED_WEATHER_THUNDER, SET_ALLOW_ALL_ENTITIES, SET_DISABLE_HOSTILE_ENTITIES,
        SET_TAMED_MAID_PROTECTION, SET_ENTITY_CANNOT_TARGET, SET_MAID_AUTHORITY, SET_MAID_ATTACK_DISCARD,
        SET_DIMENSION_TYPE, REQUEST_SYNC, REQUEST_TELEPORTER_COOLDOWN, GET_TELEPORTER,
        SET_DOMAIN_EXPANSION_DIMENSION_RULES, SET_DOMAIN_EXPANSION_ENTITY_PROTECTION
    }

    public static void encode(PersonalDimensionGuiPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.action.name());
        buf.writeUtf(msg.data);
        buf.writeInt(msg.maidId);
    }

    public static PersonalDimensionGuiPacket decode(FriendlyByteBuf buf) {
        return new PersonalDimensionGuiPacket(Action.valueOf(buf.readUtf()), buf.readUtf(), buf.readInt());
    }

    public static void handle(PersonalDimensionGuiPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            ServerLevel level = sender.serverLevel();

            // Search for the maid across ALL loaded levels, not just the player's current level
            EntityMaid maid = null;
            Entity entityById = level.getEntity(message.maidId);
            if (entityById instanceof EntityMaid m) {
                maid = m;
            } else {
                for (ServerLevel lvl : sender.server.getAllLevels()) {
                    Entity e = lvl.getEntity(message.maidId);
                    if (e instanceof EntityMaid m) { maid = m; break; }
                }
            }
            if (maid == null) return;

            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(level);
            PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData.getOrCreateSettings(sender.getUUID());

            String playerName = sender.getGameProfile().getName();
            boolean foundSelf = settings.getAllowedPlayers().stream().anyMatch(a -> a.equalsIgnoreCase(playerName));
            if (!foundSelf) { settings.getAllowedPlayers().add(playerName); savedData.setDirty(); }

            if (message.action == Action.REQUEST_SYNC) {
                syncSettings(sender, settings);
                long cooldownEnd = getCooldownEnd(sender, level);
                Touhoulittlemaidpersonaldimension.NETWORK.sendTo(
                        new TeleporterCooldownSyncPacket(cooldownEnd), sender.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                return;
            }
            if (message.action == Action.REQUEST_TELEPORTER_COOLDOWN) {
                long cooldownEnd = getCooldownEnd(sender, level);
                Touhoulittlemaidpersonaldimension.NETWORK.sendTo(
                        new TeleporterCooldownSyncPacket(cooldownEnd), sender.connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                return;
            }
            if (message.action == Action.GET_TELEPORTER) {
                if (maid == null) return; // teleporter requires a maid nearby
                giveTeleporter(sender, maid, level);
                return;
            }

            // Favorability check: skip if maid not found (player already had GUI open, trust that)
            if (maid != null && maid.getFavorabilityManager().getLevel() < 3) {
                sender.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.favorability_too_low"));
                return;
            }

            switch (message.action) {
                case ADD_ALLOWED_ENTITY -> { String entityId = message.data.trim(); if (!entityId.isEmpty() && (Config.ALLOW_FREE_WHITELIST.get() || tryConsumeCosts(sender, maid, Config.WHITELIST_BLACKLIST_COST_POWER_POINTS.get(), Config.WHITELIST_BLACKLIST_COST_XP.get(), true))) { settings.getAllowedEntities().add(entityId); savedData.setDirty(); syncSettings(sender, settings); } }
                case REMOVE_ALLOWED_ENTITY -> { String entityId = message.data.trim(); if (!entityId.isEmpty()) { settings.getAllowedEntities().remove(entityId); savedData.setDirty(); syncSettings(sender, settings); } }
                case ADD_BLOCKED_ENTITY -> { String entityId = message.data.trim(); if (!entityId.isEmpty() && (Config.ALLOW_FREE_WHITELIST.get() || tryConsumeCosts(sender, maid, Config.WHITELIST_BLACKLIST_COST_POWER_POINTS.get(), Config.WHITELIST_BLACKLIST_COST_XP.get(), true))) { settings.getBlockedEntities().add(entityId); savedData.setDirty(); syncSettings(sender, settings); } }
                case REMOVE_BLOCKED_ENTITY -> { String entityId = message.data.trim(); if (!entityId.isEmpty()) { settings.getBlockedEntities().remove(entityId); savedData.setDirty(); syncSettings(sender, settings); } }
                case ADD_ALLOWED_PLAYER -> { String player = message.data.trim(); if (!player.isEmpty()) { settings.getAllowedPlayers().add(player); savedData.setDirty(); syncSettings(sender, settings); } }
                case REMOVE_ALLOWED_PLAYER -> {
                    String playerToRemove = message.data.trim();
                    if (!playerToRemove.isEmpty()) {
                        settings.getAllowedPlayers().remove(playerToRemove);
                        savedData.setDirty(); syncSettings(sender, settings);
                        for (ResourceKey<Level> dimKey : sender.server.levelKeys()) {
                            if (!Touhoulittlemaidpersonaldimension.isOurDimension(dimKey)) continue;
                            ServerLevel dimLevel = sender.server.getLevel(dimKey);
                            if (dimLevel == null) continue;
                            for (ServerPlayer tp : dimLevel.players()) {
                                UUID islandOwner = Config.PRIVATE_DIMENSION.get()
                                        ? Touhoulittlemaidpersonaldimension.getOwnerUUIDFromDimensionKey(dimLevel.dimension())
                                        : Touhoulittlemaidpersonaldimension.getOwnerUUIDFromPosition(dimLevel, tp.getX(), tp.getZ());
                                if (islandOwner != null && islandOwner.equals(sender.getUUID())) {
                                    String tName = tp.getGameProfile().getName(), tUUID = tp.getUUID().toString();
                                    if (tName.equalsIgnoreCase(playerToRemove) || tUUID.equals(playerToRemove))
                                        Touhoulittlemaidpersonaldimension.processRemoval(tp);
                                }
                            }
                        }
                    }
                }
                case SET_ALLOW_ALL_ENTITIES -> { if (Config.ALLOW_CHEAT_CONFIGS.get() || Config.ALLOW_ALLOW_ALL_ENTITIES.get()) { boolean v = Boolean.parseBoolean(message.data); String rk = message.action.name(); if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.ALLOW_ALL_COST_POWER_POINTS.get(), Config.ALLOW_ALL_COST_XP.get(), !settings.isRuleUnlocked(rk))) { settings.setAllowAllEntities(v); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings); } } }
                case SET_DISABLE_HOSTILE_ENTITIES -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { boolean v = Boolean.parseBoolean(message.data); String rk = message.action.name(); if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.DISABLE_HOSTILE_COST_POWER_POINTS.get(), Config.DISABLE_HOSTILE_COST_XP.get(), false)) { settings.setDisableHostileEntities(v); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings); } } }
                case SET_DISABLE_HUNGER, SET_DISABLE_MAID_DEATH, SET_DISABLE_PLAYER_DEATH, SET_NATURAL_HEALING,
                        SET_BLOCK_HARMFUL_EFFECTS, SET_MAID_EMIT_LIGHT, SET_ENTITY_CANNOT_TARGET,
                        SET_MAID_AUTHORITY, SET_MAID_ATTACK_DISCARD -> {
                    if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                        boolean v = Boolean.parseBoolean(message.data); String rk = message.action.name();
                        if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(), Config.DIMENSION_RULES_COST_XP.get(), false)) {
                            applySetting(message.action, settings, v); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings);
                        }
                    }
                }
                case SET_LOCK_DAY -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { boolean v = Boolean.parseBoolean(message.data); String rk = message.action.name(); if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(), Config.DIMENSION_RULES_COST_XP.get(), false)) { settings.setLockDay(v); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings); ServerLevel pd = PlayerDimensionManager.getExistingPlayerDimension(sender.server, sender.getUUID()); if (pd != null && v) pd.setDayTime(settings.getLockedDayTime()); } } }
                case SET_LOCKED_DAY_TIME -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { try { int t = Integer.parseInt(message.data); String rk = message.action.name(); if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(), Config.DIMENSION_RULES_COST_XP.get(), false)) { settings.setLockedDayTime(t); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings); if (settings.isLockDay()) { ServerLevel pd = PlayerDimensionManager.getExistingPlayerDimension(sender.server, sender.getUUID()); if (pd != null) pd.setDayTime(t); } } } catch (NumberFormatException ignored) {} } }
                case SET_LOCK_WEATHER -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { boolean v = Boolean.parseBoolean(message.data); String rk = message.action.name(); if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(), Config.DIMENSION_RULES_COST_XP.get(), false)) { settings.setLockWeather(v); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings); if (v) { ServerLevel pd = PlayerDimensionManager.getExistingPlayerDimension(sender.server, sender.getUUID()); if (pd != null) applyWeather(pd, settings.isLockedWeatherRain(), settings.isLockedWeatherThunder()); } } } }
                case SET_LOCKED_WEATHER_RAIN -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { boolean v = Boolean.parseBoolean(message.data); String rk = message.action.name(); if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(), Config.DIMENSION_RULES_COST_XP.get(), false)) { settings.setLockedWeatherRain(v); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings); if (settings.isLockWeather()) { ServerLevel pd = PlayerDimensionManager.getExistingPlayerDimension(sender.server, sender.getUUID()); if (pd != null) applyWeather(pd, v, settings.isLockedWeatherThunder()); } } } }
                case SET_LOCKED_WEATHER_THUNDER -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { boolean v = Boolean.parseBoolean(message.data); String rk = message.action.name(); if (settings.isRuleUnlocked(rk) || tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(), Config.DIMENSION_RULES_COST_XP.get(), false)) { settings.setLockedWeatherThunder(v); settings.unlockRule(rk); savedData.setDirty(); syncSettings(sender, settings); if (settings.isLockWeather()) { ServerLevel pd = PlayerDimensionManager.getExistingPlayerDimension(sender.server, sender.getUUID()); if (pd != null) applyWeather(pd, settings.isLockedWeatherRain(), v); } } } }
                case SET_TAMED_MAID_PROTECTION -> { settings.setTamedMaidProtection(Boolean.parseBoolean(message.data)); savedData.setDirty(); syncSettings(sender, settings); }
                case SET_DOMAIN_EXPANSION_DIMENSION_RULES -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { settings.setDomainExpansionUseDimensionRules(Boolean.parseBoolean(message.data)); savedData.setDirty(); syncSettings(sender, settings); } }
                case SET_DOMAIN_EXPANSION_ENTITY_PROTECTION -> { if (Config.ALLOW_CHEAT_CONFIGS.get()) { settings.setDomainExpansionUseEntityProtection(Boolean.parseBoolean(message.data)); savedData.setDirty(); syncSettings(sender, settings); } }
                case SET_DIMENSION_TYPE -> { String dimId = message.data.trim(); if (!dimId.isEmpty()) { settings.setDimensionTypeId(dimId); savedData.setDirty(); syncSettings(sender, settings); sender.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.dim_type_set", Component.literal(dimId.toUpperCase()))); } }
                default -> {}
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void syncSettings(ServerPlayer player, PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        Touhoulittlemaidpersonaldimension.NETWORK.sendTo(
                new PersonalDimensionSettingsSyncPacket(settings.save(), Config.ALLOW_CHEAT_CONFIGS.get()),
                player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }

    private static boolean tryConsumeCosts(ServerPlayer player, EntityMaid maid, double powerCostVal, int xpCost, boolean needCake) {
        float[] powerRef = {0f};
        player.getCapability(PowerCapabilityProvider.POWER_CAP).ifPresent(cap -> powerRef[0] = cap.get());
        if (powerCostVal > 0.0 && powerRef[0] < (float) powerCostVal) {
            player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_power")); return false;
        }
        if (xpCost > 0 && player.experienceLevel < xpCost) {
            player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp")); return false;
        }
        if (needCake) {
            if (!hasCake(player)) { player.sendSystemMessage(Component.literal("Need a cake to convince maid")); return false; }
            consumeCake(player);
        }
        if (powerCostVal > 0.0) {
            player.getCapability(PowerCapabilityProvider.POWER_CAP).ifPresent(cap -> cap.min((float) powerCostVal));
            syncPowerToClient(player);
        }
        if (xpCost > 0) player.giveExperienceLevels(-xpCost);
        return true;
    }

    private static void syncPowerToClient(ServerPlayer player) {
        float power = 0f; int maidNum = 0;
        final float[] pRef = {0f}; final int[] mRef = {0};
        player.getCapability(PowerCapabilityProvider.POWER_CAP).ifPresent(c -> pRef[0] = c.get());
        player.getCapability(MaidNumCapabilityProvider.MAID_NUM_CAP).ifPresent(c -> mRef[0] = c.get());
        NetworkHandler.CHANNEL.sendTo(new SyncCapabilityMessage(pRef[0], mRef[0]), player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }

    private static boolean hasCake(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(Items.CAKE)) return true;
        return false;
    }

    private static void consumeCake(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(Items.CAKE)) { player.getInventory().getItem(i).shrink(1); return; }
    }

    private static void applySetting(Action action, PersonalDimensionSavedData.PlayerDimensionSettings settings, boolean value) {
        switch (action) {
            case SET_DISABLE_HUNGER -> settings.setDisableHunger(value);
            case SET_DISABLE_MAID_DEATH -> settings.setDisableMaidDeath(value);
            case SET_DISABLE_PLAYER_DEATH -> settings.setDisablePlayerDeath(value);
            case SET_NATURAL_HEALING -> settings.setNaturalHealing(value);
            case SET_BLOCK_HARMFUL_EFFECTS -> settings.setBlockHarmfulEffects(value);
            case SET_MAID_EMIT_LIGHT -> settings.setMaidEmitLight(value);
            case SET_ENTITY_CANNOT_TARGET -> settings.setEntityCannotTarget(value);
            case SET_MAID_AUTHORITY -> settings.setMaidAuthority(value);
            case SET_MAID_ATTACK_DISCARD -> settings.setMaidAttackDiscard(value);
            default -> {}
        }
    }

    private static final long COOLDOWN_MS = 30 * 60 * 1000L;

    private static long getCooldownEnd(ServerPlayer player, ServerLevel level) {
        Long last = PersonalDimensionSavedData.get(level).getTeleporterCooldown(player.getUUID());
        return last != null ? last + COOLDOWN_MS : 0;
    }

    private static void giveTeleporter(ServerPlayer sender, EntityMaid maid, ServerLevel level) {
        if (maid.getFavorabilityManager().getLevel() < 3) {
            sender.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.favorability_too_low")); return;
        }
        UUID playerId = sender.getUUID(); long now = System.currentTimeMillis();
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(level);
        Long lastTime = savedData.getTeleporterCooldown(playerId);
        if (lastTime != null && (now - lastTime) < COOLDOWN_MS) {
            Touhoulittlemaidpersonaldimension.NETWORK.sendTo(new TeleporterCooldownSyncPacket(lastTime + COOLDOWN_MS),
                    sender.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT); return;
        }
        ItemStack teleporter = new ItemStack(Touhoulittlemaidpersonaldimension.MAID_TELEPORTER.get());
        MaidTeleporter.setOwnerInfo(teleporter, sender.getUUID(), sender.getGameProfile().getName());
        if (!sender.getInventory().add(teleporter)) sender.drop(teleporter, false);
        savedData.setTeleporterCooldown(playerId, now);
        sender.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.teleporter_given"));
        Touhoulittlemaidpersonaldimension.NETWORK.sendTo(new TeleporterCooldownSyncPacket(now + COOLDOWN_MS),
                sender.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void applyWeather(ServerLevel level, boolean rain, boolean thunder) {
        if (thunder) level.setWeatherParameters(0, 1000000, true, true);
        else if (rain) level.setWeatherParameters(0, 1000000, true, false);
        else level.setWeatherParameters(1000000, 0, false, false);
    }
}
