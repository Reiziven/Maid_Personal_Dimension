# How to Add Custom Dimensions - Simple Guide

## The Key Concept

Your mod **copies dimension generation settings** from template dimension files to create personal dimensions for each player. You're not directly using `minecraft:overworld` - you're creating dimension templates that **copy overworld's generation settings**.

## What You Get Out of the Box

The mod now includes 5 pre-configured dimension templates:

1. **MAID ISLAND** - Floating island in void
2. **OVERWORLD** - Standard overworld terrain  
3. **CHERRY GROVE** - Cherry biome only
4. **NETHER** - Nether-like generation (**NEW!**)
5. **THE END** - End-like generation (**NEW!**)

Players can select any of these via the Maid GUI!

---

## How It Works

### The Template System

```
Template Dimension File (in mod resources)
         ↓
    Defines generation settings (biomes, terrain, structures)
         ↓
    Used as blueprint to create player's personal dimension
         ↓
    Each player gets their own instance with those settings
```

### Example: Adding "Nether" as a Personal Dimension Type

**Step 1:** Create template file
`data/touhoulittlemaidpersonaldimension/dimension/personal_dimension_nether.json`:

```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:noise",
    "settings": "minecraft:nether",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:nether"
    }
  }
}
```

**Step 2:** Add to config:
```toml
customDimensionTemplates = [
    "nether|NETHER|touhoulittlemaidpersonaldimension:personal_dimension_nether"
]
```

**Step 3:** That's it! Players can now select Nether-style generation for their personal dimension.

---

## Adding Your Own Custom Dimensions

### Method 1: Copy Vanilla Generation (Easiest)

Create a dimension file that references vanilla settings:

**Flat World:**
```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "layers": [
        { "block": "minecraft:bedrock", "height": 1 },
        { "block": "minecraft:stone", "height": 3 },
        { "block": "minecraft:grass_block", "height": 1 }
      ],
      "biome": "minecraft:plains"
    }
  }
}
```

**Amplified Terrain:**
```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:noise",
    "settings": "minecraft:amplified",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:overworld"
    }
  }
}
```

**Single Biome (e.g., Desert):**
```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:noise",
    "settings": "minecraft:overworld",
    "biome_source": {
      "type": "minecraft:fixed",
      "biome": "minecraft:desert"
    }
  }
}
```

### Method 2: Reference Other Mod's Dimensions

If you want to copy generation from another mod:

```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:noise",
    "settings": "twilightforest:twilight",
    "biome_source": {
      "type": "twilightforest:twilight_biomes"
    }
  }
}
```

---

## Where to Put Dimension Files

### In Your Mod (Permanent)
Put them in your mod's resources:
```
src/main/resources/data/touhoulittlemaidpersonaldimension/dimension/
    your_custom_dimension.json
```

### As a Datapack (User-Configurable)
Users can add their own via datapack:
```
world/datapacks/customdimensions/
├── pack.mcmeta
└── data/
    └── touhoulittlemaidpersonaldimension/
        └── dimension/
            └── user_custom_dimension.json
```

Then reference in config:
```toml
customDimensionTemplates = [
    "custom|MY DIMENSION|touhoulittlemaidpersonaldimension:user_custom_dimension"
]
```

---

## Complete Examples

### Example 1: Mushroom Island World

**File:** `personal_dimension_mushroom.json`
```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:noise",
    "settings": "minecraft:overworld",
    "biome_source": {
      "type": "minecraft:fixed",
      "biome": "minecraft:mushroom_fields"
    }
  }
}
```

**Config:**
```toml
"mushroom|MUSHROOM ISLAND|touhoulittlemaidpersonaldimension:personal_dimension_mushroom"
```

### Example 2: Superflat Creative

**File:** `personal_dimension_flat_creative.json`
```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "layers": [
        { "block": "minecraft:bedrock", "height": 1 },
        { "block": "minecraft:stone", "height": 2 },
        { "block": "minecraft:grass_block", "height": 1 }
      ],
      "biome": "minecraft:plains",
      "lakes": false,
      "features": false
    }
  }
}
```

**Config:**
```toml
"flat|FLAT WORLD|touhoulittlemaidpersonaldimension:personal_dimension_flat_creative"
```

### Example 3: Ocean World

**File:** `personal_dimension_ocean.json`
```json
{
  "type": "touhoulittlemaidpersonaldimension:personal_dimension",
  "generator": {
    "type": "minecraft:noise",
    "settings": "minecraft:overworld",
    "biome_source": {
      "type": "minecraft:fixed",
      "biome": "minecraft:deep_ocean"
    }
  }
}
```

**Config:**
```toml
"ocean|OCEAN WORLD|touhoulittlemaidpersonaldimension:personal_dimension_ocean"
```

---

## Why You Can't Directly Use `minecraft:overworld`

The code looks for dimension templates in the **dimension registry**, not actual loaded dimensions. `minecraft:overworld` is a loaded dimension instance, not a template definition.

**What doesn't work:**
```toml
"overworld|OVERWORLD|minecraft:overworld"  ❌
```

**What works:**
```toml
"overworld|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal"  ✅
```

The difference: 
- `minecraft:overworld` = The actual overworld dimension (can't be used as template)
- `personal_dimension_normal` = A template that **copies** overworld generation settings

---

## Quick Reference: Vanilla Generation Settings

You can reference these in your dimension files:

### World Generation Settings
- `minecraft:overworld` - Normal terrain
- `minecraft:nether` - Nether terrain
- `minecraft:end` - End terrain
- `minecraft:amplified` - Extreme mountains
- `minecraft:large_biomes` - Bigger biomes

### Biome Sources
- `minecraft:multi_noise` with `preset: minecraft:overworld` - All overworld biomes
- `minecraft:multi_noise` with `preset: minecraft:nether` - All nether biomes
- `minecraft:the_end` - End biome distribution
- `minecraft:fixed` with `biome: <biome_id>` - Single biome only

### Generator Types
- `minecraft:noise` - Standard terrain generation
- `minecraft:flat` - Flat world
- `minecraft:debug` - Debug mode (all blocks)

---

## Testing Your Dimension

1. Add dimension JSON file
2. Update config
3. Restart game/server
4. Open Maid GUI
5. Click "Dim:" button to select your dimension
6. Teleport to test!

---

## Troubleshooting

**Teleport doesn't work / goes to void island:**
- Check dimension file exists in correct location
- Verify JSON is valid (use JSON validator)
- Check logs for "Template LevelStem not found" error
- Ensure config references correct path

**Dimension generates incorrectly:**
- Verify generator settings are correct
- Check biome source configuration
- Test with vanilla presets first

**Config entry not showing in GUI:**
- Check config syntax (correct pipes `|`)
- Restart after config changes
- Check for typos

---

**TL;DR:** Create dimension JSON files that **copy** vanilla/mod generation settings, then reference them in the config. Each player gets their own instance of that dimension type!
