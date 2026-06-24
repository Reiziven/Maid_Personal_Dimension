package com.tlmpersonal.tlmpersonaldimension.client.gui;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.TouhoulittlemaidpersonaldimensionClient;
import com.tlmpersonal.tlmpersonaldimension.network.PersonalDimensionGuiPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
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

    private static class WidgetPos {
        final AbstractWidget widget;
        final int baseY;
        WidgetPos(AbstractWidget widget, int baseY) {
            this.widget = widget;
            this.baseY = baseY;
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

    @Override
    protected void init() {
        super.init();
        centerX = (this.width - GUI_WIDTH) / 2;
        scrollingWidgets.clear();
        
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

        // Increased list height and improved spacing
        allowedEntityListWidget = new EntityListWidget(this.minecraft, 145, 80, y, 16);
        allowedEntityListWidget.setX(centerX + 10);
        addScrollingWidget(allowedEntityListWidget);

        blockedEntityListWidget = new EntityListWidget(this.minecraft, 145, 80, y, 16);
        blockedEntityListWidget.setX(centerX + 165);
        addScrollingWidget(blockedEntityListWidget);

        y += 85;

        allowedPlayerInput = new EditBox(this.font, centerX + 10, y + 12, 110, 18, Component.empty());
        allowedPlayerInput.setSuggestion("player name/uuid");
        addScrollingWidget(allowedPlayerInput);

        addScrollingWidget(Button.builder(Component.literal("Add"), button -> addAllowedPlayer())
                .bounds(centerX + 120, y + 12, 35, 18)
                .build());

        y += 35;
        allowedPlayerListWidget = new EntityListWidget(this.minecraft, 145, 60, y, 16);
        allowedPlayerListWidget.setX(centerX + 10);
        addScrollingWidget(allowedPlayerListWidget);
        
        y += 65;

        boolean allowCheat = TouhoulittlemaidpersonaldimensionClient.isAllowCheatConfigs();
        boolean allowAllowAll = Config.ALLOW_ALLOW_ALL_ENTITIES.get();
        boolean allowAllInit = localSettings != null && localSettings.isAllowAllEntities();
        
        if (allowAllowAll) {
            addScrollingWidget(Checkbox.builder(Component.literal("Allow All Entities"), this.font)
                    .pos(centerX + 165, y - 65) 
                    .selected(allowAllInit)
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setAllowAllEntities(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_ALLOW_ALL_ENTITIES, String.valueOf(selected));
                    })
                    .build());
        }

        if (allowCheat) {
             boolean disableHostileInit = localSettings != null && localSettings.isDisableHostileEntities();
             addScrollingWidget(Checkbox.builder(Component.literal("Disable Hostile Ent"), this.font)
                    .pos(centerX + 165, y - 45) 
                    .selected(disableHostileInit)
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setDisableHostileEntities(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_HOSTILE_ENTITIES, String.valueOf(selected));
                    })
                    .build());
        }
        
        if (allowCheat) {
            addScrollingWidget(Checkbox.builder(Component.literal("Disable Hunger"), this.font)
                    .pos(centerX + 10, y)
                    .selected(localSettings != null && localSettings.isDisableHunger())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setDisableHunger(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_HUNGER, String.valueOf(selected));
                    })
                    .build());
            
            addScrollingWidget(Checkbox.builder(Component.literal("Maid Immortal"), this.font)
                    .pos(centerX + 165, y)
                    .selected(localSettings != null && localSettings.isDisableMaidDeath())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setDisableMaidDeath(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_MAID_DEATH, String.valueOf(selected));
                    })
                    .build());
            
            y += 20;
            
            addScrollingWidget(Checkbox.builder(Component.literal("Player Immortal"), this.font)
                    .pos(centerX + 10, y)
                    .selected(localSettings != null && localSettings.isDisablePlayerDeath())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setDisablePlayerDeath(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DISABLE_PLAYER_DEATH, String.valueOf(selected));
                    })
                    .build());
            
            addScrollingWidget(Checkbox.builder(Component.literal("Natural Healing"), this.font)
                    .pos(centerX + 165, y)
                    .selected(localSettings != null && localSettings.isNaturalHealing())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setNaturalHealing(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_NATURAL_HEALING, String.valueOf(selected));
                    })
                    .build());
            
            y += 20;
            
            addScrollingWidget(Checkbox.builder(Component.literal("Block Harmful"), this.font)
                    .pos(centerX + 10, y)
                    .selected(localSettings != null && localSettings.isBlockHarmfulEffects())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setBlockHarmfulEffects(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_BLOCK_HARMFUL_EFFECTS, String.valueOf(selected));
                    })
                    .build());
            
            addScrollingWidget(Checkbox.builder(Component.literal("Maid Light"), this.font)
                    .pos(centerX + 165, y)
                    .selected(localSettings != null && localSettings.isMaidEmitLight())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setMaidEmitLight(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_MAID_EMIT_LIGHT, String.valueOf(selected));
                    })
                    .build());
            
            y += 30;

            if (Config.TAMED_MAID_PROTECTION_ENABLED.get()) {
                addScrollingWidget(Checkbox.builder(Component.literal("Tamed Maid Prot"), this.font)
                        .pos(centerX + 10, y)
                        .selected(localSettings != null && localSettings.isTamedMaidProtection())
                        .onValueChange((checkbox, selected) -> {
                            localSettings.setTamedMaidProtection(selected);
                            sendPacket(PersonalDimensionGuiPacket.Action.SET_TAMED_MAID_PROTECTION, String.valueOf(selected));
                        })
                        .build());
            }

            addScrollingWidget(Checkbox.builder(Component.literal("Mobs Neutral"), this.font)
                    .pos(centerX + 10, y + 25)
                    .selected(localSettings != null && localSettings.isEntityCannotTarget())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setEntityCannotTarget(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_ENTITY_CANNOT_TARGET, String.valueOf(selected));
                    })
                    .build());
            addScrollingWidget(Checkbox.builder(Component.literal("Maid Authority"), this.font)
                    .pos(centerX + 165, y + 25)
                    .selected(localSettings != null && localSettings.isMaidAuthority())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setMaidAuthority(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_MAID_AUTHORITY, String.valueOf(selected));
                    })
                    .build());
            y += 50;
            addScrollingWidget(Checkbox.builder(Component.literal("Domain Dim Rules"), this.font)
                    .pos(centerX + 10, y)
                    .selected(localSettings != null && localSettings.isDomainExpansionUseDimensionRules())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setDomainExpansionUseDimensionRules(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DOMAIN_EXPANSION_DIMENSION_RULES, String.valueOf(selected));
                    })
                    .build());
            addScrollingWidget(Checkbox.builder(Component.literal("Domain Entities Rules"), this.font)
                    .pos(centerX + 165, y)
                    .selected(localSettings != null && localSettings.isDomainExpansionUseEntityProtection())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setDomainExpansionUseEntityProtection(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_DOMAIN_EXPANSION_ENTITY_PROTECTION, String.valueOf(selected));
                    })
                    .build());
            y += 25;
            Config.DimensionType currentType = localSettings != null && localSettings.getDimensionType() != null ? localSettings.getDimensionType() : Config.DIMENSION_TYPE.get();
            String displayDim = switch (currentType) {
                case VOID -> "MAID ISLAND";
                case NORMAL -> "OVERWORLD";
                case CHERRY -> "CHERRY";
            };
            
            addScrollingWidget(Button.builder(Component.literal("Dim: " + displayDim), button -> {
                Config.DimensionType next = Config.DimensionType.values()[(currentType.ordinal() + 1) % Config.DimensionType.values().length];
                if (localSettings != null) localSettings.setDimensionType(next);
                sendPacket(PersonalDimensionGuiPacket.Action.SET_DIMENSION_TYPE, next.name());
                String nextDisplay = switch (next) {
                    case VOID -> "MAID ISLAND";
                    case NORMAL -> "OVERWORLD";
                    case CHERRY -> "CHERRY";
                };
                button.setMessage(Component.literal("Dim: " + nextDisplay));
            }).bounds(centerX + 10, y, 140, 20).build());

            y += 25;

            addScrollingWidget(Checkbox.builder(Component.literal("Lock Day"), this.font)
                    .pos(centerX + 10, y)
                    .selected(localSettings != null && localSettings.isLockDay())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setLockDay(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCK_DAY, String.valueOf(selected));
                    })
                    .build());

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

            addScrollingWidget(Checkbox.builder(Component.literal("Lock Weather"), this.font)
                    .pos(centerX + 10, y)
                    .selected(localSettings != null && localSettings.isLockWeather())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setLockWeather(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCK_WEATHER, String.valueOf(selected));
                    })
                    .build());

            addScrollingWidget(Checkbox.builder(Component.literal("Rain"), this.font)
                    .pos(centerX + 130, y)
                    .selected(localSettings != null && localSettings.isLockedWeatherRain())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setLockedWeatherRain(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCKED_WEATHER_RAIN, String.valueOf(selected));
                    })
                    .build());

            addScrollingWidget(Checkbox.builder(Component.literal("Thunder"), this.font)
                    .pos(centerX + 210, y)
                    .selected(localSettings != null && localSettings.isLockedWeatherThunder())
                    .onValueChange((checkbox, selected) -> {
                        localSettings.setLockedWeatherThunder(selected);
                        sendPacket(PersonalDimensionGuiPacket.Action.SET_LOCKED_WEATHER_THUNDER, String.valueOf(selected));
                    })
                    .build());
            
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
        PacketDistributor.sendToServer(new PersonalDimensionGuiPacket(action, data, maid.getId()));
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.contentHeight > this.height) {
            this.scrollAmount = Math.clamp(this.scrollAmount - scrollY * 20, 0, this.contentHeight - this.height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderTransparentBackground(graphics);
        
        // Fix: Update all scrolling widgets positions instead of pose translation
        // This ensures hit detection and SelectionList scissoring works correctly
        for (WidgetPos sw : scrollingWidgets) {
            sw.widget.setY(sw.baseY - (int)scrollAmount);
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
        
        super.render(graphics, mouseX, mouseY, partialTicks);
        
        // Render scrollbar if needed (stays fixed on screen)
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
            this.scrollAmount = Math.clamp(this.scrollAmount + dragY * ratio, 0, this.contentHeight - this.height);
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

    private class EntityListWidget extends ObjectSelectionList<EntityEntry> {
        public EntityListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }
        @Override
        public int getRowWidth() { return this.width - 25; } // Improved row width to avoid scrollbar overlap
        @Override
        protected int getScrollbarPosition() { return this.getX() + this.width - 6; }
        public void addEntryInternal(EntityEntry entry) { super.addEntry(entry); }
        @Override
        public void clearEntries() { super.clearEntries(); }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Standard selection list logic now works because Y is updated in parent
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
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
            String display = entityId;
            if (PersonalDimensionGui.this.font.width(display) > entryWidth - 25) {
                display = PersonalDimensionGui.this.font.substrByWidth(Component.literal(display), entryWidth - 30).getString() + "...";
            }
            graphics.drawString(PersonalDimensionGui.this.font, display, x + 2, y + 2, 0xFFFFFF);
            removeButton.setX(x + entryWidth - 18);
            removeButton.setY(y);
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
        public Component getNarration() { return Component.literal(entityId); }
    }
}
