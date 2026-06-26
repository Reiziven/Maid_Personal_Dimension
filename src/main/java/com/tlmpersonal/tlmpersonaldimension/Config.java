package com.tlmpersonal.tlmpersonaldimension;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public enum DimensionType {
                VOID,
                NORMAL,
                CHERRY
        }

        public static final ModConfigSpec.EnumValue<DimensionType> DIMENSION_TYPE;
        public static final ModConfigSpec.BooleanValue ENABLE_STRUCTURES;
        public static final ModConfigSpec.IntValue MAID_SPAWN_CHANCE;

        // YSM Maid Model Options
        public static final ModConfigSpec.BooleanValue USE_YSM_MODELS;
        public static final ModConfigSpec.ConfigValue<List<? extends String>> YSM_MODEL_IDS;

        // Entity Protection
        public static final ModConfigSpec.BooleanValue IMMEDIATE_REMOVAL;
        public static final ModConfigSpec.BooleanValue REMOVE_BLOCKED_ENTITIES;
        public static final ModConfigSpec.BooleanValue ALLOW_ALL_ENTITIES;
        public static final ModConfigSpec.BooleanValue DISABLE_HOSTILE_ENTITIES;
        public static final ModConfigSpec.BooleanValue ENTITY_WHITELIST_MODE;
        public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_ENTITIES;
        public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_ENTITIES;

        // Block Interaction
        public static final ModConfigSpec.BooleanValue ENABLE_BLOCK_BREAKING;
        public static final ModConfigSpec.BooleanValue ENABLE_BLOCK_BUILDING;

        // Teleportation Restrictions
        public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_WHITELIST;

        // Advanced Protection
        public static final ModConfigSpec.ConfigValue<List<? extends String>> DAMAGE_SOURCE_BLACKLIST;
        public static final ModConfigSpec.DoubleValue FALL_PROTECTION_Y;

        // Ground Level Teleportation
        public static final ModConfigSpec.BooleanValue TELEPORT_TO_GROUND;

        // Sleep and Spawn
        public static final ModConfigSpec.BooleanValue ALLOW_SET_SPAWN;

        // Maid Protection
        public static final ModConfigSpec.BooleanValue TAMED_MAID_PROTECTION_ENABLED;
        public static final ModConfigSpec.BooleanValue TAMED_MAID_PROTECTION;
        public static final ModConfigSpec.IntValue TAMED_MAID_PROTECTION_RANGE;
        public static final ModConfigSpec.IntValue TAMED_MAID_PROTECTION_COOLDOWN;
        public static final ModConfigSpec.DoubleValue TAMED_MAID_PROTECTION_POWER_POINTS_COST;
        public static final ModConfigSpec.IntValue TAMED_MAID_PROTECTION_XP_COST;
        public static final ModConfigSpec.BooleanValue ALL_MAID_PROTECTION;
        public static final ModConfigSpec.BooleanValue WILD_MAID_PROTECTION;
        public static final ModConfigSpec.BooleanValue MAID_AUTHORITY;
        public static final ModConfigSpec.BooleanValue MAID_TELEPORT_WITH_OWNER_DIMENSION;
        public static final ModConfigSpec.BooleanValue MAID_ATTACK_DISCARD;

        // Entity Teleportation with Items
        public static final ModConfigSpec.BooleanValue ALLOW_ENTITY_TELEPORT;
        public static final ModConfigSpec.BooleanValue MAID_TELEPORTER_ALLOW_ALL_ENTITIES;
        public static final ModConfigSpec.BooleanValue MAID_TELEPORTER_ENTITY_WHITELIST_MODE;
        public static final ModConfigSpec.ConfigValue<List<? extends String>> MAID_TELEPORTER_ALLOWED_ENTITIES;
        public static final ModConfigSpec.ConfigValue<List<? extends String>> MAID_TELEPORTER_BLOCKED_ENTITIES;
        public static final ModConfigSpec.BooleanValue MAID_TELEPORTER_EXCLUDE_BOSSES;

        // General Survival Options
        public static final ModConfigSpec.BooleanValue DISABLE_HUNGER;
        public static final ModConfigSpec.BooleanValue DISABLE_MAID_DEATH;
        public static final ModConfigSpec.BooleanValue DISABLE_PLAYER_DEATH;
        public static final ModConfigSpec.BooleanValue NATURAL_HEALING;
        public static final ModConfigSpec.BooleanValue BLOCK_HARMFUL_EFFECTS;
        public static final ModConfigSpec.BooleanValue MAID_EMIT_LIGHT;
        public static final ModConfigSpec.BooleanValue ENTITY_CANNOT_TARGET;

        // Personal Dimension Teleportation
        public static final ModConfigSpec.BooleanValue TELEPORT_HAS_COST;
        public static final ModConfigSpec.DoubleValue TELEPORT_COST_POWER_POINTS;
        public static final ModConfigSpec.IntValue TELEPORT_COST_XP;
        public static final ModConfigSpec.IntValue TELEPORT_JOIN_COOLDOWN;
        public static final ModConfigSpec.IntValue TELEPORT_LEAVE_COOLDOWN;

        // Private Dimension Settings
        public static final ModConfigSpec.BooleanValue PRIVATE_DIMENSION;

        // Entity Allowance via Maid
        public static final ModConfigSpec.BooleanValue ALLOW_CHEAT_CONFIGS;
        public static final ModConfigSpec.BooleanValue ALLOW_FREE_WHITELIST;
        public static final ModConfigSpec.BooleanValue ALLOW_ALLOW_ALL_ENTITIES;
        public static final ModConfigSpec.IntValue MAID_ENTITY_WHITELIST_COOLDOWN_MINUTES;
        public static final ModConfigSpec.DoubleValue WHITELIST_BLACKLIST_COST_POWER_POINTS;
        public static final ModConfigSpec.IntValue WHITELIST_BLACKLIST_COST_XP;
        public static final ModConfigSpec.DoubleValue ALLOW_ALL_COST_POWER_POINTS;
        public static final ModConfigSpec.IntValue ALLOW_ALL_COST_XP;
        public static final ModConfigSpec.DoubleValue DISABLE_HOSTILE_COST_POWER_POINTS;
        public static final ModConfigSpec.IntValue DISABLE_HOSTILE_COST_XP;
        public static final ModConfigSpec.DoubleValue DIMENSION_RULES_COST_POWER_POINTS;
        public static final ModConfigSpec.IntValue DIMENSION_RULES_COST_XP;

        // GUI Layout Options
        public static final ModConfigSpec.BooleanValue USE_MAID_GUI_CONTROLS;

        // Bauble Crafting
        public static final ModConfigSpec.BooleanValue DOMAIN_EXPANSION_BAUBLE_CRAFTABLE;
        public static final ModConfigSpec.BooleanValue CHERRY_DOMAIN_BAUBLE_CRAFTABLE;
        public static final ModConfigSpec.BooleanValue CAT_FAMILIAR_BAUBLE_CRAFTABLE;

        // Domain Expansion Bauble
        public static final ModConfigSpec.BooleanValue DOMAIN_EXPANSION_ENABLED;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_COOLDOWN_SECONDS;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_DURATION_SECONDS;
        public static final ModConfigSpec.BooleanValue DOMAIN_EXPANSION_USE_DIMENSION_RULES;
        public static final ModConfigSpec.BooleanValue DOMAIN_EXPANSION_USE_ENTITY_PROTECTION;
        public static final ModConfigSpec.BooleanValue CHERRY_DOMAIN_AFFECTS_OWNER;
