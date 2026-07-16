# Custom Dimensions System - Documentation Index

## 🚀 Quick Links

- **[Quick Start Guide](QUICK_START.md)** - Get started in 5 minutes
- **[User Guide](CUSTOM_DIMENSIONS_USER_GUIDE.md)** - Complete manual
- **[Examples](CUSTOM_DIMENSIONS_EXAMPLES.md)** - Configuration examples
- **[Implementation](CUSTOM_DIMENSIONS_IMPLEMENTATION.md)** - Technical details
- **[Summary](IMPLEMENTATION_SUMMARY.md)** - Overview and status

---

## 📚 What is This?

The Custom Dimensions system allows you to configure **unlimited dimension types** for personal dimensions through a simple configuration file - no coding required!

### Key Features

✅ **Config-Based** - Add dimensions via config file  
✅ **Mod Compatible** - Use dimensions from any mod  
✅ **Datapack Support** - Create custom dimensions  
✅ **Backward Compatible** - Works with existing saves  
✅ **User-Friendly** - Easy GUI selection  

---

## 🎯 Who Should Read What?

### I'm a Player/User
**Start here:** [Quick Start Guide](QUICK_START.md) → [User Guide](CUSTOM_DIMENSIONS_USER_GUIDE.md)

Learn how to:
- Add new dimension types to your config
- Switch between dimensions
- Create custom dimensions with datapacks

### I'm a Server Admin  
**Start here:** [Quick Start Guide](QUICK_START.md) → [Examples](CUSTOM_DIMENSIONS_EXAMPLES.md)

Learn how to:
- Configure server dimension options
- Optimize for performance
- Integrate with other mods

### I'm a Datapack Creator
**Start here:** [Examples](CUSTOM_DIMENSIONS_EXAMPLES.md) → [User Guide](CUSTOM_DIMENSIONS_USER_GUIDE.md)

Learn how to:
- Create custom dimension datapacks
- Configure dimension generation
- Set dimension properties

### I'm a Developer/Modder
**Start here:** [Implementation](CUSTOM_DIMENSIONS_IMPLEMENTATION.md) → [Summary](IMPLEMENTATION_SUMMARY.md)

Learn how to:
- Understand the technical architecture
- Integrate with the system
- Extend functionality

---

## 📖 Documentation Overview

### [QUICK_START.md](QUICK_START.md)
**5-minute setup guide**
- Basic configuration
- Common use cases
- Quick troubleshooting

### [CUSTOM_DIMENSIONS_USER_GUIDE.md](CUSTOM_DIMENSIONS_USER_GUIDE.md)
**Complete user manual** (comprehensive)
- What are custom dimensions?
- How to add dimensions (3 methods)
- Advanced configuration
- Datapack creation tutorial
- FAQ and troubleshooting

### [CUSTOM_DIMENSIONS_EXAMPLES.md](CUSTOM_DIMENSIONS_EXAMPLES.md)
**Configuration examples** (copy-paste ready)
- Vanilla dimensions
- Mod dimensions
- Custom datapacks
- Special configurations (void, flat, skylands)

### [CUSTOM_DIMENSIONS_IMPLEMENTATION.md](CUSTOM_DIMENSIONS_IMPLEMENTATION.md)
**Technical implementation** (for developers)
- Architecture overview
- Code changes
- Modified files
- Testing checklist

### [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
**Project summary** (status and overview)
- Implementation status
- Technical architecture
- Benefits and features
- Migration guide

---

## 🎮 Basic Usage

### 1. Configure Dimensions
Edit `config/touhoulittlemaidpersonaldimension-common.toml`:

```toml
[customDimensionTemplates]
    customDimensionTemplates = [
        "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension",
        "overworld|OVERWORLD|touhoulittlemaidpersonaldimension:personal_dimension_normal",
        "nether|NETHER|minecraft:the_nether"
    ]
```

### 2. Restart Game/Server

### 3. Select Dimension
1. Right-click maid (favorability 3+)
2. Navigate to 4th tab
3. Click "Dim:" button to cycle
4. Teleport to enter

---

## 🔧 Configuration Format

```
"id|display_name|template_dimension_key"
```

**Parts:**
- `id` - Unique identifier (lowercase, no spaces)
- `display_name` - Name shown in GUI (any characters)
- `template_dimension_key` - Dimension reference (namespace:path)

**Examples:**
```toml
"void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension"
"nether|NETHER REALM|minecraft:the_nether"
"twilight|TWILIGHT FOREST|twilightforest:twilight_forest"
"custom|MY WORLD|mypack:custom_dimension"
```

---

## 🌟 Common Scenarios

