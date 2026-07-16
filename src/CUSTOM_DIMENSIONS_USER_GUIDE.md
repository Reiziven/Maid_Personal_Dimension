# Custom Dimensions User Guide

## What Are Custom Dimensions?

Custom dimensions allow you to configure multiple dimension types that players can choose from for their personal dimension. Instead of being limited to just 3 hardcoded options, you can add as many custom dimensions as you want through the configuration file.

## Quick Start

### Default Dimensions

By default, the mod comes with three dimension types:

1. **MAID ISLAND** (void) - A floating island in the void
2. **OVERWORLD** (normal) - Standard overworld terrain generation  
3. **CHERRY GROVE** (cherry) - Cherry grove biome only

### Choosing Your Dimension Type

Players can select their dimension type through the Maid GUI:

1. Right-click your maid with favorability level 3
2. Navigate to the 4th tab (Personal Dimension)
3. Click the **"Dim:"** button to cycle through available dimensions
4. The dimension will change on your next teleport

## Adding Custom Dimensions

### Method 1: Use Existing Mod/Vanilla Dimensions

You can reference dimensions from other mods or vanilla Minecraft directly in your config.

**Example - Adding The Nether:**

```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "normal|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
        "cherry|CHERRY GROVE|touhoulittlemaidpersonaldimension:personal_dimension_cherry",
        "nether|NETHER REALM|minecraft:the_nether"
    ]
```

**Example - Adding Twilight Forest (if installed):**

```toml
customDimensionTemplates = [
    "twilight|TWILIGHT FOREST|twilightforest:twilight_forest"
]
```

### Method 2: Create Custom Dimension via Datapack

For complete control, create your own dimension using Minecraft's datapack system.

#### Step 1: Create Datapack Structure

Create this folder structure in your world:
```
world/datapacks/mydimensions/
├── pack.mcmeta
└── data/
    └── mydimensions/
        ├── dimension_type/
        │   └── my_custom_dimension_type.json
        └── dimension/
            └── my_custom_dimension.json
```

#### Step 2: Create pack.mcmeta

```json
{
  "pack": {
    "pack_format": 26,
    "description": "My Custom Dimensions"
  }
}
```

#### Step 3: Define Dimension Type

**File:** `data/mydimensions/dimension_type/my_custom_dimension_type.json`

```json
{
  "ultrawarm": false,
  "natural": true,
  "piglin_safe": true,
  "respawn_anchor_works": false,
  "bed_works": true,
  "has_raids": false,
  "has_skylight": true,
  "has_ceiling": false,
  "coordinate_scale": 1.0,
  "ambient_light": 0.0,
  "logical_height": 256,
  "effects": "minecraft:overworld",
  "infiniburn": "#minecraft:infiniburn_overworld",
  "min_y": -64,
  "height": 320,
  "monster_spawn_light_level": 0,
  "monster_spawn_block_light_limit": 0
}
```

**Key Properties Explained:**
- `ultrawarm`: Whether water evaporates (like Nether)
- `natural`: Can use compass/clock
- `piglin_safe`: Piglins don't zombify
- `bed_works`: Can set spawn with bed
- `has_skylight`: Has daylight cycle
- `has_ceiling`: Height limit at bedrock (like Nether)
- `ambient_light`: Base light level (0.0 = dark, 1.0 = bright)
- `effects`: Sky/fog color scheme

#### Step 4: Define Dimension Generator

**File:** `data/mydimensions/dimension/my_custom_dimension.json`

**Example A - Overworld-like:**
```json
{
  "type": "mydimensions:my_custom_dimension_type",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:overworld"
    },
    "settings": "minecraft:overworld"
  }
}
```

**Example B - Flat World:**
```json
{
  "type": "mydimensions:my_custom_dimension_type",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "layers": [
        { "block": "minecraft:bedrock", "height": 1 },
        { "block": "minecraft:stone", "height": 3 },
        { "block": "minecraft:dirt", "height": 2 },
        { "block": "minecraft:grass_block", "height": 1 }
      ],
      "biome": "minecraft:plains"
    }
  }
}
```

**Example C - Single Biome:**
```json
{
  "type": "mydimensions:my_custom_dimension_type",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": {
      "type": "minecraft:fixed",
      "biome": "minecraft:cherry_grove"
    },
    "settings": "minecraft:overworld"
  }
}
```

#### Step 5: Add to Config

Edit `config/touhoulittlemaidpersonaldimension-common.toml`:

```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "mycustom|MY CUSTOM WORLD|mydimensions:my_custom_dimension"
    ]
```

#### Step 6: Load and Test

1. Place datapack in `world/datapacks/`
2. Run `/reload` or restart server
3. Verify with `/execute in mydimensions:my_custom_dimension run tp @s ~ ~ ~`
4. Open Maid GUI to see your new dimension type

## Configuration Format

### Entry Format
```
"id|display_name|template_dimension_key"
```

- **id**: Unique lowercase identifier (no spaces)
- **display_name**: Name shown in GUI (any characters)
- **template_dimension_key**: Namespace:path to dimension JSON

