package com.tlmpersonal.tlmpersonaldimension;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Touhou Little Maid Personal Dimension Config"))
                .setSavingRunnable(Config.SPEC::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General Settings"));
        general.addEntry(entryBuilder.startEnumSelector(
                Component.literal("Dimension Type"),
                Config.DimensionType.class,
                Config.DIMENSION_TYPE.get())
                .setEnumNameProvider(en -> switch ((Config.DimensionType) en) {
                    case VOID -> Component.literal("MAID ISLAND");
                    case NORMAL -> Component.literal("OVERWORLD");
                    case CHERRY -> Component.literal("CHERRY");
                })
                .setDefaultValue(Config.DimensionType.VOID)
                .setTooltip(Component.literal("MAID ISLAND (Island only), OVERWORLD (World gen), CHERRY (Grove only)"))
                .setSaveConsumer(Config.DIMENSION_TYPE::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Enable Structures"),
                Config.ENABLE_STRUCTURES.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Toggle vanilla structures like villages"))
                .setSaveConsumer(Config.ENABLE_STRUCTURES::set)
                .build());
        general.addEntry(entryBuilder.startIntField(
                Component.literal("Maid Spawn Chance"),
                Config.MAID_SPAWN_CHANCE.get())
                .setDefaultValue(100)
                .setMin(0)
                .setMax(1000)
                .setTooltip(Component.literal("the bigger the number the smaller the chance for a maid to spawn naturally"))
                .setSaveConsumer(Config.MAID_SPAWN_CHANCE::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Teleport To Ground"),
                Config.TELEPORT_TO_GROUND.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Always land on surface when teleporting"))
                .setSaveConsumer(Config.TELEPORT_TO_GROUND::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Allow Set Spawn"),
                Config.ALLOW_SET_SPAWN.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Allow beds to set respawn point"))
                .setSaveConsumer(Config.ALLOW_SET_SPAWN::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Allow Entity Teleport"),
                Config.ALLOW_ENTITY_TELEPORT.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Allow teleporting entities with shift-right click using Okina's door or maid teleporter"))
                .setSaveConsumer(Config.ALLOW_ENTITY_TELEPORT::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Private Dimension"),
                Config.PRIVATE_DIMENSION.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, each player's personal dimension is private"))
                .setSaveConsumer(Config.PRIVATE_DIMENSION::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Block Breaking"), Config.ENABLE_BLOCK_BREAKING.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Whether players are allowed to break blocks in the dimension."))
                .setSaveConsumer(Config.ENABLE_BLOCK_BREAKING::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Block Building"), Config.ENABLE_BLOCK_BUILDING.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Whether players are allowed to place blocks in the dimension."))
                .setSaveConsumer(Config.ENABLE_BLOCK_BUILDING::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Use Maid GUI Controls (4th Tab)"), Config.USE_MAID_GUI_CONTROLS.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, the buttons will be directly inside the maid's GUI (4th tab). If false, clicking the 4th tab's art will open the full-screen mod GUI."))
                .setSaveConsumer(Config.USE_MAID_GUI_CONTROLS::set)
                .build());

        ConfigCategory ysmSettings = builder.getOrCreateCategory(Component.literal("YSM Maid Model Settings"));
        ysmSettings.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Use YSM Models"),
                Config.USE_YSM_MODELS.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, spawned maids will use YSM models if available"))
                .setSaveConsumer(Config.USE_YSM_MODELS::set)
                .build());
        ysmSettings.addEntry(entryBuilder.startStrList(
                Component.literal("YSM Model IDs"),
                new ArrayList<>(Config.YSM_MODEL_IDS.get()))
                .setDefaultValue(List.of("default"))
                .setTooltip(Component.literal("List of YSM model IDs to randomly choose from"))
                .setSaveConsumer(Config.YSM_MODEL_IDS::set)
                .build());

        ConfigCategory entityProtection = builder.getOrCreateCategory(Component.literal("Entity Protection"));
        entityProtection.addEntry(entryBuilder.startBooleanToggle(Component.literal("Immediate Removal"), Config.IMMEDIATE_REMOVAL.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Instantly remove blocked mobs on the first tick can cause crash (Prevents horror mob effects)"))
                .setSaveConsumer(Config.IMMEDIATE_REMOVAL::set)
                .build());
        entityProtection.addEntry(entryBuilder.startBooleanToggle(Component.literal("Delete Blocked Entities"), Config.REMOVE_BLOCKED_ENTITIES.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If ON, blocked entities are deleted instead of teleported to Overworld"))
                .setSaveConsumer(Config.REMOVE_BLOCKED_ENTITIES::set)
                .build());
        entityProtection.addEntry(entryBuilder.startBooleanToggle(Component.literal("Allow All Entities"), Config.ALLOW_ALL_ENTITIES.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Allows all entities except those explicitly listed in Blocked Entities, AllowHostile still apply."))
                .setSaveConsumer(Config.ALLOW_ALL_ENTITIES::set)
                .build());
        entityProtection.addEntry(entryBuilder.startBooleanToggle(Component.literal("Disable Hostile Entities"), Config.DISABLE_HOSTILE_ENTITIES.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Completely prevent hostile mobs from existing"))
                .setSaveConsumer(Config.DISABLE_HOSTILE_ENTITIES::set)
                .build());
        entityProtection.addEntry(entryBuilder.startBooleanToggle(Component.literal("Whitelist Mode"), Config.ENTITY_WHITELIST_MODE.get())
                .setDefaultValue(true)
                .setSaveConsumer(Config.ENTITY_WHITELIST_MODE::set)
                .build());
        entityProtection.addEntry(entryBuilder.startStrList(Component.literal("Allowed Entities"), new ArrayList<>(Config.ALLOWED_ENTITIES.get()))
                .setDefaultValue(List.of("touhou_little_maid:broom"))
                .setSaveConsumer(Config.ALLOWED_ENTITIES::set)
                .build());
        entityProtection.addEntry(entryBuilder.startStrList(Component.literal("Blocked Entities"), new ArrayList<>(Config.BLOCKED_ENTITIES.get()))
                .setDefaultValue(List.of())
                .setSaveConsumer(Config.BLOCKED_ENTITIES::set)
                .build());


        ConfigCategory teleportRestrictions = builder.getOrCreateCategory(Component.literal("Teleport Restrictions"));
        teleportRestrictions.addEntry(entryBuilder.startStrList(Component.literal("Dimension Whitelist"), new ArrayList<>(Config.DIMENSION_WHITELIST.get()))
                .setDefaultValue(List.of("minecraft:overworld"))
                .setSaveConsumer(Config.DIMENSION_WHITELIST::set)
                .build());

        ConfigCategory advancedProtection = builder.getOrCreateCategory(Component.literal("Protection Settings"));
        advancedProtection.addEntry(entryBuilder.startStrList(Component.literal("Damage Blacklist"), new ArrayList<>(Config.DAMAGE_SOURCE_BLACKLIST.get()))
                .setDefaultValue(List.of())
                .setSaveConsumer(Config.DAMAGE_SOURCE_BLACKLIST::set)
                .build());
        advancedProtection.addEntry(entryBuilder.startDoubleField(Component.literal("Fall Protection Y"), Config.FALL_PROTECTION_Y.get())
                .setDefaultValue(-64.0)
                .setSaveConsumer(Config.FALL_PROTECTION_Y::set)
                .build());

        ConfigCategory maidProtection = builder.getOrCreateCategory(Component.literal("Maid Protection"));
        maidProtection.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Tamed Maid Protection Enabled"),
                Config.TAMED_MAID_PROTECTION_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If OFF, tamed maid protection is completely disabled and the option won't appear in the GUI."))
                .setSaveConsumer(Config.TAMED_MAID_PROTECTION_ENABLED::set)
                .build());
        maidProtection.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Tamed Maid Protection"),
                Config.TAMED_MAID_PROTECTION.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Tamed maids expel entities about to kill their owner or other tamed maids."))
                .setSaveConsumer(Config.TAMED_MAID_PROTECTION::set)
                .build());
        maidProtection.addEntry(entryBuilder.startIntField(
                Component.literal("Tamed Maid Protection Range"),
                Config.TAMED_MAID_PROTECTION_RANGE.get())
                .setDefaultValue(20)
                .setMin(1)
                .setMax(100)
                .setTooltip(Component.literal("Range in blocks for tamed maid protection."))
                .setSaveConsumer(Config.TAMED_MAID_PROTECTION_RANGE::set)
                .build());
        maidProtection.addEntry(entryBuilder.startIntField(
                Component.literal("Tamed Maid Protection Cooldown (Seconds)"),
                Config.TAMED_MAID_PROTECTION_COOLDOWN.get())
                .setDefaultValue(60)
                .setMin(0)
                .setTooltip(Component.literal("Cooldown in seconds for tamed maid protection to trigger again."))
                .setSaveConsumer(Config.TAMED_MAID_PROTECTION_COOLDOWN::set)
                .build());
        maidProtection.addEntry(entryBuilder.startDoubleField(
                Component.literal("Tamed Maid Protection Power Cost"),
                Config.TAMED_MAID_PROTECTION_POWER_POINTS_COST.get())
                .setDefaultValue(0.0)
                .setMin(0.0)
                .setTooltip(Component.literal("Cost in power points (maid experience) for tamed maid protection to trigger (0 to disable)."))
                .setSaveConsumer(Config.TAMED_MAID_PROTECTION_POWER_POINTS_COST::set)
                .build());
        maidProtection.addEntry(entryBuilder.startIntField(
                Component.literal("Tamed Maid Protection XP Cost (Levels)"),
                Config.TAMED_MAID_PROTECTION_XP_COST.get())
                .setDefaultValue(0)
                .setMin(0)
                .setTooltip(Component.literal("Cost in experience levels for tamed maid protection to trigger (0 to disable)."))
                .setSaveConsumer(Config.TAMED_MAID_PROTECTION_XP_COST::set)
                .build());
        maidProtection.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("All Maid Protection"),
                Config.ALL_MAID_PROTECTION.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("All maids expel entities about to kill any player or maid."))
                .setSaveConsumer(Config.ALL_MAID_PROTECTION::set)
                .build());
        maidProtection.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Wild Maid Protection"),
                Config.WILD_MAID_PROTECTION.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Non-tamed maids expel entities about to kill them."))
                .setSaveConsumer(Config.WILD_MAID_PROTECTION::set)
                .build());
        maidProtection.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Maid Authority"),
                        Config.MAID_AUTHORITY.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Some entities will protect maid and player."))
                .setSaveConsumer(Config.MAID_AUTHORITY::set)
                .build());
        maidProtection.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Maid Teleports With Owner Through Dimensions"),
                Config.MAID_TELEPORT_WITH_OWNER_DIMENSION.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Tamed maids will teleport with their owner through any dimension, regardless of distance."))
                .setSaveConsumer(Config.MAID_TELEPORT_WITH_OWNER_DIMENSION::set)
                .build());

        ConfigCategory survivalOptions = builder.getOrCreateCategory(Component.literal("Survival Options"));
        survivalOptions.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Disable Hunger"),
                Config.DISABLE_HUNGER.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Players won't lose hunger in this dimension."))
                .setSaveConsumer(Config.DISABLE_HUNGER::set)
                .build());
        survivalOptions.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Disable Maid Death"),
                Config.DISABLE_MAID_DEATH.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Maids cannot die in this dimension."))
                .setSaveConsumer(Config.DISABLE_MAID_DEATH::set)
                .build());
        survivalOptions.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Disable Player Death"),
                Config.DISABLE_PLAYER_DEATH.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Players cannot die in this dimension (stay at 1 HP)."))
                .setSaveConsumer(Config.DISABLE_PLAYER_DEATH::set)
                .build());
        survivalOptions.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Natural Healing"),
                Config.NATURAL_HEALING.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Players and maids regenerate health over time."))
                .setSaveConsumer(Config.NATURAL_HEALING::set)
                .build());
        survivalOptions.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Block Harmful Effects"),
                Config.BLOCK_HARMFUL_EFFECTS.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Prevents harmful potion effects."))
                .setSaveConsumer(Config.BLOCK_HARMFUL_EFFECTS::set)
                .build());
        survivalOptions.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Maid Emit Light"),
                Config.MAID_EMIT_LIGHT.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Maids will emit light in this dimension."))
                .setSaveConsumer(Config.MAID_EMIT_LIGHT::set)
                .build());
        survivalOptions.addEntry(entryBuilder.startBooleanToggle(
                        Component.literal("Mobs Neutral"),
                        Config.ENTITY_CANNOT_TARGET.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Mobs are neutral only attack if attacked first ( doens't work for every mob )"))
                .setSaveConsumer(Config.ENTITY_CANNOT_TARGET::set)
                .build());

        ConfigCategory teleportCosts = builder.getOrCreateCategory(Component.literal("Teleportation Costs"));
        teleportCosts.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Teleport Has Cost"),
                Config.TELEPORT_HAS_COST.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, teleporting (GUI/Item) has costs"))
                .setSaveConsumer(Config.TELEPORT_HAS_COST::set)
                .build());
        teleportCosts.addEntry(entryBuilder.startDoubleField(
                Component.literal("Teleport Power Cost"),
                Config.TELEPORT_COST_POWER_POINTS.get())
                .setDefaultValue(0.16)
                .setMin(0.0)
                .setSaveConsumer(Config.TELEPORT_COST_POWER_POINTS::set)
                .build());
        teleportCosts.addEntry(entryBuilder.startIntField(
                Component.literal("Teleport XP Cost (Levels)"),
                Config.TELEPORT_COST_XP.get())
                .setDefaultValue(5)
                .setMin(0)
                .setSaveConsumer(Config.TELEPORT_COST_XP::set)
                .build());
        teleportCosts.addEntry(entryBuilder.startIntField(
                Component.literal("Join Cooldown (Seconds)"),
                Config.TELEPORT_JOIN_COOLDOWN.get())
                .setDefaultValue(300)
                .setMin(0)
                .setTooltip(Component.literal("Cooldown in seconds to join the personal dimension"))
                .setSaveConsumer(Config.TELEPORT_JOIN_COOLDOWN::set)
                .build());
        teleportCosts.addEntry(entryBuilder.startIntField(
                Component.literal("Leave Cooldown (Seconds)"),
                Config.TELEPORT_LEAVE_COOLDOWN.get())
                .setDefaultValue(0)
                .setMin(0)
                .setTooltip(Component.literal("Cooldown in seconds to leave the personal dimension"))
                .setSaveConsumer(Config.TELEPORT_LEAVE_COOLDOWN::set)
                .build());

        ConfigCategory guiSettings = builder.getOrCreateCategory(Component.literal("Maid Entity Allowance & GUI"));
        guiSettings.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Allow Cheat Configs"),
                Config.ALLOW_CHEAT_CONFIGS.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("Allow players to toggle survival options via MAID GUI"))
                .setSaveConsumer(Config.ALLOW_CHEAT_CONFIGS::set)
                .build());
        guiSettings.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Free Whitelist"),
                Config.ALLOW_FREE_WHITELIST.get())
                .setDefaultValue(false)
                .setSaveConsumer(Config.ALLOW_FREE_WHITELIST::set)
                .build());
        guiSettings.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Allow All Entities Toggle"),
                Config.ALLOW_ALLOW_ALL_ENTITIES.get())
                .setDefaultValue(false)
                .setSaveConsumer(Config.ALLOW_ALLOW_ALL_ENTITIES::set)
                .build());
        guiSettings.addEntry(entryBuilder.startIntField(
                Component.literal("Whitelist Cooldown (m)"),
                Config.MAID_ENTITY_WHITELIST_COOLDOWN_MINUTES.get())
                .setDefaultValue(7)
                .setMin(5)
                .setMax(10)
                .setSaveConsumer(Config.MAID_ENTITY_WHITELIST_COOLDOWN_MINUTES::set)
                .build());
        guiSettings.addEntry(entryBuilder.startDoubleField(
                Component.literal("Whitelist/Blacklist Power Cost"),
                Config.WHITELIST_BLACKLIST_COST_POWER_POINTS.get())
                .setDefaultValue(0.50)
                .setMin(0.0)
                .setSaveConsumer(Config.WHITELIST_BLACKLIST_COST_POWER_POINTS::set)
                .build());
        guiSettings.addEntry(entryBuilder.startIntField(
                Component.literal("Whitelist/Blacklist XP Cost (Levels)"),
                Config.WHITELIST_BLACKLIST_COST_XP.get())
                .setDefaultValue(20)
                .setMin(0)
                .setSaveConsumer(Config.WHITELIST_BLACKLIST_COST_XP::set)
                .build());
        guiSettings.addEntry(entryBuilder.startDoubleField(
                Component.literal("Allow All Power Cost"),
                Config.ALLOW_ALL_COST_POWER_POINTS.get())
                .setDefaultValue(5.0)
                .setMin(0.0)
                .setSaveConsumer(Config.ALLOW_ALL_COST_POWER_POINTS::set)
                .build());
        guiSettings.addEntry(entryBuilder.startIntField(
                Component.literal("Allow All XP Cost (Levels)"),
                Config.ALLOW_ALL_COST_XP.get())
                .setDefaultValue(100)
                .setMin(0)
                .setSaveConsumer(Config.ALLOW_ALL_COST_XP::set)
                .build());
        guiSettings.addEntry(entryBuilder.startDoubleField(
                Component.literal("Disable Hostile Power Cost"),
                Config.DISABLE_HOSTILE_COST_POWER_POINTS.get())
                .setDefaultValue(5.0)
                .setMin(0.0)
                .setSaveConsumer(Config.DISABLE_HOSTILE_COST_POWER_POINTS::set)
                .build());
        guiSettings.addEntry(entryBuilder.startIntField(
                Component.literal("Disable Hostile XP Cost (Levels)"),
                Config.DISABLE_HOSTILE_COST_XP.get())
                .setDefaultValue(100)
                .setMin(0)
                .setSaveConsumer(Config.DISABLE_HOSTILE_COST_XP::set)
                .build());
        guiSettings.addEntry(entryBuilder.startDoubleField(
                Component.literal("Dimension Rules Power Cost"),
                Config.DIMENSION_RULES_COST_POWER_POINTS.get())
                .setDefaultValue(5.0)
                .setMin(0.0)
                .setSaveConsumer(Config.DIMENSION_RULES_COST_POWER_POINTS::set)
                .build());
        guiSettings.addEntry(entryBuilder.startIntField(
                Component.literal("Dimension Rules XP Cost (Levels)"),
                Config.DIMENSION_RULES_COST_XP.get())
                .setDefaultValue(100)
                .setMin(0)
                .setSaveConsumer(Config.DIMENSION_RULES_COST_XP::set)
                .build());

        return builder.build();
    }
}