### Scenario 1: Add Vanilla Dimensions
```toml
customDimensionTemplates = [
    "overworld|OVERWORLD|minecraft:overworld",
    "nether|NETHER|minecraft:the_nether",
    "end|THE END|minecraft:the_end"
]
```

### Scenario 2: Add Mod Dimensions
```toml
customDimensionTemplates = [
    "twilight|TWILIGHT FOREST|twilightforest:twilight_forest",
    "aether|THE AETHER|aether:the_aether"
]
```

### Scenario 3: Create Custom Dimension
1. Create datapack (see [User Guide](CUSTOM_DIMENSIONS_USER_GUIDE.md))
2. Add to config: `"custom|MY DIMENSION|mypack:custom"`
3. Use `/reload` to load

---

## ❓ FAQ

### Can I add unlimited dimensions?
Yes, there's no hardcoded limit.

### Do I need to code?
No, just edit the config file.

### Will this work with existing saves?
Yes, automatically converts old formats.

### Can I use dimensions from other mods?
Yes, if the mod is installed.

### Do I need to restart?
Yes, after config changes. Datapacks can use `/reload`.

### What if a dimension doesn't exist?
System falls back to default dimension with error log.

---

## 🛠️ Troubleshooting

### Dimension not appearing?
- ✓ Check config syntax (pipes `|`)
- ✓ Verify no typos
- ✓ Restart after changes

### Can't teleport?
- ✓ Ensure dimension exists
- ✓ Check logs for errors
- ✓ Verify mod/datapack loaded

### Wrong dimension loads?
- ✓ Check dimension JSON
- ✓ Verify generator settings
- ✓ Test with vanilla first

**More help:** See [User Guide](CUSTOM_DIMENSIONS_USER_GUIDE.md) troubleshooting section

---

## 📦 What's Included

### Code Files
- `CustomDimensionConfig.java` - Configuration manager
- Modified: Config, SavedData, Manager, GUI, Packets, etc.

### Documentation Files
- `README_CUSTOM_DIMENSIONS.md` - This file
- `QUICK_START.md` - Quick setup
- `CUSTOM_DIMENSIONS_USER_GUIDE.md` - Full manual  
- `CUSTOM_DIMENSIONS_EXAMPLES.md` - Examples
- `CUSTOM_DIMENSIONS_IMPLEMENTATION.md` - Technical docs
- `IMPLEMENTATION_SUMMARY.md` - Project summary

### Language Files
- `en_us.json` - English translations
- `zh_cn.json` - Chinese translations

---

## 🎯 Getting Started Path

### Beginners
1. Read [QUICK_START.md](QUICK_START.md)
2. Try adding vanilla dimensions
3. Experiment with GUI selection
4. Read [User Guide](CUSTOM_DIMENSIONS_USER_GUIDE.md) for more

### Intermediate
1. Review [EXAMPLES.md](CUSTOM_DIMENSIONS_EXAMPLES.md)
2. Try adding mod dimensions
3. Explore datapack creation
4. Customize dimension properties

### Advanced
1. Study [IMPLEMENTATION.md](CUSTOM_DIMENSIONS_IMPLEMENTATION.md)
2. Create complex datapacks
3. Integrate with other systems
4. Optimize for performance

---

## 💡 Tips & Best Practices

### Configuration
- Use clear, descriptive names
- Keep IDs simple (lowercase, no spaces)
- Test in creative mode first
- Document your custom dimensions

### Performance
- Limit number of active dimensions
- Use simpler generators when possible
- Consider fixed biomes over multi-noise
- Test on target hardware

### Compatibility
- Verify mod dependencies
- Check dimension availability
- Test with minimal mod set first
- Watch logs for errors

---

## 🔗 External Resources

- [Minecraft Wiki - Custom Dimensions](https://minecraft.wiki/w/Custom_dimension)
- [Datapack Tutorial](https://minecraft.wiki/w/Tutorials/Creating_a_data_pack)
- [Dimension Types Reference](https://minecraft.wiki/w/Dimension_type)
- [World Generation](https://minecraft.wiki/w/World_generation)

---

## 📝 Version Information

**Implementation Date:** 2026-07-15  
**Status:** ✅ Complete and Tested  
**Compatibility:** NeoForge 1.21+  
**Breaking Changes:** None (backward compatible)

---

## 🤝 Support

### Getting Help
1. Check documentation files
2. Review examples
3. Read FAQ sections
4. Check server/client logs
5. Test in creative mode

### Reporting Issues
- Include config file content
- Attach relevant logs
- Describe steps to reproduce
- Specify Minecraft/mod versions

---

## 📄 License

Part of Touhou Little Maid Personal Dimension mod.  
Follow mod's license terms.

---

**Happy dimension customizing! 🎉**

*Navigate to specific guides using links at the top of this file.*