### Multiple Entries

```toml
customDimensionTemplates = [
    "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
    "normal|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
    "nether|NETHER DIMENSION|minecraft:the_nether",
    "end|THE END|minecraft:the_end",
    "custom1|CUSTOM WORLD 1|mypack:custom1",
    "custom2|CUSTOM WORLD 2|mypack:custom2"
]
```

## Advanced Examples

### Amplified Terrain

```json
{
  "type": "mydimensions:amplified_type",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:overworld"
    },
    "settings": "minecraft:amplified"
  }
}
```

### Large Biomes

```json
{
  "type": "mydimensions:large_biomes_type",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:overworld"
    },
    "settings": {
      "sea_level": 63,
      "disable_mob_generation": false,
      "aquifers_enabled": true,
      "ore_veins_enabled": true,
      "legacy_random_source": false,
      "default_block": {
        "Name": "minecraft:stone"
      },
      "default_fluid": {
        "Name": "minecraft:water"
      },
      "noise": {
        "min_y": -64,
        "height": 384,
        "size_horizontal": 4,
        "size_vertical": 2
      }
    }
  }
}
```

### Skylands (Floating Islands)

```json
{
  "type": "mydimensions:skylands_type",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:overworld"
    },
    "settings": {
      "sea_level": 0,
      "disable_mob_generation": false,
      "aquifers_enabled": false,
      "ore_veins_enabled": true,
      "legacy_random_source": false,
      "default_block": {
        "Name": "minecraft:air"
      },
      "default_fluid": {
        "Name": "minecraft:air"
      },
      "noise": {
        "min_y": 0,
        "height": 256,
        "size_horizontal": 2,
        "size_vertical": 1
      }
    }
  }
}
```

## Private vs Shared Dimensions

### Private Mode (Default)

When `privateDimension = true` in config:
- Each player gets their own instance
- Player A's "Overworld" dimension is separate from Player B's
- Dimension type is stored per-player

### Shared Mode

When `privateDimension = false`:
- All players share one dimension
- Dimension type is global (first configured dimension)
- Players can see each other's builds

## Tips & Best Practices

### Performance

- Avoid too many complex dimensions on servers
- Test dimension generation before adding to production
- Consider using fixed biomes for better performance

### Compatibility

- Check mod compatibility before referencing mod dimensions
- Some mods may have special dimension requirements
- Always test in singleplayer first

### Naming

- Use clear, descriptive display names
- Keep IDs short and simple (lowercase, no spaces)
- Use consistent naming across similar dimensions

### Organization

```toml
# Vanilla dimensions
customDimensionTemplates = [
    "void|VOID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
    "overworld|OVERWORLD|minecraft:overworld",
    "nether|NETHER|minecraft:the_nether",
    "end|THE END|minecraft:the_end",
    
    # Mod dimensions
    "twilight|TWILIGHT FOREST|twilightforest:twilight_forest",
    "aether|THE AETHER|aether:the_aether",
    
    # Custom dimensions
    "flat_creative|FLAT CREATIVE|mypack:flat_creative",
    "skyblock|SKYBLOCK|mypack:skyblock"
]
```

## Troubleshooting

### Dimension doesn't appear in GUI
- Check config syntax (correct pipes `|`)
- Verify no typos in dimension keys
- Ensure referenced dimension exists
- Check logs for errors

### Can't teleport to dimension
- Dimension may not be loaded
- Check dimension definition is valid
- Verify datapack is in correct location
- Use `/reload` to refresh datapacks

### Dimension generates incorrectly
- Check generator settings in dimension JSON
- Verify biome source configuration
- Test with vanilla presets first
- Check for mod conflicts

### Old dimension still loads
- Players must change dimension type via GUI
- Existing instances won't automatically update
- May need to delete old dimension folder

## Migration from Old System

### Automatic Conversion

Old enum-based saves are automatically converted:
- `VOID` → `"void"`
- `NORMAL` → `"normal"`
- `CHERRY` → `"cherry"`

### Manual Updates

If you had custom code using the old system:
```java
// Old
Config.DimensionType.VOID

// New
CustomDimensionConfig.findById("void", dimensions)
```

## FAQ

**Q: Can I remove default dimensions?**
A: Yes, just don't include them in the config list.

**Q: How many dimensions can I add?**
A: No hard limit, but consider server performance.

**Q: Do I need to restart the server?**
A: `/reload` works for datapacks, but config changes need restart.

**Q: Can players switch dimensions after creating one?**
A: Yes, via the Maid GUI. Old dimension stays until deleted.

**Q: Will this work with existing saves?**
A: Yes, old saves are automatically converted.

**Q: Can I use dimensions from other mods?**
A: Yes, if the mod is installed and the dimension exists.

## Support & Resources

- [Minecraft Wiki - Custom Dimensions](https://minecraft.wiki/w/Custom_dimension)
- [Datapack Tutorial](https://minecraft.wiki/w/Tutorials/Creating_a_data_pack)
- Check mod logs for detailed error messages
- Test configurations in creative mode first
