package com.tlmpersonal.tlmpersonaldimension.network;

import com.github.tartaricacid.touhoulittlemaid.data.PowerAttachment;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncDataPackage;
import com.github.tartaricacid.touhoulittlemaid.data.MaidNumAttachment;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.MaidTeleporter;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.PlayerDimensionManager;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import java.util.UUID;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PersonalDimensionGuiPacket(Action action, String data, int maidId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PersonalDimensionGuiPacket> TYPE = new CustomPacketPayload.Type<>(
            Touhoulittlemaidpersonaldimension.id("personal_dimension_gui"));

    public static final StreamCodec<ByteBuf, PersonalDimensionGuiPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PersonalDimensionGuiPacket::actionName,
            ByteBufCodecs.STRING_UTF8, PersonalDimensionGuiPacket::data,
            ByteBufCodecs.INT, PersonalDimensionGuiPacket::maidId,
            (name, data, id) -> new PersonalDimensionGuiPacket(Action.valueOf(name), data, id));

    private String actionName() {
        return action.name();
    }

    public enum Action {
        ADD_ALLOWED_ENTITY,
        REMOVE_ALLOWED_ENTITY,
        ADD_BLOCKED_ENTITY,
        REMOVE_BLOCKED_ENTITY,
        ADD_ALLOWED_PLAYER,
        REMOVE_ALLOWED_PLAYER,
        SET_DISABLE_HUNGER,
        SET_DISABLE_MAID_DEATH,
        SET_DISABLE_PLAYER_DEATH,
        SET_NATURAL_HEALING,
        SET_BLOCK_HARMFUL_EFFECTS,
        SET_MAID_EMIT_LIGHT,
        SET_LOCK_DAY,
        SET_LOCKED_DAY_TIME,
        SET_LOCK_WEATHER,
        SET_LOCKED_WEATHER_RAIN,
        SET_LOCKED_WEATHER_THUNDER,
        SET_ALLOW_ALL_ENTITIES,
        SET_DISABLE_HOSTILE_ENTITIES,
        SET_TAMED_MAID_PROTECTION,
        SET_ENTITY_CANNOT_TARGET,
        SET_MAID_AUTHORITY,
        SET_MAID_ATTACK_DISCARD,
        SET_DIMENSION_TYPE,
        REQUEST_SYNC,
        REQUEST_TELEPORTER_COOLDOWN,
        GET_TELEPORTER,
        SET_DOMAIN_EXPANSION_DIMENSION_RULES,
        SET_DOMAIN_EXPANSION_ENTITY_PROTECTION
    }

    public static void handle(PersonalDimensionGuiPacket message, IPayloadContext context) {
        if (context.flow().isServerbound()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = (ServerPlayer) context.player();
                if (sender == null)
                    return;

                ServerLevel level = sender.serverLevel();
                Entity entity = level.getEntity(message.maidId());
                if (!(entity instanceof EntityMaid maid))
                    return;

                PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(level);
                PersonalDimensionSavedData.PlayerDimensionSettings settings = savedData
                        .getOrCreateSettings(sender.getUUID());

                // Use clean name for tracking
                String playerName = sender.getGameProfile().getName();
                boolean foundSelf = false;
                for (String allowed : settings.getAllowedPlayers()) {
                    if (allowed.equalsIgnoreCase(playerName)) {
                        foundSelf = true;
                        break;
                    }
                }
                if (!foundSelf) {
                    settings.getAllowedPlayers().add(playerName);
                    savedData.setDirty();
                }

                if (message.action() == Action.REQUEST_SYNC) {
                    syncSettings(sender, settings);
                    long cooldownEnd = getCooldownEnd(sender, level);
                    PacketDistributor.sendToPlayer(sender, new TeleporterCooldownSyncPacket(cooldownEnd));
                    return;
                }
                if (message.action() == Action.REQUEST_TELEPORTER_COOLDOWN) {
                    long cooldownEnd = getCooldownEnd(sender, level);
                    PacketDistributor.sendToPlayer(sender, new TeleporterCooldownSyncPacket(cooldownEnd));
                    return;
                }
                if (message.action() == Action.GET_TELEPORTER) {
                    giveTeleporter(sender, maid, level);
                    return;
                }

                if (maid.getFavorabilityManager().getLevel() < 3) {
                    sender.sendSystemMessage(
                            Component.translatable("message.tlmpersonaldimension.favorability_too_low"));
                    return;
                }

                switch (message.action()) {
                    case ADD_ALLOWED_ENTITY: {
                        String entityId = message.data().trim();
                        if (!entityId.isEmpty()) {
                            boolean skipCosts = Config.ALLOW_FREE_WHITELIST.get();
                            if (skipCosts
                                    || tryConsumeCosts(sender, maid, Config.WHITELIST_BLACKLIST_COST_POWER_POINTS.get(),
                                            Config.WHITELIST_BLACKLIST_COST_XP.get(), true)) {
                                settings.getAllowedEntities().add(entityId);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;

                    case REMOVE_ALLOWED_ENTITY: {
                        String entityId = message.data().trim();
                        if (!entityId.isEmpty()) {
                            settings.getAllowedEntities().remove(entityId);
                            savedData.setDirty();
                            syncSettings(sender, settings);
                        }
                    }
                        break;

                    case ADD_BLOCKED_ENTITY: {
                        String entityId = message.data().trim();
                        if (!entityId.isEmpty()) {
                            boolean skipCosts = Config.ALLOW_FREE_WHITELIST.get();
                            if (skipCosts
                                    || tryConsumeCosts(sender, maid, Config.WHITELIST_BLACKLIST_COST_POWER_POINTS.get(),
                                            Config.WHITELIST_BLACKLIST_COST_XP.get(), true)) {
                                settings.getBlockedEntities().add(entityId);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;

                    case REMOVE_BLOCKED_ENTITY: {
                        String entityId = message.data().trim();
                        if (!entityId.isEmpty()) {
                            settings.getBlockedEntities().remove(entityId);
                            savedData.setDirty();
                            syncSettings(sender, settings);
                        }
                    }
                        break;

                    case ADD_ALLOWED_PLAYER: {
                        String player = message.data().trim();
                        if (!player.isEmpty()) {
                            settings.getAllowedPlayers().add(player);
                            savedData.setDirty();
                            syncSettings(sender, settings);
                        }
                    }
                        break;

                    case REMOVE_ALLOWED_PLAYER: {
                        String playerToRemove = message.data().trim();
                        if (!playerToRemove.isEmpty()) {
                            settings.getAllowedPlayers().remove(playerToRemove);
                            savedData.setDirty();
                            syncSettings(sender, settings);

                            for (ResourceKey<Level> dimKey : sender.server.levelKeys()) {
                                if (Touhoulittlemaidpersonaldimension.isOurDimension(dimKey)) {
                                    ServerLevel dimLevel = sender.server.getLevel(dimKey);
                                    if (dimLevel == null)
                                        continue;

                                    for (ServerPlayer targetPlayer : dimLevel.players()) {
                                        UUID islandOwner;
                                        if (Config.PRIVATE_DIMENSION.get()) {
                                            islandOwner = Touhoulittlemaidpersonaldimension
                                                    .getOwnerUUIDFromDimensionKey(dimLevel.dimension());
                                        } else {
                                            islandOwner = Touhoulittlemaidpersonaldimension.getOwnerUUIDFromPosition(
                                                    dimLevel, targetPlayer.getX(), targetPlayer.getZ());
                                        }

                                        if (islandOwner != null && islandOwner.equals(sender.getUUID())) {
                                            String targetName = targetPlayer.getGameProfile().getName();
                                            String targetUUID = targetPlayer.getUUID().toString();
                                            if (targetName.equalsIgnoreCase(playerToRemove)
                                                    || targetUUID.equals(playerToRemove)) {
                                                Touhoulittlemaidpersonaldimension.processRemoval(targetPlayer);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                        break;

                    case SET_ALLOW_ALL_ENTITIES: {
                        boolean canChange = Config.ALLOW_CHEAT_CONFIGS.get() || Config.ALLOW_ALLOW_ALL_ENTITIES.get();
                        if (canChange) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.ALLOW_ALL_COST_POWER_POINTS.get(),
                                    Config.ALLOW_ALL_COST_XP.get(), true)) {
                                settings.setAllowAllEntities(value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;

                    case SET_DISABLE_HOSTILE_ENTITIES: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DISABLE_HOSTILE_COST_POWER_POINTS.get(),
                                    Config.DISABLE_HOSTILE_COST_XP.get(), true)) {
                                settings.setDisableHostileEntities(value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;

                    case SET_DISABLE_HUNGER: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_DISABLE_MAID_DEATH: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_DISABLE_PLAYER_DEATH: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_NATURAL_HEALING: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_BLOCK_HARMFUL_EFFECTS: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_MAID_EMIT_LIGHT: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;

                    case SET_LOCK_DAY: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                settings.setLockDay(value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                                ServerLevel personalDim = PlayerDimensionManager
                                        .getExistingPlayerDimension(sender.server, sender.getUUID());
                                if (personalDim != null && value)
                                    personalDim.setDayTime(settings.getLockedDayTime());
                            }
                        }
                    }
                        break;

                    case SET_LOCKED_DAY_TIME: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            try {
                                int time = Integer.parseInt(message.data());
                                if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                        Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                    settings.setLockedDayTime(time);
                                    savedData.setDirty();
                                    syncSettings(sender, settings);
                                    if (settings.isLockDay()) {
                                        ServerLevel personalDim = PlayerDimensionManager
                                                .getExistingPlayerDimension(sender.server, sender.getUUID());
                                        if (personalDim != null)
                                            personalDim.setDayTime(time);
                                    }
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                        break;

                    case SET_LOCK_WEATHER: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                settings.setLockWeather(value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                                if (value) {
                                    ServerLevel personalDim = PlayerDimensionManager
                                            .getExistingPlayerDimension(sender.server, sender.getUUID());
                                    if (personalDim != null)
                                        applyWeather(personalDim, settings.isLockedWeatherRain(),
                                                settings.isLockedWeatherThunder());
                                }
                            }
                        }
                    }
                        break;

                    case SET_LOCKED_WEATHER_RAIN: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                settings.setLockedWeatherRain(value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                                if (settings.isLockWeather()) {
                                    ServerLevel personalDim = PlayerDimensionManager
                                            .getExistingPlayerDimension(sender.server, sender.getUUID());
                                    if (personalDim != null)
                                        applyWeather(personalDim, value, settings.isLockedWeatherThunder());
                                }
                            }
                        }
                    }
                        break;

                    case SET_LOCKED_WEATHER_THUNDER: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                settings.setLockedWeatherThunder(value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                                if (settings.isLockWeather()) {
                                    ServerLevel personalDim = PlayerDimensionManager
                                            .getExistingPlayerDimension(sender.server, sender.getUUID());
                                    if (personalDim != null)
                                        applyWeather(personalDim, settings.isLockedWeatherRain(), value);
                                }
                            }
                        }
                    }
                        break;

                    case SET_TAMED_MAID_PROTECTION: {
                        boolean value = Boolean.parseBoolean(message.data());
                        settings.setTamedMaidProtection(value);
                        savedData.setDirty();
                        syncSettings(sender, settings);
                    }
                        break;
                    case SET_ENTITY_CANNOT_TARGET: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_MAID_AUTHORITY: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_MAID_ATTACK_DISCARD: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            if (tryConsumeCosts(sender, maid, Config.DIMENSION_RULES_COST_POWER_POINTS.get(),
                                    Config.DIMENSION_RULES_COST_XP.get(), false)) {
                                applySetting(message.action(), settings, value);
                                savedData.setDirty();
                                syncSettings(sender, settings);
                            }
                        }
                    }
                        break;
                    case SET_DOMAIN_EXPANSION_DIMENSION_RULES: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            settings.setDomainExpansionUseDimensionRules(value);
                            savedData.setDirty();
                            syncSettings(sender, settings);
                        }
                    }
                        break;
                    case SET_DOMAIN_EXPANSION_ENTITY_PROTECTION: {
                        if (Config.ALLOW_CHEAT_CONFIGS.get()) {
                            boolean value = Boolean.parseBoolean(message.data());
                            settings.setDomainExpansionUseEntityProtection(value);
                            savedData.setDirty();
                            syncSettings(sender, settings);
                        }
                    }
                        break;

                    case SET_DIMENSION_TYPE: {
                        try {
                            Config.DimensionType type = Config.DimensionType.valueOf(message.data());
                            settings.setDimensionType(type);
                            savedData.setDirty();
                            syncSettings(sender, settings);
                            String typeNameKey = switch (type) {
                                case VOID -> "gui.tlmpersonaldimension.dim_type.void";
                                case NORMAL -> "gui.tlmpersonaldimension.dim_type.normal";
                                case CHERRY -> "gui.tlmpersonaldimension.dim_type.cherry";
                            };
                            sender.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.dim_type_set",
                                    Component.translatable(typeNameKey)));
                        } catch (Exception ignored) {
                        }
                    }
                        break;
                }
            });
        }
    }

    private static void syncSettings(ServerPlayer player, PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        PacketDistributor.sendToPlayer(player,
                new PersonalDimensionSettingsSyncPacket(settings.save(), Config.ALLOW_CHEAT_CONFIGS.get()));
    }

    private static boolean tryConsumeCosts(ServerPlayer player, EntityMaid maid, double powerCostVal, int xpCost,
            boolean needCake) {
        PowerAttachment powerData = player.getData(InitDataAttachment.POWER_NUM);

        if (powerCostVal > 0.0 && powerData.get() < (float) powerCostVal) {
            player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_power"));
            return false;
        }

        if (xpCost > 0 && player.experienceLevel < xpCost) {
            player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp"));
            return false;
        }

        if (needCake) {
            if (!hasCake(player)) {
                player.sendSystemMessage(
                        Component.literal("Need a cake to convince maid as it's her personal dimension"));
                return false;
            }
            consumeCake(player);
        }

        if (powerCostVal > 0.0) {
            powerData.min((float) powerCostVal);
            syncPowerToClient(player);
        }
        if (xpCost > 0)
            player.giveExperienceLevels(-xpCost);

        return true;
    }

    private static void syncPowerToClient(ServerPlayer player) {
        PowerAttachment powerData = player.getData(InitDataAttachment.POWER_NUM);
        MaidNumAttachment maidNumData = player.getData(InitDataAttachment.MAID_NUM);
        PacketDistributor.sendToPlayer(player, new SyncDataPackage(powerData.get(), maidNumData.get()));
    }

    private static boolean hasCake(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.CAKE))
                return true;
        }
        return false;
    }

    private static void consumeCake(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.CAKE)) {
                player.getInventory().getItem(i).shrink(1);
                return;
            }
        }
    }

    private static void applySetting(Action action, PersonalDimensionSavedData.PlayerDimensionSettings settings,
            boolean value) {
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
            default -> {
            }
        }
    }

    private static final long COOLDOWN_MS = 30 * 60 * 1000;

    private static long getCooldownEnd(ServerPlayer player, ServerLevel level) {
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(level);
        Long lastTime = savedData.getTeleporterCooldown(player.getUUID());
        return lastTime != null ? lastTime + COOLDOWN_MS : 0;
    }

    private static void giveTeleporter(ServerPlayer sender, EntityMaid maid, ServerLevel level) {
        if (maid.getFavorabilityManager().getLevel() < 3) {
            sender.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.favorability_too_low"));
            return;
        }
        UUID playerId = sender.getUUID();
        long now = System.currentTimeMillis();
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(level);
        Long lastTime = savedData.getTeleporterCooldown(playerId);
        if (lastTime != null && (now - lastTime) < COOLDOWN_MS) {
            PacketDistributor.sendToPlayer(sender, new TeleporterCooldownSyncPacket(lastTime + COOLDOWN_MS));
            return;
        }
        ItemStack teleporter = new ItemStack(Touhoulittlemaidpersonaldimension.MAID_TELEPORTER.get());
        MaidTeleporter.setOwnerInfo(teleporter, sender.getUUID(), sender.getGameProfile().getName());
        if (!sender.getInventory().add(teleporter))
            sender.drop(teleporter, false);
        savedData.setTeleporterCooldown(playerId, now);
        sender.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.teleporter_given"));
        PacketDistributor.sendToPlayer(sender, new TeleporterCooldownSyncPacket(now + COOLDOWN_MS));
    }

    private static void applyWeather(ServerLevel level, boolean rain, boolean thunder) {
        if (thunder)
            level.setWeatherParameters(0, 1000000, true, true);
        else if (rain)
            level.setWeatherParameters(0, 1000000, true, false);
        else
            level.setWeatherParameters(1000000, 0, false, false);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
