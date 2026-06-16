package com.tlmpersonal.tlmpersonaldimension.compat;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.MaidContainerGuiEvent;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.MaidTabButton;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionGui;
import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionMainGui;
import com.tlmpersonal.tlmpersonaldimension.client.screen.PersonalDimensionMaidScreen;
import com.tlmpersonal.tlmpersonaldimension.inventory.PersonalDimensionMenu;
import com.tlmpersonal.tlmpersonaldimension.network.TeleportToPersonalDimensionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Touhoulittlemaidpersonaldimension.MODID, value = Dist.CLIENT)
public class TouhouLittleMaidCompat {

    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(Touhoulittlemaidpersonaldimension.MODID, "textures/gui/personal_dimension_icon.png");
    private static final int TOP_TAB_Y_OFFSET = 5;
    private static final int FIRST_TOP_TAB_X_OFFSET = 94;
    private static final int FIRST_EXTERNAL_TAB_X_OFFSET = 194;
    private static final int TOP_TAB_SPACING = 25;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onMaidGuiInit(MaidContainerGuiEvent.Init event) {
        AbstractMaidContainerGui<?> gui = event.getGui();
        int leftPos = event.getLeftPos();
        int topPos = event.getTopPos();

        // 1. Find and modify the 2nd tab (Task Config)
        for (var child : gui.children()) {
            if (child instanceof MaidTabButton tab && tab.getX() == leftPos + 119) {
                event.addButton("task_config_shortcut", new InvisibleShortcutButton(tab.getX(), tab.getY(), tab.getWidth(), tab.getHeight(), gui));
            }
        }

        // 2. Add clickable overlay if on the Default Task Config screen (the one with the island)
        if (gui.getClass().getSimpleName().equals("DefaultMaidTaskConfigGui")) {
            event.addButton("island_task_overlay", new InvisibleIslandButton(leftPos + 80, topPos + 28, 176, 137, gui));
        }

        // 3. Add Personal Dimension Tab Button (4th Slot)
        int tabX = resolveTabX(gui, leftPos, topPos);
        MaidTabButton personalTabButton = new MaidTabButton(tabX, topPos + TOP_TAB_Y_OFFSET, 182, "personal_dimension", btn -> {
            if (gui instanceof PersonalDimensionMaidScreen) return;
            openPersonalTab(gui);
        }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                boolean isActive = gui instanceof PersonalDimensionMaidScreen;
                boolean wasActive = this.active;
                if (isActive) this.active = false;
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                this.active = wasActive;

                // Cover the old AI icon area with a solid box
                int coverColor = isActive ? 0xFFC6C6C6 : 0xFF717171;
                guiGraphics.fill(this.getX() + 4, this.getY() + 4, this.getX() + 20, this.getY() + 20, coverColor);
                
                // Draw our new icon on top
                guiGraphics.blit(ICON, this.getX() + 4, this.getY() + 4, 0, 0, 16, 16, 16, 16);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.active && this.visible && this.isHoveredOrFocused()) {
                    if (button == 1) { // Right Click
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().setScreen(new PersonalDimensionGui(gui.getMaid()));
                            return true;
                        }
                    }
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        };

        event.addButton("personal_tab_button", personalTabButton);
    }

    private static void openPersonalTab(AbstractMaidContainerGui<?> gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && gui.getMaid() != null) {
            mc.setScreen(new PersonalDimensionMaidScreen(
                new PersonalDimensionMenu(0, mc.player.getInventory(), gui.getMaid().getId()),
                mc.player.getInventory(),
                Component.literal("Personal Dimension")
            ));
        }
    }

    private static int resolveTabX(AbstractMaidContainerGui<?> gui, int leftPos, int topPos) {
        List<Integer> topTabXs = new ArrayList<>();
        for (var child : gui.children()) {
            if (child instanceof MaidTabButton tab && tab.getY() == topPos + TOP_TAB_Y_OFFSET) {
                topTabXs.add(tab.getX());
            }
        }
        if (topTabXs.isEmpty()) return leftPos + FIRST_EXTERNAL_TAB_X_OFFSET;
        int nextX = leftPos + FIRST_TOP_TAB_X_OFFSET;
        for (int x : topTabXs) {
            if (x >= leftPos + FIRST_TOP_TAB_X_OFFSET) nextX = Math.max(nextX, x + TOP_TAB_SPACING);
        }
        return nextX == leftPos + FIRST_TOP_TAB_X_OFFSET ? leftPos + FIRST_EXTERNAL_TAB_X_OFFSET : nextX;
    }

    private static class InvisibleShortcutButton extends Button {
        private final AbstractMaidContainerGui<?> gui;
        public InvisibleShortcutButton(int x, int y, int w, int h, AbstractMaidContainerGui<?> gui) {
            super(x, y, w, h, Component.empty(), b -> {}, DEFAULT_NARRATION);
            this.gui = gui;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.visible && this.isHoveredOrFocused() && button == 1) {
                Minecraft.getInstance().setScreen(new PersonalDimensionGui(gui.getMaid()));
                return true;
            }
            return false;
        }
    }

    private static class InvisibleIslandButton extends Button {
        private final AbstractMaidContainerGui<?> gui;
        public InvisibleIslandButton(int x, int y, int w, int h, AbstractMaidContainerGui<?> gui) {
            super(x, y, w, h, Component.empty(), b -> {
                Minecraft.getInstance().setScreen(new PersonalDimensionMainGui(gui.getMaid()));
            }, DEFAULT_NARRATION);
            this.gui = gui;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
            if (this.isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x25FFFFFF);
                graphics.renderComponentTooltip(Minecraft.getInstance().font, List.of(
                        Component.literal("Click to open dimension GUI").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Right Click to teleport to dimension")
                                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFfb7c5)))
                ), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.visible && this.isHoveredOrFocused()) {
                if (button == 0) {
                    this.onPress();
                    return true;
                } else if (button == 1) {
                    if (gui.getMaid().getFavorabilityManager().getLevel() >= 3) {
                        PacketDistributor.sendToServer(new TeleportToPersonalDimensionPacket(gui.getMaid().getId(), true));
                        Minecraft.getInstance().setScreen(null);
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
