package com.tlmpersonal.tlmpersonaldimension.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * An EditBox with autocomplete functionality for entity IDs.
 * Similar to Minecraft's command suggestions.
 */
public class EntityIdEditBox extends EditBox {
    private final List<String> suggestions = new ArrayList<>();
    private int selectedSuggestionIndex = -1;
    private boolean showSuggestions = false;
    private static final int MAX_VISIBLE_SUGGESTIONS = 5;
    private int suggestionScrollOffset = 0;

    public EntityIdEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        
        // Set up responder to update suggestions
        this.setResponder(this::onTextChanged);
    }

    private void onTextChanged(String text) {
        updateSuggestions(text);
    }

    private void updateSuggestions(String input) {
        suggestions.clear();
        selectedSuggestionIndex = -1;
        suggestionScrollOffset = 0;
        
        if (input.isEmpty()) {
            showSuggestions = false;
            return;
        }

        // Use the suggestion helper
        suggestions.addAll(EntitySuggestionHelper.getSuggestions(input));
        
        showSuggestions = !suggestions.isEmpty();
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        
        // Only show suggestions if focused and visible on screen
        if (showSuggestions && isFocused() && this.visible) {
            renderSuggestions(graphics, mouseX, mouseY);
        }
    }

    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        if (suggestions.isEmpty()) return;
        
        int x = getX();
        int y = getY() + this.height;
        int width = Math.max(this.width, 200);
        
        // Calculate visible suggestions
        int visibleCount = Math.min(MAX_VISIBLE_SUGGESTIONS, suggestions.size());
        int boxHeight = visibleCount * 12 + 4;
        
        // Check if suggestions would go off screen, render above if needed
        if (y + boxHeight > Minecraft.getInstance().getWindow().getGuiScaledHeight()) {
            y = getY() - boxHeight;
        }
        
        // Draw background with higher z-level to render above other elements
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400); // Render above everything
        
        graphics.fill(x, y, x + width, y + boxHeight, 0xF0000000);
        graphics.fill(x, y, x + width, y + 1, 0xFF555555); // Top border
        graphics.fill(x, y + boxHeight - 1, x + width, y + boxHeight, 0xFF555555); // Bottom border
        
        // Draw suggestions
        int endIndex = Math.min(suggestionScrollOffset + MAX_VISIBLE_SUGGESTIONS, suggestions.size());
        for (int i = suggestionScrollOffset; i < endIndex; i++) {
            String suggestion = suggestions.get(i);
            int itemY = y + 2 + (i - suggestionScrollOffset) * 12;
            
            // Highlight selected suggestion
            if (i == selectedSuggestionIndex) {
                graphics.fill(x + 1, itemY, x + width - 1, itemY + 12, 0x80FFFFFF);
            }
            
            // Draw text
            graphics.drawString(Minecraft.getInstance().font, suggestion, x + 3, itemY + 2, 0xFFFFFF);
        }
        
        // Draw scrollbar if needed
        if (suggestions.size() > MAX_VISIBLE_SUGGESTIONS) {
            int scrollbarX = x + width - 4;
            int scrollbarHeight = Math.max(10, (MAX_VISIBLE_SUGGESTIONS * boxHeight) / suggestions.size());
            int scrollbarY = y + 2 + ((boxHeight - 4 - scrollbarHeight) * suggestionScrollOffset) / 
                             Math.max(1, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
            
            graphics.fill(scrollbarX, y + 2, scrollbarX + 2, y + boxHeight - 2, 0x40FFFFFF);
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + scrollbarHeight, 0xFFFFFFFF);
        }
        
        graphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!showSuggestions || suggestions.isEmpty()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        // Handle arrow key navigation
        switch (keyCode) {
            case 264: // Down arrow
                if (selectedSuggestionIndex < suggestions.size() - 1) {
                    selectedSuggestionIndex++;
                    // Auto-scroll
                    if (selectedSuggestionIndex >= suggestionScrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                        suggestionScrollOffset++;
                    }
                } else {
                    selectedSuggestionIndex = 0;
                    suggestionScrollOffset = 0;
                }
                return true;
                
            case 265: // Up arrow
                if (selectedSuggestionIndex > 0) {
                    selectedSuggestionIndex--;
                    // Auto-scroll
                    if (selectedSuggestionIndex < suggestionScrollOffset) {
                        suggestionScrollOffset--;
                    }
                } else {
                    selectedSuggestionIndex = suggestions.size() - 1;
                    suggestionScrollOffset = Math.max(0, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
                }
                return true;
                
            case 257: // Enter
            case 258: // Tab
                if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < suggestions.size()) {
                    applySuggestion(suggestions.get(selectedSuggestionIndex));
                } else if (!suggestions.isEmpty()) {
                    applySuggestion(suggestions.get(0));
                }
                return true;
                
            case 256: // Escape
                showSuggestions = false;
                return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showSuggestions && !suggestions.isEmpty() && isMouseOverSuggestions(mouseX, mouseY)) {
            if (suggestions.size() > MAX_VISIBLE_SUGGESTIONS) {
                suggestionScrollOffset = (int) Math.clamp(
                    suggestionScrollOffset - scrollY,
                    0,
                    suggestions.size() - MAX_VISIBLE_SUGGESTIONS
                );
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showSuggestions && isMouseOverSuggestions(mouseX, mouseY)) {
            int x = getX();
            int y = getY() + this.height;
            int relativeY = (int) (mouseY - y - 2);
            int clickedIndex = suggestionScrollOffset + (relativeY / 12);
            
            if (clickedIndex >= 0 && clickedIndex < suggestions.size()) {
                applySuggestion(suggestions.get(clickedIndex));
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOverSuggestions(double mouseX, double mouseY) {
        if (!showSuggestions) return false;
        
        int x = getX();
        int y = getY() + this.height;
        int width = Math.max(this.width, 200);
        int boxHeight = Math.min(MAX_VISIBLE_SUGGESTIONS, suggestions.size()) * 12 + 4;
        
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + boxHeight;
    }

    private void applySuggestion(String suggestion) {
        setValue(suggestion);
        showSuggestions = false;
        selectedSuggestionIndex = -1;
        setFocused(true);
    }
    
    /**
     * Hide suggestions when focus is lost
     */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            showSuggestions = false;
        }
    }
}
