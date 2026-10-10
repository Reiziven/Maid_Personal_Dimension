package com.tlmpersonal.tlmpersonaldimension.client.gui;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.CustomDimensionConfig;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.TouhoulittlemaidpersonaldimensionClient;
import com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PersonalDimensionGui extends Screen {
    private final EntityMaid maid;
    private static final Component TITLE = Component.translatable("gui.tlmpersonaldimension.title");
    private static final int GUI_WIDTH = 320;
    
    private EntityIdEditBox allowedEntityInput;
    private EntityIdEditBox blockedEntityInput;
    private EditBox allowedPlayerInput;
    private EditBox dayTimeInputWidget;
    private EntityListWidget allowedEntityListWidget;
    private EntityListWidget blockedEntityListWidget;
    private EntityListWidget allowedPlayerListWidget;
    
    private final List<String> allowedEntities = new ArrayList<>();
    private final List<String> blockedEntities = new ArrayList<>();
    private final List<String> allowedPlayers = new ArrayList<>();
    
    private int centerX;
    private PersonalDimensionSavedData.PlayerDimensionSettings localSettings;
    
    // Scrollable content support
    private double scrollAmount;
    private int contentHeight;
    private boolean isScrolling;
    private final List<WidgetPos> scrollingWidgets = new ArrayList<>();
    // Separate tracking for EntityListWidgets (not AbstractWidget in 1.20.1)
    private final List<ListWidgetPos> scrollingLists = new ArrayList<>();

    private static class WidgetPos {
        final AbstractWidget widget;
        final int baseY;
        WidgetPos(AbstractWidget widget, int baseY) {
            this.widget = widget;
            this.baseY = baseY;
        }
    }

    private static class ListWidgetPos {
        final EntityListWidget widget;
        final int baseY;
        final int listHeight;
        ListWidgetPos(EntityListWidget widget, int baseY, int listHeight) {
            this.widget = widget;
            this.baseY = baseY;
            this.listHeight = listHeight;
        }
    }

    public PersonalDimensionGui(EntityMaid maid) {
        super(TITLE);
        this.maid = maid;
        refreshLocalSettings();
    }

    private void refreshLocalSettings() {
        net.minecraft.nbt.CompoundTag tag = TouhoulittlemaidpersonaldimensionClient.getLastSettings();
        if (!tag.isEmpty()) {
            this.localSettings = PersonalDimensionSavedData.PlayerDimensionSettings.load(tag);
        } else {
            this.localSettings = new PersonalDimensionSavedData.PlayerDimensionSettings();
        }
    }

    private <T extends AbstractWidget> T addScrollingWidget(T widget) {
        this.scrollingWidgets.add(new WidgetPos(widget, widget.getY()));
        return addRenderableWidget(widget);
    }

    private void addScrollingList(EntityListWidget list, int baseY, int listHeight) {
        this.scrollingLists.add(new ListWidgetPos(list, baseY, listHeight));
        this.addWidget(list);
    }

    @Override
    protected void init() {
        super.init();
        centerX = (this.width - GUI_WIDTH) / 2;
        scrollingWidgets.clear();
        scrollingLists.clear();
        
        // Buttons that stay at top (Static)
        addRenderableWidget(Button.builder(Component.literal("< Back"), 
            button -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new PersonalDimensionMainGui(maid));
                }
            })
        .bounds(centerX + 10, 10, 50, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Refresh"), 
            button -> {
                sendPacket(PersonalDimensionGuiPacket.Action.REQUEST_SYNC, "");
            })
        .bounds(centerX + GUI_WIDTH - 60, 10, 50, 20).build());

        int y = 40;
        
        allowedEntityInput = new EntityIdEditBox(this.font, centerX + 10, y + 12, 110, 18, Component.empty());
        allowedEntityInput.setSuggestion("entity ID Whitelist");
        addScrollingWidget(allowedEntityInput);

        addScrollingWidget(Button.builder(Component.literal("Add"), button -> addAllowedEntity())
                .bounds(centerX + 120, y + 12, 35, 18)
                .build());

        blockedEntityInput = new EntityIdEditBox(this.font, centerX + 165, y + 12, 110, 18, Component.empty());
        blockedEntityInput.setSuggestion("Entity ID blacklist");
        addScrollingWidget(blockedEntityInput);

        addScrollingWidget(Button.builder(Component.literal("Add"), button -> addBlockedEntity())
                .bounds(centerX + 275, y + 12, 35, 18)
                .build());
        
        y += 35;

        allowedEntityListWidget = new EntityListWidget(this.minecraft, 145, 80, y, y + 80, 16, centerX + 10);
        addScrollingList(allowedEntityListWidget, y, 80);

        blockedEntityListWidget = new EntityListWidget(this.minecraft, 145, 80, y, y + 80, 16, centerX + 165);
        addScrollingList(blockedEntityListWidget, y, 80);

        y += 85;

        allowedPlayerInput = new EditBox(this.font, centerX + 10, y + 12, 110, 18, Component.empty());
        allowedPlayerInput.setSuggestion("player name/uuid");
        addScrollingWidget(allowedPlayerInput);

        addScrollingWidget(Button.builder(Component.literal("Add"), button -> addAllowedPlayer())
                .bounds(centerX + 120, y + 12, 35, 18)
                .build());

        y += 35;
        allowedPlayerListWidget = new EntityListWidget(this.minecraft, 145, 60, y, y + 60, 16, centerX + 10);
        addScrollingList(allowedPlayerListWidget, y, 60);
        
        y += 65;

        boolean allowCheat = TouhoulittlemaidpersonaldimensionClient.isAllowCheatConfigs();
        boolean allowAllowAll = Config.ALLOW_ALLOW_ALL_ENTITIES.get();
        boolean allowAllInit = localSettings != null && localSettings.isAllowAllEntities();
        
        if (allowAllowAll) {
            // Forge 1.20.1: Use custom CallbackCheckbox
            addScrollingWidget(new CallbackCheckbox(centerX + 165, y - 65, 20, 20, 
                    Component.literal("Allow All Entities"), allowAllInit, 
                    (checkbox, selected) -> {
                        localSettings.setAllowAllEntities(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_ALLOW_ALL_ENTITIES, String.valueOf(selected));
                    }));
        }

        if (allowCheat) {
             boolean disableHostileInit = localSettings != null && localSettings.isDisableHostileEntities();
             addScrollingWidget(new CallbackCheckbox(centerX + 165, y - 45, 20, 20, 
                    Component.literal("Disable Hostile Ent"), disableHostileInit, 
                    (checkbox, selected) -> {
                        localSettings.setDisableHostileEntities(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_HOSTILE_ENTITIES, String.valueOf(selected));
                    }));
        }
        
        if (allowCheat) {
            addScrollingWidget(new CallbackCheckbox(centerX + 10, y, 20, 20, 
                    Component.literal("Disable Hunger"),
                    localSettings != null && localSettings.isDisableHunger(),
                    (checkbox, selected) -> {
                        localSettings.setDisableHunger(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_HUNGER, String.valueOf(selected));
                    }));
            
            addScrollingWidget(new CallbackCheckbox(centerX + 165, y, 20, 20, 
                    Component.literal("Maid Immortal"),
                    localSettings != null && localSettings.isDisableMaidDeath(),
                    (checkbox, selected) -> {
                        localSettings.setDisableMaidDeath(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_MAID_DEATH, String.valueOf(selected));
                    }));
            
            y += 20;
            
            addScrollingWidget(new CallbackCheckbox(centerX + 10, y, 20, 20, 
                    Component.literal("Player Immortal"),
                    localSettings != null && localSettings.isDisablePlayerDeath(),
                    (checkbox, selected) -> {
                        localSettings.setDisablePlayerDeath(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_PLAYER_DEATH, String.valueOf(selected));
                    }));
            
            addScrollingWidget(new CallbackCheckbox(centerX + 165, y, 20, 20, 
                    Component.literal("Natural Healing"),
                    localSettings != null && localSettings.isNaturalHealing(),
                    (checkbox, selected) -> {
                        localSettings.setNaturalHealing(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_NATURAL_HEALING, String.valueOf(selected));
                    }));
            
            y += 20;
            
            addScrollingWidget(new CallbackCheckbox(centerX + 10, y, 20, 20, 
                    Component.literal("Block Harmful"),
                    localSettings != null && localSettings.isBlockHarmfulEffects(),
                    (checkbox, selected) -> {
                        localSettings.setBlockHarmfulEffects(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_BLOCK_HARMFUL_EFFECTS, String.valueOf(selected));
                    }));
            
            addScrollingWidget(new CallbackCheckbox(centerX + 165, y, 20, 20, 
                    Component.literal("Maid Light"),
                    localSettings != null && localSettings.isMaidEmitLight(),
                    (checkbox, selected) -> {
                        localSettings.setMaidEmitLight(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_MAID_EMIT_LIGHT, String.valueOf(selected));
                    }));
            
            y += 30;

            if (Config.TAMED_MAID_PROTECTION_ENABLED.get()) {
                addScrollingWidget(new CallbackCheckbox(centerX + 10, y, 20, 20, 
                        Component.literal("Tamed Maid Prot"),
                        localSettings != null && localSettings.isTamedMaidProtection(),
                        (checkbox, selected) -> {
                            localSettings.setTamedMaidProtection(selected);
                            sendPacket(PersonalDimensionGuiPacket.Action.SET_TAMED_MAID_PROTECTION, String.valueOf(selected));
                        }));
            }

            y += 25;
            addScrollingWidget(new CallbackCheckbox(centerX + 10, y, 20, 20, 
                    Component.literal("Mobs Neutral"),
                    localSettings != null && localSettings.isEntityCannotTarget(),
                    (checkbox, selected) -> {
                        localSettings.setEntityCannotTarget(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_ENTITY_CANNOT_TARGET, String.valueOf(selected));
                    }));
            addScrollingWidget(new CallbackCheckbox(centerX + 165, y, 20, 20, 
                    Component.literal("Maid Authority"),
                    localSettings != null && localSettings.isMaidAuthority(),
                    (checkbox, selected) -> {
                        localSettings.setMaidAuthority(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_MAID_AUTHORITY, String.valueOf(selected));
                    }));
            y += 25;
            
            // Load available dimensions
            List<CustomDimensionConfig> availableDimensions = CustomDimensionConfig.loadFromConfig();
            
            String currentDimId = localSettings != null ? localSettings.getDimensionTypeId() : null;
            if (currentDimId == null || currentDimId.isEmpty()) {
                currentDimId = availableDimensions.isEmpty() ? "void" : availableDimensions.get(0).getId();
            }
            
            // Make effectively final for lambda
            final String finalCurrentDimId = currentDimId;
            
            CustomDimensionConfig currentDim = CustomDimensionConfig.findById(finalCurrentDimId, availableDimensions);
            String displayDim = currentDim != null ? currentDim.getDisplayName() : finalCurrentDimId.toUpperCase();
            
            addScrollingWidget(Button.builder(Component.literal("Dim: " + displayDim), button -> {
                // Find current dimension index
                int currentIndex = -1;
                for (int i = 0; i < availableDimensions.size(); i++) {
                    if (availableDimensions.get(i).getId().equals(finalCurrentDimId)) {
                        currentIndex = i;
                        break;
                    }
                }
                
                // Get next dimension (cycle through list)
                int nextIndex = (currentIndex + 1) % availableDimensions.size();
                CustomDimensionConfig nextDim = availableDimensions.get(nextIndex);
                
                if (localSettings != null) localSettings.setDimensionTypeId(nextDim.getId());
                sendPacket(PersonalDimensionGuiPacket.Action.SET_DIMENSION_TYPE, nextDim.getId());
                
                button.setMessage(Component.literal("Dim: " + nextDim.getDisplayName()));
            }).bounds(centerX + 10, y, 140, 20).build());

            y += 22;
            // Warn user that new dimension types added to config require a world restart to be available
            Button dimWarning = Button.builder(
                    Component.literal("§e⚠ New dim types need world restart"),
                    b -> {}).bounds(centerX + 10, y, 210, 14).build();
            dimWarning.active = false;
            addScrollingWidget(dimWarning);

            y += 18;

            addScrollingWidget(new CallbackCheckbox(centerX + 10, y, 20, 20, 
                    Component.literal("Lock Day"),
                    localSettings != null && localSettings.isLockDay(),
                    (checkbox, selected) -> {
                        localSettings.setLockDay(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCK_DAY, String.valueOf(selected));
                    }));

            dayTimeInputWidget = new EditBox(this.font, centerX + 100, y, 60, 18, Component.empty());
            dayTimeInputWidget.setSuggestion("ticks");
            dayTimeInputWidget.setValue(localSettings != null ? String.valueOf(localSettings.getLockedDayTime()) : "1000");
            dayTimeInputWidget.setResponder(value -> {
                try {
                    int time = Integer.parseInt(value);
                    if (localSettings != null) localSettings.setLockedDayTime(time);
                    sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCKED_DAY_TIME, value);
                } catch (NumberFormatException ignored) {
                }
            });
            addScrollingWidget(dayTimeInputWidget);

            y += 22;

            addScrollingWidget(new CallbackCheckbox(centerX + 10, y, 20, 20, 
                    Component.literal("Lock Weather"),
                    localSettings != null && localSettings.isLockWeather(),
                    (checkbox, selected) -> {
                        localSettings.setLockWeather(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCK_WEATHER, String.valueOf(selected));
                    }));

            addScrollingWidget(new CallbackCheckbox(centerX + 130, y, 20, 20, 
                    Component.literal("Rain"),
                    localSettings != null && localSettings.isLockedWeatherRain(),
                    (checkbox, selected) -> {
                        localSettings.setLockedWeatherRain(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCKED_WEATHER_RAIN, String.valueOf(selected));
                    }));

            addScrollingWidget(new CallbackCheckbox(centerX + 210, y, 20, 20, 
                    Component.literal("Thunder"),
                    localSettings != null && localSettings.isLockedWeatherThunder(),
                    (checkbox, selected) -> {
                        localSettings.setLockedWeatherThunder(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCKED_WEATHER_THUNDER, String.valueOf(selected));
                    }));
            
            y += 30;
        }
        
        this.contentHeight = y + 20;
        
        updateLocalLists();
        updateLists();

        if (TouhoulittlemaidpersonaldimensionClient.getLastSettings().isEmpty()) {
            sendPacket(PersonalDimensionGuiPacket.Action.REQUEST_SYNC, "");
        }
    }

    private void updateLocalLists() {
        allowedEntities.clear();
        blockedEntities.clear();
        allowedPlayers.clear();

        if (localSettings != null) {
            allowedEntities.addAll(localSettings.getAllowedEntities());
            blockedEntities.addAll(localSettings.getBlockedEntities());
            allowedPlayers.addAll(localSettings.getAllowedPlayers());
        }
    }

    private void updateLists() {
        allowedEntityListWidget.clearEntries();
        for (String entity : allowedEntities) {
            allowedEntityListWidget.addEntryInternal(new EntityEntry(entity, true, false));
        }

        blockedEntityListWidget.clearEntries();
        for (String entity : blockedEntities) {
            blockedEntityListWidget.addEntryInternal(new EntityEntry(entity, false, false));
        }
        
        allowedPlayerListWidget.clearEntries();
        for (String player : allowedPlayers) {
            allowedPlayerListWidget.addEntryInternal(new EntityEntry(player, true, true));
        }
    }

    public void updateSettings(PersonalDimensionSavedData.PlayerDimensionSettings settings) {
        this.localSettings = settings;
        updateLocalLists();
        updateLists();
        if (this.minecraft != null && (this.getFocused() == null || !(this.getFocused() instanceof EditBox))) {
             this.init(this.minecraft, this.width, this.height);
        }
    }

    private void sendPacket(PersonalDimensionGuiPacket.Action action, String data) {
        Touhoulittlemaidpersonaldimension.NETWORK.sendToServer(new PersonalDimensionGuiPacket(action, data, maid.getId()));
    }

    private void addAllowedEntity() {
        String entityId = allowedEntityInput.getValue().trim();
        if (!entityId.isEmpty() && !allowedEntities.contains(entityId)) {
            allowedEntities.add(entityId);
            sendPacket(PersonalDimensionGuiPacket.Action.ADD_ALLOWED_ENTITY, entityId);
            allowedEntityInput.setValue("");
            updateLists();
        }
    }

    private void addBlockedEntity() {
        String entityId = blockedEntityInput.getValue().trim();
        if (!entityId.isEmpty() && !blockedEntities.contains(entityId)) {
            blockedEntities.add(entityId);
            sendPacket(PersonalDimensionGuiPacket.Action.ADD_BLOCKED_ENTITY, entityId);
            blockedEntityInput.setValue("");
            updateLists();
        }
    }
    
    private void addAllowedPlayer() {
        String player = allowedPlayerInput.getValue().trim();
        if (!player.isEmpty() && !allowedPlayers.contains(player)) {
            allowedPlayers.add(player);
            sendPacket(PersonalDimensionGuiPacket.Action.ADD_ALLOWED_PLAYER, player);
            allowedPlayerInput.setValue("");
            updateLists();
        }
    }

    // In 1.20.1, mouseScrolled only takes 3 parameters
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.contentHeight > this.height) {
            this.scrollAmount = net.minecraft.util.Mth.clamp(this.scrollAmount - delta * 20, 0, this.contentHeight - this.height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        // Update all scrolling AbstractWidget positions before super.render draws them
        for (WidgetPos sw : scrollingWidgets) {
            sw.widget.setY(sw.baseY - (int)scrollAmount);
        }

        // Update list widget scissor bounds before rendering
        for (ListWidgetPos lp : scrollingLists) {
            lp.widget.updateTopBottom(lp.baseY - (int)scrollAmount);
        }

        int yOff = (int) -scrollAmount;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, yOff + 5, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("Whitelist (Allowed)"), centerX + 10, yOff + 30, 0xAAAAAA);
        graphics.drawString(this.font, Component.literal("Blacklist (Blocked)"), centerX + 165, yOff + 30, 0xAAAAAA);
        graphics.drawString(this.font, Component.literal("Allowed Players"), centerX + 10, yOff + 155, 0xAAAAAA);

        int yText = yOff + 250;
        if (TouhoulittlemaidpersonaldimensionClient.isAllowCheatConfigs()) {
            graphics.drawString(this.font, Component.literal("Settings"), centerX + 10, yText, 0xAAAAAA);
            yText += 115;
        }
        yText += 10;
        if (TouhoulittlemaidpersonaldimensionClient.isAllowCheatConfigs()) {
            graphics.drawString(this.font, Component.literal("Weather/Day"), centerX + 10, yText, 0xAAAAAA);
        }

        // Render buttons/checkboxes via the normal pipeline
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Render list widgets AFTER super.render so they appear on top of everything
        for (ListWidgetPos lp : scrollingLists) {
            lp.widget.render(graphics, mouseX, mouseY, partialTicks);
        }

        // Scrollbar (always on top)
        if (this.contentHeight > this.height) {
            int scrollbarX = this.width - 6;
            int scrollbarWidth = 4;
            int scrollbarHeight = (int)((float)this.height / this.contentHeight * this.height);
            int scrollbarY = (int)((float)this.scrollAmount / (this.contentHeight - this.height) * (this.height - scrollbarHeight));
            graphics.fill(scrollbarX, 0, scrollbarX + scrollbarWidth, this.height, 0x40000000);
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Forward clicks to list widgets first (they're not in the normal widget pipeline)
        for (ListWidgetPos lp : scrollingLists) {
            if (lp.widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (this.contentHeight > this.height) {
            int scrollbarX = this.width - 10;
            if (mouseX >= scrollbarX) {
                this.isScrolling = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling && this.contentHeight > this.height) {
            double ratio = (double)this.contentHeight / this.height;
            this.scrollAmount = net.minecraft.util.Mth.clamp(this.scrollAmount + dragY * ratio, 0, this.contentHeight - this.height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // In 1.20.1, ObjectSelectionList needs proper X positioning
    private class EntityListWidget extends ObjectSelectionList<EntityEntry> {
        private final int xPos;
        private final int listHeight;
        
        public EntityListWidget(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight, int xPos) {
            super(minecraft, width, height, top, bottom, itemHeight);
            this.xPos = xPos;
            this.listHeight = height;
            // Fix x position — default x0=0, x1=width; must offset to xPos
            this.x0 = xPos;
            this.x1 = xPos + width;
            // Disable the dirt background and gradient overlays
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        // Called by scroll system to update the scissor/hit-test bounds
        public void updateTopBottom(int newY) {
            this.y0 = newY;
            this.y1 = newY + listHeight;
        }

        // Suppress the dirt background (belt-and-suspenders)
        @Override
        protected void renderBackground(GuiGraphics graphics) {}

        // Suppress the top/bottom gradient overlays (belt-and-suspenders)
        @Override
        protected void renderDecorations(GuiGraphics graphics, int mouseX, int mouseY) {}
        
        @Override
        public int getRowLeft() {
            return this.xPos + 2;
        }
        
        @Override
        public int getRowWidth() {
            return this.width - 10;
        }
        
        @Override
        protected int getScrollbarPosition() {
            return this.xPos + this.width - 6;
        }
        
        public void addEntryInternal(EntityEntry entry) {
            super.addEntry(entry);
        }
        
        @Override
        public void clearEntries() {
            super.clearEntries();
        }
    }

    private class EntityEntry extends ObjectSelectionList.Entry<EntityEntry> {
        private final String entityId;
        private final boolean isAllowed;
        private final boolean isPlayer;
        private final Button removeButton;

        public EntityEntry(String entityId, boolean isAllowed, boolean isPlayer) {
            this.entityId = entityId;
            this.isAllowed = isAllowed;
            this.isPlayer = isPlayer;
            this.removeButton = Button.builder(Component.literal("X"), button -> removeEntity()).bounds(0, 0, 15, 14).build();
        }

        private void removeEntity() {
            if (isPlayer) {
                allowedPlayers.remove(entityId);
                sendPacket(PersonalDimensionGuiPacket.Action.REMOVE_ALLOWED_PLAYER, entityId);
            } else if (isAllowed) {
                allowedEntities.remove(entityId);
                sendPacket(PersonalDimensionGuiPacket.Action.REMOVE_ALLOWED_ENTITY, entityId);
            } else {
                blockedEntities.remove(entityId);
                sendPacket(PersonalDimensionGuiPacket.Action.REMOVE_BLOCKED_ENTITY, entityId);
            }
            updateLists();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            // X button is 15px wide + 3px gap from right edge
            int buttonWidth = 15;
            int buttonX = x + entryWidth - buttonWidth - 2;
            int availableTextWidth = entryWidth - buttonWidth - 6; // gap on both sides

            String display = entityId;
            if (PersonalDimensionGui.this.font.width(display) > availableTextWidth) {
                display = PersonalDimensionGui.this.font.substrByWidth(
                        Component.literal(display), availableTextWidth - 8).getString() + "..";
            }
            graphics.drawString(PersonalDimensionGui.this.font, display, x + 2, y + (entryHeight - 8) / 2, 0xFFFFFF);

            removeButton.setX(buttonX);
            removeButton.setY(y + (entryHeight - 14) / 2);
            removeButton.render(graphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (removeButton.isMouseOver(mouseX, mouseY)) {
                removeButton.onClick(mouseX, mouseY);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(entityId);
        }
    }
}

