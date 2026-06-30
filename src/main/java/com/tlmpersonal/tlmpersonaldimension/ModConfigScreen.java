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
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, all structures generate except blacklisted ones. If false, only whitelisted structures generate (my_island always allowed)."))
                .setSaveConsumer(Config.ENABLE_STRUCTURES::set)
                .build());
        general.addEntry(entryBuilder.startStrList(
                Component.literal("Structure Whitelist"),
                new ArrayList<>(Config.STRUCTURE_WHITELIST.get()))
                .setDefaultValue(List.of("touhoulittlemaidpersonaldimension:my_island"))
                .setTooltip(Component.literal("When Enable Structures is OFF, only these structure IDs will generate. my_island is always allowed regardless."))
                .setSaveConsumer(Config.STRUCTURE_WHITELIST::set)
                .build());
        general.addEntry(entryBuilder.startStrList(
                Component.literal("Structure Blacklist"),
                new ArrayList<>(Config.STRUCTURE_BLACKLIST.get()))
                .setDefaultValue(List.of())
                .setTooltip(Component.literal("When Enable Structures is ON, these structure IDs will be blocked from generating."))
                .setSaveConsumer(Config.STRUCTURE_BLACKLIST::set)
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
                Component.literal("Maid Teleporter: Allow All Entities"),
                Config.MAID_TELEPORTER_ALLOW_ALL_ENTITIES.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, all entities can be teleported with the maid teleporter unless blacklisted or a boss."))
                .setSaveConsumer(Config.MAID_TELEPORTER_ALLOW_ALL_ENTITIES::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Maid Teleporter: Whitelist Mode"),
                Config.MAID_TELEPORTER_ENTITY_WHITELIST_MODE.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, only entities in the allowed list can be teleported. If false, all except the blocked list."))
                .setSaveConsumer(Config.MAID_TELEPORTER_ENTITY_WHITELIST_MODE::set)
                .build());
        general.addEntry(entryBuilder.startStrList(
                Component.literal("Maid Teleporter: Allowed Entities"),
                new ArrayList<>(Config.MAID_TELEPORTER_ALLOWED_ENTITIES.get()))
                .setDefaultValue(List.of())
                .setTooltip(Component.literal("Entity IDs allowed to be teleported with maid teleporter (e.g. 'minecraft:cow')."))
                .setSaveConsumer(Config.MAID_TELEPORTER_ALLOWED_ENTITIES::set)
                .build());
        general.addEntry(entryBuilder.startStrList(
                Component.literal("Maid Teleporter: Blocked Entities"),
                new ArrayList<>(Config.MAID_TELEPORTER_BLOCKED_ENTITIES.get()))
                .setDefaultValue(List.of())
                .setTooltip(Component.literal("Entity IDs blocked from being teleported with maid teleporter (higher priority than whitelist)."))
                .setSaveConsumer(Config.MAID_TELEPORTER_BLOCKED_ENTITIES::set)
                .build());
        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Maid Teleporter: Exclude Bosses"),
                Config.MAID_TELEPORTER_EXCLUDE_BOSSES.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, boss entities cannot be teleported with the maid teleporter."))
                .setSaveConsumer(Config.MAID_TELEPORTER_EXCLUDE_BOSSES::set)
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
                .setTooltip(Component.literal("List of YSM model IDs to randomly choose from List of YSM model IDs to randomly choose from (e.g., [\"default\", \"misc/1_alex\", \"misc/2_steve\"]). Use /ysm model set @s to find your model's location and ID!"))
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
                .setDefaultValue(List.of("touhou_little_maid:broom","touhou_little_maid:chair", "touhoulittlemaidpersonaldimension:cat_familiar"))
                .setTooltip(Component.literal("Use entity IDs like 'minecraft:zombie' or wildcards like 'minecraft:*' to allow all entities from a mod."))
                .setSaveConsumer(Config.ALLOWED_ENTITIES::set)
                .build());
        entityProtection.addEntry(entryBuilder.startStrList(Component.literal("Blocked Entities"), new ArrayList<>(Config.BLOCKED_ENTITIES.get()))
                .setDefaultValue(List.of())
                .setTooltip(Component.literal("Use entity IDs like 'minecraft:zombie' or wildcards like 'minecraft:*' to block all entities from a mod."))
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
        maidProtection.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Maid Attack Discard"),
                Config.MAID_ATTACK_DISCARD.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If ON, maids in the personal dimension will discard entities they attack or are hostile towards."))
                .setSaveConsumer(Config.MAID_ATTACK_DISCARD::set)
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

        ConfigCategory domainExpansion = builder.getOrCreateCategory(Component.literal("Domain Expansion Bauble"));
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Domain Expansion Enabled"),
                Config.DOMAIN_EXPANSION_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, maids with the Domain Expansion bauble and favorability level 3 can activate domain expansions."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_ENABLED::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntField(
                Component.literal("Domain Expansion Cooldown (Seconds)"),
                Config.DOMAIN_EXPANSION_COOLDOWN_SECONDS.get())
                .setDefaultValue(300)
                .setMin(0)
                .setTooltip(Component.literal("Cooldown in seconds before a maid can activate a domain expansion again."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_COOLDOWN_SECONDS::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntField(
                Component.literal("Domain Expansion Duration (Seconds)"),
                Config.DOMAIN_EXPANSION_DURATION_SECONDS.get())
                .setDefaultValue(60)
                .setMin(-1)
                .setMax(3600)
                .setTooltip(Component.literal("How long (in seconds) the domain expansion lasts before collapsing."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_DURATION_SECONDS::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Use Dimension Rules"),
                Config.DOMAIN_EXPANSION_USE_DIMENSION_RULES.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, domain expansion enforce dimension-like rules: maid authority, natural healing etc."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_USE_DIMENSION_RULES::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Use Entity Protection"),
                Config.DOMAIN_EXPANSION_USE_ENTITY_PROTECTION.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, domain expansion apply combat effects: allies get buffs, enemies get debuffs."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_USE_ENTITY_PROTECTION::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Use Entity Filtering"),
                Config.DOMAIN_EXPANSION_USE_ENTITY_FILTERING.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, domain expansion expels or removes blocked entities."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_USE_ENTITY_FILTERING::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cherry Domain Affects Owner"),
                Config.CHERRY_DOMAIN_AFFECTS_OWNER.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, the Cherry Domain Bauble also creates an aura around the owner."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_AFFECTS_OWNER::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cherry Domain: Use Dimension Rules"),
                Config.CHERRY_DOMAIN_USE_DIMENSION_RULES.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, Cherry Domain enforces dimension-like rules: maid authority, natural healing etc."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_USE_DIMENSION_RULES::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cherry Domain: Use Entity Protection"),
                Config.CHERRY_DOMAIN_USE_ENTITY_PROTECTION.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, Cherry Domain applies combat effects: allies get buffs, enemies get Weakness and Slowness."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_USE_ENTITY_PROTECTION::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cherry Domain: Use Entity Filtering"),
                Config.CHERRY_DOMAIN_USE_ENTITY_FILTERING.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, Cherry Domain expels or removes blocked entities."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_USE_ENTITY_FILTERING::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Cherry Domain Rules Bypass Chance (%)"),
                Config.CHERRY_DOMAIN_RULES_BYPASS_CHANCE.get(),
                0, 100)
                .setDefaultValue(30)
                .setTooltip(Component.literal("Percent chance that Cherry Domain dimension rules are skipped each check. 20 = 20% chance rules won't apply."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_RULES_BYPASS_CHANCE::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Enable Block Breaking in Cherry Domain"),
                Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If false, blocks cannot be broken inside a cherry domain."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startDoubleField(
                Component.literal("Minimum Domain Distance"),
                Config.DOMAIN_EXPANSION_MIN_DISTANCE.get())
                .setDefaultValue(100.0)
                .setMin(10.0)
                .setMax(10000.0)
                .setTooltip(Component.literal("Minimum distance between active domains to prevent overlapping."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_MIN_DISTANCE::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Enable Block Breaking in Domain"),
                Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If false, blocks cannot be broken inside a domain expansion."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Domain Expansion XP Cost Enabled"),
                Config.DOMAIN_EXPANSION_XP_COST_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, Domain Expansion consumes XP levels over time."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_XP_COST_ENABLED::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntField(
                Component.literal("Domain Expansion XP Cost (Levels)"),
                Config.DOMAIN_EXPANSION_XP_COST.get())
                .setDefaultValue(1)
                .setMin(0)
                .setMax(100)
                .setTooltip(Component.literal("How many experience levels to consume per interval."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_XP_COST::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntField(
                Component.literal("Domain Expansion XP Cost Interval (Seconds)"),
                Config.DOMAIN_EXPANSION_XP_COST_INTERVAL_SECONDS.get())
                .setDefaultValue(10)
                .setMin(1)
                .setMax(3600)
                .setTooltip(Component.literal("How often (in seconds) the Domain Expansion consumes XP."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_XP_COST_INTERVAL_SECONDS::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Ally Strength Amplifier"),
                Config.DOMAIN_EXPANSION_ALLY_STRENGTH.get(), 0, 255)
                .setDefaultValue(1)
                .setTooltip(Component.literal("Strength amplifier for allies inside domain expansion (0=I, 1=II...)."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_ALLY_STRENGTH::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Ally Regeneration Amplifier"),
                Config.DOMAIN_EXPANSION_ALLY_REGEN.get(), 0, 255)
                .setDefaultValue(1)
                .setTooltip(Component.literal("Regeneration amplifier for allies inside domain expansion (0=I, 1=II...)."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_ALLY_REGEN::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Ally Resistance Amplifier"),
                Config.DOMAIN_EXPANSION_ALLY_RESISTANCE.get(), 0, 255)
                .setDefaultValue(1)
                .setTooltip(Component.literal("Resistance amplifier for allies inside domain expansion (0=I, 1=II...)."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_ALLY_RESISTANCE::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Enemy Weakness Amplifier"),
                Config.DOMAIN_EXPANSION_ENEMY_WEAKNESS.get(), 0, 255)
                .setDefaultValue(0)
                .setTooltip(Component.literal("Weakness amplifier for enemies inside domain expansion (0=I, 1=II...)."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_ENEMY_WEAKNESS::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Enemy Slowness Amplifier"),
                Config.DOMAIN_EXPANSION_ENEMY_SLOWNESS.get(), 0, 255)
                .setDefaultValue(0)
                .setTooltip(Component.literal("Slowness amplifier for enemies inside domain expansion (0=I, 1=II...)."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_ENEMY_SLOWNESS::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startStrField(
                Component.literal("Domain Expansion Structure"),
                Config.DOMAIN_EXPANSION_STRUCTURE.get())
                .setDefaultValue("domain_expansion")
                .setTooltip(Component.literal("Which structure to use for Domain Expansion: \"domain_expansion\" or \"my_island\"."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_STRUCTURE::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Cherry Domain Horizontal Radius"),
                Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS.get(), 0, 100)
                .setDefaultValue(10)
                .setTooltip(Component.literal("Horizontal radius for Cherry Domain (half-width). High values can cause lag."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_HORIZONTAL_RADIUS::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntSlider(
                Component.literal("Cherry Domain Vertical Half"),
                Config.CHERRY_DOMAIN_VERTICAL_HALF.get(), 0, 256)
                .setDefaultValue(15)
                .setTooltip(Component.literal("Vertical half-range for Cherry Domain (half of total height). High values can cause lag."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_VERTICAL_HALF::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cherry Domain XP Cost Enabled"),
                Config.CHERRY_DOMAIN_XP_COST_ENABLED.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, Cherry Domain consumes XP levels over time."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_XP_COST_ENABLED::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntField(
                Component.literal("Cherry Domain XP Cost (Levels)"),
                Config.CHERRY_DOMAIN_XP_COST.get())
                .setDefaultValue(1)
                .setMin(0)
                .setMax(100)
                .setTooltip(Component.literal("How many experience levels to consume per interval."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_XP_COST::set)
                .build());
        domainExpansion.addEntry(entryBuilder.startIntField(
                Component.literal("Cherry Domain XP Cost Interval (Seconds)"),
                Config.CHERRY_DOMAIN_XP_COST_INTERVAL_SECONDS.get())
                .setDefaultValue(120)
                .setMin(1)
                .setMax(3600)
                .setTooltip(Component.literal("How often (in seconds) the Cherry Domain consumes XP."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_XP_COST_INTERVAL_SECONDS::set)
                .build());

        ConfigCategory catFamiliar = builder.getOrCreateCategory(Component.literal("Cat Familiar Bauble"));
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Effect Application Cooldown"),
                Config.CAT_FAMILIAR_EFFECT_COOLDOWN.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, cat familiar beneficial effects have a cooldown between applications."))
                .setSaveConsumer(Config.CAT_FAMILIAR_EFFECT_COOLDOWN::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cat Can Attack"),
                Config.CAT_FAMILIAR_CAN_ATTACK.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, cat familiar can attack (self-defense, attack maid targets, and owner-related targets if enabled)."))
                .setSaveConsumer(Config.CAT_FAMILIAR_CAN_ATTACK::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startIntSlider(
                Component.literal("Cat Revival Cooldown (seconds)"),
                Config.CAT_FAMILIAR_REVIVAL_COOLDOWN.get(),
                0, 1000000)
                .setDefaultValue(600)
                .setTooltip(Component.literal("Cooldown in seconds before a dead cat familiar can be revived (respawned) by the maid."))
                .setSaveConsumer(Config.CAT_FAMILIAR_REVIVAL_COOLDOWN::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cat Detects Hostile Mobs"),
                Config.CAT_FAMILIAR_DETECT_HOSTILES.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("Cat highlights hostile mobs within 128 blocks with Glowing for 30 seconds. Re-detects same mob after 1 minute."))
                .setSaveConsumer(Config.CAT_FAMILIAR_DETECT_HOSTILES::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cat Detection Chat Whisper"),
                Config.CAT_FAMILIAR_DETECT_HOSTILES_CHAT.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, cat also sends a chat message with exact coordinates when a hostile is detected."))
                .setSaveConsumer(Config.CAT_FAMILIAR_DETECT_HOSTILES_CHAT::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cat Teleports to Target"),
                Config.CAT_FAMILIAR_TELEPORTS_TO_TARGET.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, the cat familiar will teleport to its attacking target when more than 3 blocks away."))
                .setSaveConsumer(Config.CAT_FAMILIAR_TELEPORTS_TO_TARGET::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cat Particles"),
                Config.CAT_FAMILIAR_PARTICLES.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, the cat familiar will spawn witch-like particles when teleporting and spawning."))
                .setSaveConsumer(Config.CAT_FAMILIAR_PARTICLES::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Ignore Follow Range for Attack"),
                Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, the cat familiar will try to attack targets even if they are beyond its follow range, and will not teleport back to the maid until it stops attacking."))
                .setSaveConsumer(Config.CAT_FAMILIAR_IGNORE_FOLLOW_RANGE_FOR_ATTACK::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Mirror Maid Attack"),
                Config.CAT_FAMILIAR_MIRROR_ATTACK.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, the cat familiar will mirror the maid's total attack damage."))
                .setSaveConsumer(Config.CAT_FAMILIAR_MIRROR_ATTACK::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Mirror Maid Health"),
                Config.CAT_FAMILIAR_MIRROR_HEALTH.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If true, the cat familiar will mirror the maid's total health."))
                .setSaveConsumer(Config.CAT_FAMILIAR_MIRROR_HEALTH::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Mirror Maid Defence"),
                Config.CAT_FAMILIAR_MIRROR_DEFENCE.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, the cat familiar will mirror the maid's total defence (armour + toughness)."))
                .setSaveConsumer(Config.CAT_FAMILIAR_MIRROR_DEFENCE::set)
                .build());
        catFamiliar.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cat Attacks Player Targets"),
                Config.CAT_FAMILIAR_ATTACKS_PLAYER_TARGETS.get())
                .setDefaultValue(false)
                .setTooltip(Component.literal("If true, the cat familiar will attack what the maid's owner attacks or is attacked by."))
                .setSaveConsumer(Config.CAT_FAMILIAR_ATTACKS_PLAYER_TARGETS::set)
                .build());

        ConfigCategory baubleCrafting = builder.getOrCreateCategory(Component.literal("Bauble Crafting"));
        baubleCrafting.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Domain Expansion Bauble Craftable"),
                Config.DOMAIN_EXPANSION_BAUBLE_CRAFTABLE.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If false, the Domain Expansion Bauble cannot be crafted at the altar. Still obtainable via commands."))
                .setSaveConsumer(Config.DOMAIN_EXPANSION_BAUBLE_CRAFTABLE::set)
                .build());
        baubleCrafting.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cherry Domain Bauble Craftable"),
                Config.CHERRY_DOMAIN_BAUBLE_CRAFTABLE.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If false, the Cherry Domain Bauble cannot be crafted at the altar. Still obtainable via commands."))
                .setSaveConsumer(Config.CHERRY_DOMAIN_BAUBLE_CRAFTABLE::set)
                .build());
        baubleCrafting.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Cat Familiar Bauble Craftable"),
                Config.CAT_FAMILIAR_BAUBLE_CRAFTABLE.get())
                .setDefaultValue(true)
                .setTooltip(Component.literal("If false, the Cat Familiar Bauble cannot be crafted at the altar. Still obtainable via commands."))
                .setSaveConsumer(Config.CAT_FAMILIAR_BAUBLE_CRAFTABLE::set)
                .build());

        return builder.build();
    }
}
