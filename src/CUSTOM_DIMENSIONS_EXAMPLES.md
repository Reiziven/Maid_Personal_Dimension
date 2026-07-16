# Custom Dimensions Configuration Examples

## Configuration Format

Each dimension template entry follows this format:
```
"id|display_name|template_dimension_key"
```

- **id**: Unique identifier (lowercase, no spaces)
- **display_name**: Name shown in GUI (can have spaces/special chars)
- **template_dimension_key**: ResourceLocation pointing to dimension definition

## Default Configuration

```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "normal|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
        "cherry|CHERRY GROVE|touhoulittlemaidpersonaldimension:personal_dimension_cherry"
    ]
```

## Example 1: Adding Vanilla Dimensions

```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "normal|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
        "cherry|CHERRY GROVE|touhoulittlemaidpersonaldimension:personal_dimension_cherry",
        "nether|NETHER REALM|minecraft:the_nether",
        "end|THE END|minecraft:the_end"
    ]
```

## Example 2: Adding Mod Dimensions

If you have other mods installed with dimensions:

```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "twilight|TWILIGHT FOREST|twilightforest:twilight_forest",
        "aether|THE AETHER|aether:the_aether",
        "undergarden|UNDERGARDEN|undergarden:undergarden"
    ]
```

## Example 3: Create Custom Dimension with Datapack

### Step 1: Create Dimension Type JSON
Create file: `data/mypack/dimension_type/custom_sky.json`

```json
{
  "ultrawarm": false,
  "natural": true,
  "piglin_safe": false,
  "respawn_anchor_works": false,
  "bed_works": true,
  "has_raids": true,
  "has_skylight": true,
  "has_ceiling": false,
  "coordinate_scale": 1.0,
  "ambient_light": 0.0,
  "logical_height": 384,
  "effects": "minecraft:overworld",
  "infiniburn": "#minecraft:infiniburn_overworld",
  "min_y": -64,
  "height": 384,
  "monster_spawn_light_level": 0,
  "monster_spawn_block_light_limit": 0
}
```

### Step 2: Create Dimension JSON
Create file: `data/mypack/dimension/custom_sky.json`

```json
{
  "type": "mypack:custom_sky",
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

### Step 3: Add to Config
```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "custom_sky|CUSTOM SKY WORLD|mypack:custom_sky"
    ]
```

## Example 4: Flat World Dimension

### Create Dimension Type
File: `data/mypack/dimension_type/flat_world.json`
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
  "ambient_light": 0.5,
  "logical_height": 256,
  "effects": "minecraft:overworld",
  "infiniburn": "#minecraft:infiniburn_overworld",
  "min_y": 0,
  "height": 256,
  "monster_spawn_light_level": 0,
  "monster_spawn_block_light_limit": 0
}
```

### Create Dimension with Flat Generator
File: `data/mypack/dimension/flat_world.json`
```json
{
  "type": "mypack:flat_world",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "layers": [
        {
          "block": "minecraft:bedrock",
          "height": 1
        },
        {
          "block": "minecraft:dirt",
          "height": 2
        },
        {
          "block": "minecraft:grass_block",
          "height": 1
        }
      ],
      "biome": "minecraft:plains",
      "lakes": false,
      "features": false
    }
  }
}
```

### Config Entry
```toml
customDimensionTemplates = [
    "flat|FLAT WORLD|mypack:flat_world"
]
```

## Example 5: Void World (Custom)

### Dimension Type
File: `data/mypack/dimension_type/void_world.json`
```json
{
  "ultrawarm": false,
  "natural": false,
  "piglin_safe": true,
  "respawn_anchor_works": false,
  "bed_works": true,
  "has_raids": false,
  "has_skylight": false,
  "has_ceiling": true,
  "coordinate_scale": 1.0,
  "ambient_light": 0.1,
  "logical_height": 256,
  "effects": "minecraft:the_end",
  "infiniburn": "#minecraft:infiniburn_overworld",
  "min_y": 0,
  "height": 256,
  "monster_spawn_light_level": 0,
  "monster_spawn_block_light_limit": 0
}
```

### Dimension with No Generation
File: `data/mypack/dimension/void_world.json`
```json
{
  "type": "mypack:void_world",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": {
      "type": "minecraft:fixed",
      "biome": "minecraft:the_void"
    },
    "settings": {
      "sea_level": 0,
      "disable_mob_generation": true,
      "aquifers_enabled": false,
      "ore_veins_enabled": false,
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
        "size_horizontal": 1,
        "size_vertical": 2
      },
      "noise_router": {
        "barrier": 0,
        "fluid_level_floodedness": 0,
        "fluid_level_spread": 0,
        "lava": 0,
        "temperature": 0,
        "vegetation": 0,
        "continents": 0,
        "erosion": 0,
        "depth": 0,
        "ridges": 0,
        "initial_density_without_jaggedness": 0,
        "final_density": 0,
        "vein_toggle": 0,
        "vein_ridged": 0,
        "vein_gap": 0
      },
      "spawn_target": [],
      "surface_rule": {
        "type": "minecraft:sequence",
        "sequence": []
      }
    }
  }
}
```

### Config Entry
```toml
customDimensionTemplates = [
    "void_custom|CUSTOM VOID|mypack:void_world"
]
```

## Important Notes

1. **Dimension Key**: The template dimension key must reference an existing dimension definition (either from vanilla, mods, or datapacks)

2. **Mod Dependencies**: If referencing mod dimensions, ensure the mod is installed

3. **Datapack Location**: Place datapack files in:
   - Singleplayer: `<world>/datapacks/mypack/`
   - Server: `<server>/world/datapacks/mypack/`

4. **Reload**: After adding datapack, use `/reload` command or restart

5. **Testing**: Use `/execute in <dimension> run tp @s ~ ~ ~` to test if dimension loads

6. **Each Player**: With `privateDimension=true`, each player gets their own instance of the selected dimension type

## Troubleshooting

**Dimension not appearing in GUI:**
- Check config syntax (proper format with pipes `|`)
- Ensure no typos in dimension keys
- Verify mod/datapack is loaded

**Dimension fails to load:**
- Check server/client logs for errors
- Verify dimension type JSON is valid
- Ensure dimension generator settings are correct

**Existing dimension changes:**
- Players must select new dimension type via GUI
- Existing dimension instances won't automatically change
- Create new dimension to see changes
