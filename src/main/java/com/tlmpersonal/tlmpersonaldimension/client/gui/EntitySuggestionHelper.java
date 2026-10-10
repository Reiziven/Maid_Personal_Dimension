package com.tlmpersonal.tlmpersonaldimension.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class to provide entity ID suggestions for autocomplete functionality
 */
public class EntitySuggestionHelper {
    private static List<String> allEntityIds;
    
    /**
     * Get all registered entity IDs
     */
    public static List<String> getAllEntityIds() {
        if (allEntityIds == null) {
            allEntityIds = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .collect(Collectors.toList());
        }
        return allEntityIds;
    }
    
    /**
     * Get entity ID suggestions based on input
     */
    public static List<String> getSuggestions(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        
        String lowerInput = input.toLowerCase();
        List<String> suggestions = new ArrayList<>();
        
        // Check if it's a wildcard pattern (e.g., "minecraft:*")
        if (lowerInput.endsWith(":*") || lowerInput.endsWith(":")) {
            String namespace = lowerInput.replace(":*", "").replace(":", "");
            // Suggest the wildcard pattern first
            if (lowerInput.endsWith(":*")) {
                suggestions.add(namespace + ":*");
            }
            // Then suggest specific entities from that namespace
            for (String entityId : getAllEntityIds()) {
                if (entityId.startsWith(namespace + ":")) {
                    suggestions.add(entityId);
                    if (suggestions.size() >= 100) {
                        break;
                    }
                }
            }
        } else {
            // Normal search
            for (String entityId : getAllEntityIds()) {
                if (entityId.toLowerCase().contains(lowerInput)) {
                    suggestions.add(entityId);
                    if (suggestions.size() >= 100) {
                        break;
                    }
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Check if an entity ID is valid (includes wildcard patterns)
     */
    public static boolean isValidEntityId(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return false;
        }
        
        // Check if it's a wildcard pattern
        if (entityId.endsWith(":*")) {
            String namespace = entityId.substring(0, entityId.length() - 2);
            // Check if any entity exists with this namespace
            return getAllEntityIds().stream().anyMatch(id -> id.startsWith(namespace + ":"));
        }
        
        return getAllEntityIds().contains(entityId);
    }
    
    /**
     * Check if a pattern matches an entity ID
     * Supports wildcard patterns like "minecraft:*"
     */
    public static boolean matchesPattern(String pattern, String entityId) {
        if (pattern == null || entityId == null) {
            return false;
        }
        
        // Exact match
        if (pattern.equals(entityId)) {
            return true;
        }
        
        // Wildcard pattern (e.g., "minecraft:*")
        if (pattern.endsWith(":*")) {
            String namespace = pattern.substring(0, pattern.length() - 2);
            return entityId.startsWith(namespace + ":");
        }
        
        return false;
    }
    
    /**
     * Get all entity IDs that match a pattern
     */
    public static List<String> getMatchingEntities(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return new ArrayList<>();
        }
        
        // If it's a wildcard pattern, expand it
        if (pattern.endsWith(":*")) {
            String namespace = pattern.substring(0, pattern.length() - 2);
            return getAllEntityIds().stream()
                    .filter(id -> id.startsWith(namespace + ":"))
                    .collect(Collectors.toList());
        }
        
        // Otherwise, just return the single entity if it exists
        if (isValidEntityId(pattern)) {
            return List.of(pattern);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Get suggestions for namespace (mod id) based on input
     */
    public static List<String> getNamespaceSuggestions(String input) {
        return getAllEntityIds().stream()
                .map(id -> id.split(":")[0])
                .distinct()
                .filter(namespace -> namespace.toLowerCase().startsWith(input.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}
