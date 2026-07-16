# Custom Dimension System Implementation

## Overview
Replaced the hardcoded dimension type enum with a flexible config-based system that allows players to add unlimited custom dimensions through configuration.

## Changes Made

### 1. New Classes Created

#### `CustomDimensionConfig.java`
- Manages custom dimension configurations
- Parses config strings in format: `"id|display_name|template_dimension_key"`
- Provides methods to find, load, and cycle through dimensions
- Includes default dimensions (void, normal, cherry) as fallback

### 2. Config Changes (`Config.java`)

Added new configuration section:
```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "normal|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
        "cherry|CHERRY GROVE|touhoulittlemaidpersonaldimension:personal_dimension_cherry"
    ]
```

The old `DIMENSION_TYPE` enum config is kept for backward compatibility but marked as deprecated.

### 3. Data Storage (`PersonalDimensionSavedData.java`)

- Changed `dimensionType` (enum) → `dimensionTypeId` (String)
- Added legacy conversion for existing saves
- Stored as string ID for flexibility

### 4. Dimension Manager (`PlayerDimensionManager.java`)

- Updated to use string dimension IDs instead of enum
- Loads dimension configurations dynamically
- Falls back to defaults if ID not found
- Template dimension keys resolved from config

### 5. GUI (`PersonalDimensionGui.java`)

- Updated dimension selector button to cycle through configured dimensions
- Shows display names from config
- Sends dimension ID string to server

### 6. Network (`PersonalDimensionGuiPacket.java`)

- Updated `SET_DIMENSION_TYPE` handler to accept string IDs
- Stores dimension ID directly in player settings

### 7. World Generation (`StructurePlacer.java`)

- Updated to check dimension type by ID instead of enum
- Determines spawn height and natural island spawning based on dimension type ID

### 8. Main Class (`Touhoulittlemaidpersonaldimension.java`)

- Updated `getCurrentPersonalDimensionKey()` to use first configured dimension

## How to Add Custom Dimensions

### Option 1: Use Existing Vanilla/Mod Dimensions
```toml
customDimensionTemplates = [
    "nether|NETHER REALM|minecraft:the_nether",
    "end|THE END|minecraft:the_end",
    "twilight|TWILIGHT|twilightforest:twilight_forest"
]
```

### Option 2: Create Custom Dimension via Datapack

1. Create dimension JSON in datapack:
   `data/yourpack/dimension/yourdimension.json`

2. Define dimension type:
   `data/yourpack/dimension_type/yourdimensiontype.json`

3. Add to config:
```toml
customDimensionTemplates = [
    "custom_id|DISPLAY NAME|yourpack:yourdimension"
]
```

## Backward Compatibility

- Old saves with enum values are automatically converted to string IDs
- Existing dimensions (void, normal, cherry) continue to work
- Template dimension keys map to existing dimension definitions

## Benefits

1. **Mod Compatibility**: Can reference any dimension from any mod
2. **Datapack Support**: Players can add dimensions via datapacks
3. **Unlimited Dimensions**: No code changes needed to add new types
4. **Flexible Configuration**: Easy to customize via config file
5. **User-Friendly**: Display names can be customized per dimension

## Files Modified

- `Config.java` - Added custom dimension templates config
- `CustomDimensionConfig.java` - NEW: Dimension config management
- `PersonalDimensionSavedData.java` - Store dimension ID as string
- `PlayerDimensionManager.java` - Load and resolve dimensions dynamically
- `PersonalDimensionGui.java` - Dynamic dimension selection UI
- `PersonalDimensionGuiPacket.java` - Handle string dimension IDs
- `StructurePlacer.java` - Check dimension types by ID
- `Touhoulittlemaidpersonaldimension.java` - Use first configured dimension as default

## Testing Checklist

- [ ] Existing saves load correctly with automatic enum→string conversion
- [ ] Dimension selector cycles through all configured dimensions
- [ ] New dimensions can be created and selected
- [ ] Dimension-specific features work (spawn height, natural islands)
- [ ] Invalid dimension IDs fallback gracefully
- [ ] Config reloading updates available dimensions
