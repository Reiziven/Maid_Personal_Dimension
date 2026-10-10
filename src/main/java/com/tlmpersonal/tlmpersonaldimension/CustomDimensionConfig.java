package com.tlmpersonal.tlmpersonaldimension;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Manages custom dimension configurations loaded from config.
 * Each dimension template defines an ID, display name, and dimension type reference.
 */
public class CustomDimensionConfig {
    private static final Logger LOGGER = Touhoulittlemaidpersonaldimension.LOGGER;
    
    private final String id;
    private final String displayName;
    private final ResourceLocation dimensionTypeLocation;
    private final ResourceLocation templateDimensionKey;

    private CustomDimensionConfig(String id, String displayName, ResourceLocation dimensionTypeLocation, ResourceLocation templateDimensionKey) {
        this.id = id;
        this.displayName = displayName;
        this.dimensionTypeLocation = dimensionTypeLocation;
        this.templateDimensionKey = templateDimensionKey;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceLocation getDimensionTypeLocation() {
        return dimensionTypeLocation;
    }

    public ResourceLocation getTemplateDimensionKey() {
        return templateDimensionKey;
    }

    /**
     * Parse config string format: "id|display_name|template_dimension_key"
     * Example: "void|MAID ISLAND|touhoulittlemaidpersonaldimension:personal_dimension"
     */
    public static CustomDimensionConfig parse(String configEntry) {
        try {
            String[] parts = configEntry.split("\\|");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Expected format: id|display_name|template_dimension_key");
            }

            String id = parts[0].trim();
            String displayName = parts[1].trim();
            String templateKey = parts[2].trim();

            if (id.isEmpty() || displayName.isEmpty() || templateKey.isEmpty()) {
                throw new IllegalArgumentException("All fields must be non-empty");
            }

            ResourceLocation templateDimensionKey = new ResourceLocation(templateKey);
            
            return new CustomDimensionConfig(id, displayName, templateDimensionKey, templateDimensionKey);
        } catch (Exception e) {
            LOGGER.error("Failed to parse dimension config entry: '{}' - {}", configEntry, e.getMessage());
            return null;
        }
    }

    /**
     * Load all custom dimensions from config
     */
    public static List<CustomDimensionConfig> loadFromConfig() {
        List<CustomDimensionConfig> dimensions = new ArrayList<>();
        List<? extends String> configEntries = Config.CUSTOM_DIMENSION_TEMPLATES.get();

        for (String entry : configEntries) {
            CustomDimensionConfig config = parse(entry);
            if (config != null) {
                dimensions.add(config);
                LOGGER.info("Loaded custom dimension: {} -> {}", config.getId(), config.getDisplayName());
            }
        }

        if (dimensions.isEmpty()) {
            LOGGER.warn("No valid custom dimensions loaded! Using fallback defaults.");
            dimensions.addAll(getDefaultDimensions());
        }

        return dimensions;
    }

    /**
     * Default dimension configurations (fallback)
     */
    private static List<CustomDimensionConfig> getDefaultDimensions() {
        return Arrays.asList(
            new CustomDimensionConfig(
                "void",
                "MAID ISLAND",
                new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_type_void"),
                Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_VOID_KEY.location()
            ),
            new CustomDimensionConfig(
                "normal",
                "OVERWORLD",
                new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_type_normal"),
                Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_NORMAL_KEY.location()
            ),
            new CustomDimensionConfig(
                "cherry",
                "CHERRY GROVE",
                new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_type_cherry"),
                Touhoulittlemaidpersonaldimension.PERSONAL_DIMENSION_CHERRY_KEY.location()
            ),
            new CustomDimensionConfig(
                "nether",
                "NETHER",
                new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_nether"),
                new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_nether")
            ),
            new CustomDimensionConfig(
                "end",
                "THE END",
                new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_end"),
                new ResourceLocation(Touhoulittlemaidpersonaldimension.MODID, "personal_dimension_end")
            )
        );
    }

    /**
     * Find dimension config by ID
     */
    public static CustomDimensionConfig findById(String id, List<CustomDimensionConfig> dimensions) {
        return dimensions.stream()
            .filter(d -> d.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get dimension by index (for cycling)
     */
    public static CustomDimensionConfig getByIndex(int index, List<CustomDimensionConfig> dimensions) {
        if (dimensions.isEmpty()) {
            return null;
        }
        int safeIndex = ((index % dimensions.size()) + dimensions.size()) % dimensions.size();
        return dimensions.get(safeIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomDimensionConfig that = (CustomDimensionConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CustomDimensionConfig{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", templateKey=" + templateDimensionKey +
                '}';
    }
}
