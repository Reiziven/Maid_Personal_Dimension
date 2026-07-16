# Quick Start: Custom Dimensions

## 5-Minute Setup Guide

### What You Get
Add unlimited dimension types to your personal dimension system - no coding required!

### Default Dimensions
- **MAID ISLAND** - Floating island in void
- **OVERWORLD** - Normal terrain generation
- **CHERRY GROVE** - Cherry biome only

---

## Method 1: Use Vanilla Dimensions (Easiest)

### Step 1: Find Config File
Navigate to: `config/touhoulittlemaidpersonaldimension-common.toml`

### Step 2: Edit Config
Find the `[customDimensionTemplates]` section and add dimensions:

```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "overworld|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
        "cherry|CHERRY GROVE|touhoulittlemaidpersonaldimension:personal_dimension_cherry",
        "nether|NETHER REALM|minecraft:the_nether",
        "end|THE END|minecraft:the_end"
    ]
```

### Step 3: Restart
Restart your Minecraft client or server.

### Step 4: Select Dimension
1. Right-click your maid (favorability level 3 required)
2. Go to 4th tab (Personal Dimension)
3. Click **"Dim: ..."** button to cycle through dimensions
4. Teleport to enter your new dimension!

---

## Method 2: Use Mod Dimensions

If you have other dimension mods installed:

```toml
customDimensionTemplates = [
    "twilight|TWILIGHT FOREST|twilightforest:twilight_forest",
    "aether|THE AETHER|aether:the_aether",
    "undergarden|UNDERGARDEN|undergarden:undergarden"
]
```

⚠️ **Note:** The mod must be installed for this to work!

---

## Method 3: Create Custom Dimension (Advanced)

### Quick Datapack Setup

**1. Create folder structure:**
```
world/datapacks/mydimensions/
├── pack.mcmeta
└── data/mydimensions/
    ├── dimension_type/
    │   └── custom.json
    └── dimension/
        └── custom.json
```

**2. Create `pack.mcmeta`:**
```json
{
  "pack": {
    "pack_format": 26,
    "description": "My Custom Dimensions"
  }
}
```

**3. Create `dimension_type/custom.json`:**
```json
{
  "ultrawarm": false,
  "natural": true,
  "piglin_safe": true,
  "bed_works": true,
  "has_skylight": true,
  "has_ceiling": false,
  "ambient_light": 0.0,
  "effects": "minecraft:overworld",
  "min_y": -64,
  "height": 320
}
```

**4. Create `dimension/custom.json`:**
```json
{
  "type": "mydimensions:custom",
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

**5. Add to config:**
```toml
customDimensionTemplates = [
    "custom|MY FLAT WORLD|mydimensions:custom"
]
```

**6. Load:**
- Run `/reload` or restart server
- Select dimension via Maid GUI

---

## Configuration Format

### Format
```
"id|display_name|template_dimension_key"
```

### Examples

**Basic:**
```toml
"void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension"
```

**Vanilla:**
```toml
"nether|NETHER|minecraft:the_nether"
```

**Mod:**
```toml
"twilight|TWILIGHT FOREST|twilightforest:twilight_forest"
```

**Custom:**
```toml
"myworld|MY CUSTOM WORLD|mypack:myworld"
```

---

## Common Use Cases

### 1. Peaceful Creative World
```toml
"creative|CREATIVE WORLD|touhoulittlemaidpersonaldimension:personal_dimension"
```
Set in GUI: Disable hostile entities, disable hunger, etc.

### 2. Hardcore Survival
```toml
"hardcore|HARDCORE|minecraft:overworld"
```
Enable all challenges in GUI.

### 3. Resource Dimension
```toml
"mining|MINING WORLD|minecraft:overworld"
```
Dedicated world for gathering resources.

### 4. Building World
```toml
"creative_flat|BUILDING WORLD|mypack:flat_world"
```
Flat world for creative building.

---

## Troubleshooting

### Dimension not showing up?
- Check config syntax (correct pipes `|`)
- Restart after config changes
- Check for typos in dimension keys

### Can't teleport?
- Ensure dimension exists (vanilla/mod installed or datapack loaded)
- Check logs for errors
- Try `/reload` if using datapack

### Dimension loads incorrectly?
- Verify dimension JSON is valid
- Test with vanilla dimensions first
- Check datapack is in correct folder

---

## Tips

✅ **Do:**
- Test in creative mode first
- Use clear, descriptive names
- Start with vanilla dimensions
- Read the full guide for advanced features

❌ **Don't:**
- Add too many complex dimensions (performance)
- Forget to restart after config changes
- Use spaces in dimension IDs
- Reference non-existent dimensions

---

## Need More Help?

📖 **Full Documentation:**
- `CUSTOM_DIMENSIONS_USER_GUIDE.md` - Complete manual
- `CUSTOM_DIMENSIONS_EXAMPLES.md` - More examples
- `CUSTOM_DIMENSIONS_IMPLEMENTATION.md` - Technical details

🔍 **Resources:**
- [Minecraft Wiki - Custom Dimensions](https://minecraft.wiki/w/Custom_dimension)
- [Datapack Guide](https://minecraft.wiki/w/Tutorials/Creating_a_data_pack)
- Check mod logs for errors

---

## Examples to Get Started

### Minimalist Setup (2 dimensions)
```toml
customDimensionTemplates = [
    "island|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
    "world|NORMAL WORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal"
]
```

### All Vanilla Dimensions
```toml
customDimensionTemplates = [
    "overworld|OVERWORLD|minecraft:overworld",
    "nether|NETHER|minecraft:the_nether",
    "end|THE END|minecraft:the_end"
]
```

### Variety Pack
```toml
customDimensionTemplates = [
    "void|VOID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
    "overworld|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
    "cherry|CHERRY GROVE|touhoulittlemaidpersonaldimension:personal_dimension_cherry",
    "nether|NETHER|minecraft:the_nether",
    "twilight|TWILIGHT FOREST|twilightforest:twilight_forest"
]
```

---

**Ready to start? Edit your config file and add some dimensions!**