public static final ModConfigSpec.IntValue CHERRY_DOMAIN_HORIZONTAL_RADIUS;
public static final ModConfigSpec.IntValue CHERRY_DOMAIN_VERTICAL_HALF;
        public static final ModConfigSpec.IntValue CHERRY_DOMAIN_RULES_BYPASS_CHANCE;
        public static final ModConfigSpec.BooleanValue CHERRY_DOMAIN_XP_COST_ENABLED;
        public static final ModConfigSpec.IntValue CHERRY_DOMAIN_XP_COST;
        public static final ModConfigSpec.IntValue CHERRY_DOMAIN_XP_COST_INTERVAL_SECONDS;
        public static final ModConfigSpec.BooleanValue DOMAIN_EXPANSION_XP_COST_ENABLED;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_XP_COST;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_XP_COST_INTERVAL_SECONDS;
        public static final ModConfigSpec.DoubleValue DOMAIN_EXPANSION_MIN_DISTANCE;
        public static final ModConfigSpec.BooleanValue DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING;
        public static final ModConfigSpec.BooleanValue CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING;
        public static final ModConfigSpec.ConfigValue<String> DOMAIN_EXPANSION_STRUCTURE;
        // Domain Expansion buff/debuff amplifiers (0 = level I, 1 = level II, etc.)
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_ALLY_STRENGTH;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_ALLY_REGEN;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_ALLY_RESISTANCE;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_ENEMY_WEAKNESS;
        public static final ModConfigSpec.IntValue DOMAIN_EXPANSION_ENEMY_SLOWNESS;

        // Cat Familiar Bauble
        public static final ModConfigSpec.BooleanValue CAT_FAMILIAR_EFFECT_COOLDOWN;
        public static final ModConfigSpec.BooleanValue CAT_FAMILIAR_ATTACKS_ENTITIES;
        public static final ModConfigSpec.IntValue CAT_FAMILIAR_REVIVAL_COOLDOWN;
        public static final ModConfigSpec.BooleanValue CAT_FAMILIAR_DETECT_HOSTILES;
        public static final ModConfigSpec.BooleanValue CAT_FAMILIAR_DETECT_HOSTILES_CHAT;

        static {
                BUILDER.push("General Settings");
                DIMENSION_TYPE = BUILDER.defineEnum("dimensionType", DimensionType.VOID);
                ENABLE_STRUCTURES = BUILDER.define("enableStructures", true);
                MAID_SPAWN_CHANCE = BUILDER
                                .comment("Rare chance per chunk for a maid to spawn naturally in OVERWORLD or CHERRY dimensions (1 in x chunks). Set to 0 to disable.")
                                .defineInRange("maidSpawnChance", 100, 0, 1000);
                TELEPORT_TO_GROUND = BUILDER.define("teleportToGround", true);
                ALLOW_SET_SPAWN = BUILDER.define("allowSetSpawn", false);
                ENABLE_BLOCK_BREAKING = BUILDER.define("enableBlockBreaking", true);
                ENABLE_BLOCK_BUILDING = BUILDER
                                .comment("If ON, players are allowed to place blocks in the dimension.")
                                .define("enableBlockBuilding", true);
                BUILDER.pop();

                BUILDER.push("YSM Maid Model Settings");
                USE_YSM_MODELS = BUILDER
                                .comment("If true, spawned maids will use YSM (Yes Steve Model) models if available")
                                .define("useYsmModels", false);
                YSM_MODEL_IDS = BUILDER
                                .comment("List of YSM model IDs to randomly choose from (e.g., [\"default\", \"misc/1_alex\", \"misc/2_steve\"]). Use /ysm model set @s to find your model's location and ID!")
                                .defineList("ysmModelIds", () -> List.of("default"), o -> o instanceof String);
                BUILDER.pop();

                BUILDER.push("Entity Spawning Protection");
                IMMEDIATE_REMOVAL = BUILDER.define("immediateRemoval", true);
                REMOVE_BLOCKED_ENTITIES = BUILDER.define("removeBlockedEntities", false);
                ALLOW_ALL_ENTITIES = BUILDER
                                .comment("If true, all entities are allowed except those in the blockedEntities list or restricted categories.")
                                .define("allowAllEntities", false);
                DISABLE_HOSTILE_ENTITIES = BUILDER
                                .comment("Completely prevent hostile mobs from existing")
                                .define("disableHostileEntities", false);
                ENTITY_WHITELIST_MODE = BUILDER.define("entityWhitelistMode", true);
                ALLOWED_ENTITIES = BUILDER.defineList("allowedEntities",
                                () -> List.of("touhou_little_maid:broom","touhou_little_maid:chair"),
                                o -> o instanceof String);
                BLOCKED_ENTITIES = BUILDER.defineList("blockedEntities", ArrayList::new, o -> o instanceof String);
                BUILDER.pop();

                BUILDER.push("Advanced Protection");
                DAMAGE_SOURCE_BLACKLIST = BUILDER.defineList("damageSourceBlacklist", ArrayList::new,
                                o -> o instanceof String);
                FALL_PROTECTION_Y = BUILDER.defineInRange("fallProtectionY", -64.0, -128.0, 320.0);
                BUILDER.pop();

                BUILDER.push("Teleportation Restrictions");
                DIMENSION_WHITELIST = BUILDER.defineList("dimensionWhitelist",
                                () -> List.of("minecraft:overworld", "minecraft:overworld", "minecraft:the_end"),
                                o -> o instanceof String);
                BUILDER.pop();

                BUILDER.push("Maid Protection");
                TAMED_MAID_PROTECTION_ENABLED = BUILDER
                                .comment("If OFF, tamed maid protection is completely disabled and the option won't appear in the GUI.")
                                .define("tamedMaidProtectionEnabled", true);
                TAMED_MAID_PROTECTION = BUILDER
                                .comment("If ON, tamed maids will expel entities that are about to kill their owner or other tamed maids. Maids need to be near the owner (configurable range).")
                                .define("tamedMaidProtection", true);
                TAMED_MAID_PROTECTION_RANGE = BUILDER
                                .comment("Range in blocks that tamed maids will protect their owner from lethal threats.")
                                .defineInRange("tamedMaidProtectionRange", 20, 1, 100);
                TAMED_MAID_PROTECTION_COOLDOWN = BUILDER
                                .comment("Cooldown in seconds for tamed maid protection to trigger again.")
                                .defineInRange("tamedMaidProtectionCooldown", 60, 0, 1000000);
                TAMED_MAID_PROTECTION_POWER_POINTS_COST = BUILDER
                                .comment("Cost in power points for tamed maid protection to trigger (0 to disable).")
                                .defineInRange("tamedMaidProtectionPowerPointsCost", 0.0, 0.0, 1000000.0);
                TAMED_MAID_PROTECTION_XP_COST = BUILDER
                                .comment("Cost in experience levels for tamed maid protection to trigger (0 to disable).")
                                .defineInRange("tamedMaidProtectionXpCost", 0, 0, 1000000);
                ALL_MAID_PROTECTION = BUILDER
                                .comment("If ON, all maids (including non-tamed) will expel entities that are about to kill any player or maid.")
                                .define("allMaidProtection", false);
                WILD_MAID_PROTECTION = BUILDER
                                .comment("If ON, non-tamed (wild) maids will expel entities (including players) that are about to kill them.")
                                .define("wildMaidProtection", true);
                MAID_AUTHORITY = BUILDER
                                .comment("If ON, some entities will attack what is attacking maid and player.")
                                .define("maidAuthority", false);
                MAID_TELEPORT_WITH_OWNER_DIMENSION = BUILDER
                                .comment("If ON, tamed maids following their owner will teleport with them through any dimension, regardless of distance.")
                                .define("maidTeleportWithOwnerDimension", false);
                MAID_ATTACK_DISCARD = BUILDER
                                .comment("If ON, maids in the personal dimension will discard entities they attack or are hostile towards.")
                                .define("maidAttackDiscard", false);
                BUILDER.pop();

                BUILDER.push("Entity Teleportation with Items");
                ALLOW_ENTITY_TELEPORT = BUILDER
                                .comment("If ON, players can teleport entities by shift-right-clicking them with Okina's door or maid teleporter.")
                                .define("allowEntityTeleport", true);
                MAID_TELEPORTER_ALLOW_ALL_ENTITIES = BUILDER
                                .comment("If true, all entities are allowed to be teleported with maid teleporter (unless blacklisted or excluded as a boss).")
                                .define("maidTeleporterAllowAllEntities", true);
                MAID_TELEPORTER_ENTITY_WHITELIST_MODE = BUILDER
                                .comment("If true, only entities in maid teleporter whitelist are allowed; if false, only entities in blacklist are blocked.")
                                .define("maidTeleporterEntityWhitelistMode", true);
                MAID_TELEPORTER_ALLOWED_ENTITIES = BUILDER
                                .comment("List of entity IDs allowed to be teleported with maid teleporter (e.g., 'touhou_little_maid:entity_maid', 'minecraft:cow').")
                                .defineList("maidTeleporterAllowedEntities", ArrayList::new, o -> true);
                MAID_TELEPORTER_BLOCKED_ENTITIES = BUILDER
                                .comment("List of entity IDs blocked from being teleported with maid teleporter (higher priority than whitelist).")
                                .defineList("maidTeleporterBlockedEntities", ArrayList::new, o -> true);
                MAID_TELEPORTER_EXCLUDE_BOSSES = BUILDER
                                .comment("If true, entities tagged as bosses (e.g., #c:bosses, #neoforge:bosses) cannot be teleported with maid teleporter.")
                                .define("maidTeleporterExcludeBosses", true);
                BUILDER.pop();

                BUILDER.push("General Survival Options");
                DISABLE_HUNGER = BUILDER
                                .comment("If ON, players in the personal dimension won't get hungry.")
                                .define("disableHunger", false);
                DISABLE_MAID_DEATH = BUILDER
                                .comment("If ON, maids in the personal dimension can't die.")
                                .define("disableMaidDeath", false);
                DISABLE_PLAYER_DEATH = BUILDER
                                .comment("If ON, players in the personal dimension can't die (they'll stay at 1 health instead).")
                                .define("disablePlayerDeath", false);
                NATURAL_HEALING = BUILDER
                                .comment("If ON, players and maids in the personal dimension will naturally regenerate health over time.")
                                .define("naturalHealing", false);
                BLOCK_HARMFUL_EFFECTS = BUILDER
                                .comment("If ON, harmful potion effects will be completely blocked from affecting players and maids in the dimension.")
                                .define("blockHarmfulEffects", false);
                MAID_EMIT_LIGHT = BUILDER
                                .comment("If ON, maids in the personal dimension will emit light.")
                                .define("maidEmitLight", false);
                ENTITY_CANNOT_TARGET = BUILDER
                                .comment("If ON, Entities in the personaldimension are neutral (except maids)")
                                .define("entityCannotTarget", false);
                BUILDER.pop();

                BUILDER.push("Teleportation Costs");
                TELEPORT_HAS_COST = BUILDER
                                .comment("If true, teleporting (GUI or Item) costs power points and XP")
                                .define("teleportHasCost", true);
                TELEPORT_COST_POWER_POINTS = BUILDER
                                .comment("Cost in power points (float) to use personal dimension teleportation (0.0 to disable)")
                                .defineInRange("teleportCostPowerPoints", 0.16, 0.0, 1000000.0);
                TELEPORT_COST_XP = BUILDER
                                .comment("Cost in experience levels to use personal dimension teleportation (0 to disable)")
                                .defineInRange("teleportCostXp", 5, 0, 1000000);
                TELEPORT_JOIN_COOLDOWN = BUILDER
                                .comment("Cooldown in seconds to join the personal dimension (0 to disable)")
                                .defineInRange("teleportJoinCooldown", 300, 0, 1000000);
                TELEPORT_LEAVE_COOLDOWN = BUILDER
                                .comment("Cooldown in seconds to leave the personal dimension (0 to disable)")
                                .defineInRange("teleportLeaveCooldown", 0, 0, 1000000);
                BUILDER.pop();

                BUILDER.push("Private Dimension Settings");
                PRIVATE_DIMENSION = BUILDER
                                .comment("If true, each player's personal dimension is private, and teleporter items are bound to their owner")
                                .define("privateDimension", true);
                BUILDER.pop();

                BUILDER.push("Maid Entity Allowance & GUI Config");
                ALLOW_CHEAT_CONFIGS = BUILDER
                                .comment("If true, players can configure cheat options via the GUI like disable hunger, disable death, etc.")
                                .define("allowCheatConfigs", true);
                ALLOW_FREE_WHITELIST = BUILDER
                                .comment("If true, players can whitelist entities via GUI without needing cake or maid costs")
                                .define("allowFreeWhitelist", false);
                ALLOW_ALLOW_ALL_ENTITIES = BUILDER
                                .comment("If true, players can set \"allow all entities\" via GUI without needing cake or maid costs")
                                .define("allowAllowAllEntities", false);
                MAID_ENTITY_WHITELIST_COOLDOWN_MINUTES = BUILDER
                                .comment("Cooldown in minutes per maid for whitelisting/allow all entities (5-10 minutes)")
                                .defineInRange("maidEntityWhitelistCooldownMinutes", 7, 5, 10);
                WHITELIST_BLACKLIST_COST_POWER_POINTS = BUILDER
                                .comment("Cost in power points (float) to whitelist/blacklist entities via maid (0.0 to disable)")
                                .defineInRange("whitelistBlacklistCostPowerPoints", 0.50, 0.0, 1000000.0);
                WHITELIST_BLACKLIST_COST_XP = BUILDER
                                .comment("Cost in experience levels to whitelist/blacklist entities via maid (0 to disable)")
                                .defineInRange("whitelistBlacklistCostXp", 20, 0, 1000000);
                ALLOW_ALL_COST_POWER_POINTS = BUILDER
                                .comment("Cost in power points (float) to set allow all entities via maid (0.0 to disable)")
                                .defineInRange("allowAllCostPowerPoints", 5.0, 0.0, 1000000.0);
                ALLOW_ALL_COST_XP = BUILDER
                                .comment("Cost in experience levels to set allow all entities via maid (0 to disable)")
                                .defineInRange("allowAllCostXp", 100, 0, 1000000);
                DISABLE_HOSTILE_COST_POWER_POINTS = BUILDER
                                .comment("Cost in power points (float) to disable hostile entities via maid (0.0 to disable)")
                                .defineInRange("disableHostileCostPowerPoints", 5.0, 0.0, 1000000.0);
                DISABLE_HOSTILE_COST_XP = BUILDER
                                .comment("Cost in experience levels to disable hostile entities via maid (0 to disable)")
                                .defineInRange("disableHostileCostXp", 100, 0, 1000000);
                DIMENSION_RULES_COST_POWER_POINTS = BUILDER
                                .comment("Cost in power points (float) to change dimension survival/time rules in GUI")
                                .defineInRange("dimensionRulesCostPowerPoints", 5.0, 0.0, 1000000.0);
                DIMENSION_RULES_COST_XP = BUILDER
                                .comment("Cost in experience levels to change dimension survival/time rules in GUI")
                                .defineInRange("dimensionRulesCostXp", 100, 0, 1000000);
                BUILDER.pop();

                BUILDER.push("GUI Layout Options");
                USE_MAID_GUI_CONTROLS = BUILDER
                                .comment("If true, the buttons will be directly inside the maid's GUI (4th tab). If false, clicking the 4th tab's art will open the full-screen mod GUI.")
                                .define("useMaidGuiControls", false);
                BUILDER.pop();

                BUILDER.push("Bauble Configuration");

                BUILDER.push("Bauble Crafting");
                DOMAIN_EXPANSION_BAUBLE_CRAFTABLE = BUILDER
                                .comment("If true, the Domain Expansion Bauble can be crafted at the altar in survival. Still obtainable via commands.")
                                .define("domainExpansionBaubleCraftable", true);
                CHERRY_DOMAIN_BAUBLE_CRAFTABLE = BUILDER
                                .comment("If true, the Cherry Domain Bauble can be crafted at the altar in survival. Still obtainable via commands.")
                                .define("cherryDomainBaubleCraftable", true);
                CAT_FAMILIAR_BAUBLE_CRAFTABLE = BUILDER
                                .comment("If true, the Cat Familiar Bauble can be crafted at the altar in survival. Still obtainable via commands.")
                                .define("catFamiliarBaubleCraftable", true);
                BUILDER.pop();

                BUILDER.push("Domain Expansion Bauble");
                DOMAIN_EXPANSION_ENABLED = BUILDER
                                .comment("If true, maids with the Domain Expansion bauble and favorability level 3 can activate domain expansions.")
                                .define("domainExpansionEnabled", true);
                DOMAIN_EXPANSION_COOLDOWN_SECONDS = BUILDER
                                .comment("Cooldown in seconds before a maid can activate a domain expansion again.")
                                .defineInRange("domainExpansionCooldownSeconds", 300, 0, 1000000);
                DOMAIN_EXPANSION_DURATION_SECONDS = BUILDER
                                .comment("How long (in seconds) the domain expansion lasts before collapsing.")
                                .defineInRange("domainExpansionDurationSeconds", 60, -1, 3600);
                DOMAIN_EXPANSION_XP_COST_ENABLED = BUILDER
                                .comment("If true, Domain Expansion consumes XP levels from its owner over time.")
                                .define("domainExpansionXpCostEnabled", true);
                DOMAIN_EXPANSION_XP_COST = BUILDER
                                .comment("How many experience levels to consume per interval.")
                                .defineInRange("domainExpansionXpCost", 1, 0, 100);
                DOMAIN_EXPANSION_XP_COST_INTERVAL_SECONDS = BUILDER
                                .comment("How often (in seconds) the Domain Expansion consumes XP.")
                                .defineInRange("domainExpansionXpCostIntervalSeconds", 10, 1, 3600);
                DOMAIN_EXPANSION_USE_DIMENSION_RULES = BUILDER
                                .comment("If true, domain expansions enforce dimension-like rules: maid authority, natural healing etc. Can be overridden per-player via GUI.")
                                .define("domainExpansionUseDimensionRules", true);
                DOMAIN_EXPANSION_USE_ENTITY_PROTECTION = BUILDER
                                .comment("If true, domain expansions apply combat effects: allies get buffs (Strength III, Regen II, Resistance III, Absorption IV), enemies get Weakness II and Slowness V. Can be overridden per-player via GUI.")
                                .define("domainExpansionUseEntityProtection", true);
                CHERRY_DOMAIN_AFFECTS_OWNER = BUILDER
                        .comment("If true, the Cherry Domain Bauble also creates an aura around the owner.")
                        .define("cherryDomainAffectsOwner", false);
                CHERRY_DOMAIN_HORIZONTAL_RADIUS = BUILDER
                        .comment("Horizontal radius for Cherry Domain block placement (half width). 2 gives a 5x5 area. NOTE tat high value can cause lag")
                        .defineInRange("cherryDomainHorizontalRadius", 2, 0, 100);
                CHERRY_DOMAIN_VERTICAL_HALF = BUILDER
                        .comment("Vertical half-range for Cherry Domain (half of total height). 10 gives a 20 block tall area. NOTE tat high value can cause lag")
                        .defineInRange("cherryDomainVerticalHalf", 10, 0, 256);
                CHERRY_DOMAIN_RULES_BYPASS_CHANCE = BUILDER
                        .comment("Percent chance (0-100) that Cherry Domain dimension rules are skipped on any given check. Default 20 means 20% chance rules won't apply.")
                        .defineInRange("cherryDomainRulesBypassChance", 30, 0, 100);
                CHERRY_DOMAIN_XP_COST_ENABLED = BUILDER
                        .comment("If true, Cherry Domain consumes XP levels from its owner over time.")
                        .define("cherryDomainXpCostEnabled", true);
                CHERRY_DOMAIN_XP_COST = BUILDER
                        .comment("How many experience levels to consume per interval.")
                        .defineInRange("cherryDomainXpCost", 1, 0, 100);
                CHERRY_DOMAIN_XP_COST_INTERVAL_SECONDS = BUILDER
                        .comment("How often (in seconds) the Cherry Domain consumes XP. Default is 120 (2 minutes).")
                        .defineInRange("cherryDomainXpCostIntervalSeconds", 120, 1, 3600);
                DOMAIN_EXPANSION_MIN_DISTANCE = BUILDER
                                .comment("Minimum distance between active domains to prevent overlapping.")
                                .defineInRange("domainExpansionMinDistance", 100.0, 10.0, 10000.0);
                DOMAIN_EXPANSION_ENABLE_BLOCK_BREAKING = BUILDER
                                .comment("If false, blocks cannot be broken inside a domain expansion.")
                                .define("domainExpansionEnableBlockBreaking", false);
                CHERRY_DOMAIN_ENABLE_BLOCK_BREAKING = BUILDER
                                .comment("If false, blocks cannot be broken inside a cherry domain.")
                                .define("cherryDomainEnableBlockBreaking", false);
                DOMAIN_EXPANSION_STRUCTURE = BUILDER
                                .comment("Which structure to use for Domain Expansion (\"domain_expansion\" or \"my_island\").")
                                .define("domainExpansionStructure", "domain_expansion");
                DOMAIN_EXPANSION_ALLY_STRENGTH = BUILDER
                                .comment("Strength amplifier for allies inside domain expansion (0=I, 1=II, 2=III...).")
                                .defineInRange("domainExpansionAllyStrength", 1, 0, 255);
                DOMAIN_EXPANSION_ALLY_REGEN = BUILDER
                                .comment("Regeneration amplifier for allies inside domain expansion (0=I, 1=II...).")
                                .defineInRange("domainExpansionAllyRegen", 1, 0, 255);
                DOMAIN_EXPANSION_ALLY_RESISTANCE = BUILDER
                                .comment("Resistance amplifier for allies inside domain expansion (0=I, 1=II, 2=III...).")
                                .defineInRange("domainExpansionAllyResistance", 1, 0, 255);
                DOMAIN_EXPANSION_ENEMY_WEAKNESS = BUILDER
                                .comment("Weakness amplifier for enemies inside domain expansion (0=I, 1=II...).")
                                .defineInRange("domainExpansionEnemyWeakness", 0, 0, 255);
                DOMAIN_EXPANSION_ENEMY_SLOWNESS = BUILDER
                                .comment("Slowness amplifier for enemies inside domain expansion (0=I ... 9=X...).")
                                .defineInRange("domainExpansionEnemySlowness", 0, 0, 255);
                BUILDER.pop();

                BUILDER.push("Cat Familiar Bauble");
                CAT_FAMILIAR_EFFECT_COOLDOWN = BUILDER
                                .comment("If true, cat familiar beneficial effects have a cooldown between applications.")
                                .define("catFamiliarEffectCooldown", true);
                CAT_FAMILIAR_ATTACKS_ENTITIES = BUILDER
                                .comment("If true, cat familiar will attack entities that attack the maid.")
                                .define("catFamiliarAttacksEntities", true);
                CAT_FAMILIAR_REVIVAL_COOLDOWN = BUILDER
                                .comment("Cooldown in seconds before a dead cat familiar can be revived (respawned) by the maid.")
                                .defineInRange("catFamiliarRevivalCooldown", 600, 0, 1000000);
                CAT_FAMILIAR_DETECT_HOSTILES = BUILDER
                                .comment("If true, the cat familiar will detect hostile mobs within 128 blocks, apply Glowing for 30 seconds, and optionally whisper coordinates to the owner.")
                                .define("catFamiliarDetectHostiles", false);
                CAT_FAMILIAR_DETECT_HOSTILES_CHAT = BUILDER
                                .comment("If true, the cat familiar will also send a chat whisper with coordinates when detecting a hostile mob. Requires catFamiliarDetectHostiles to be true.")
                                .define("catFamiliarDetectHostilesChat", false);
                BUILDER.pop();
                
                BUILDER.pop(); // End Bauble Configuration
        }

        public static final ModConfigSpec SPEC = BUILDER.build();
}
